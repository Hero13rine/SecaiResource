package com.example.secaicontainerengine.service.container;

import com.example.secaicontainerengine.context.SchedulerContext;
import com.example.secaicontainerengine.pojo.entity.Container;
import com.example.secaicontainerengine.util.PodUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.template.TemplateException;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

import static com.example.secaicontainerengine.util.YamlUtil.getName;
import static com.example.secaicontainerengine.util.YamlUtil.renderTemplate;

@Service(value = "k8sContainerImpl")
@Slf4j
public class K8sImpl implements ContainerService {

    @Autowired
    private KubernetesClient K8sClient;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExecutorService taskExecutor;

    @Autowired
    private PodUtil podUtil;

    //初始化接口
    public List<ByteArrayInputStream> init(String userId, Map<String, String> imageUrl, Map<String, Map> imageParam) throws IOException, TemplateException {
        List<ByteArrayInputStream> streams = new ArrayList<>();
        for(String value: imageUrl.values()){
            //pod命名方式：url+用户id
            String podName = value+userId;
            log.info("初始化接口：Pod的名称-" + podName);
            boolean podExist = redisTemplate.hasKey(userId+":"+podName);
            //如果已存在，继续创建下一个pod
            if(podExist) {
                log.info("初始化接口：该Pod之前已启动-" + podName);
                continue;
            }
            //准备模板变量
            Map<String, String> values = new HashMap<>();
            values.put("pod_name", podName);
            values.put("container_name", value);
            values.put("image", value);
            //生成填充好的yml文件字节流
            String yamlContent = renderTemplate("template/Next.yml", values);
            ByteArrayInputStream ymlStream = new ByteArrayInputStream(yamlContent.getBytes());
            streams.add(ymlStream);
        }
        return streams;
    }

    //启动接口
    public void start(String userId, List<ByteArrayInputStream> streams) throws IOException {
        //如果该用户还没有上下文，初始化一个新的上下文
        userContexts.computeIfAbsent(userId, id -> new SchedulerContext(streams.size()));

        for (ByteArrayInputStream stream : streams) {
            //获取pod的名字
            String podName = getName(stream);
            taskExecutor.execute(() -> {
                //创建Pod
                HasMetadata metadata = K8sClient.resource(stream).inNamespace("default").create();
                try {
                    collectLogs(userId, podName, "/home/secAI/test.txt");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                watchStatus(userId, podName);
            });
        }
    }

    //监听接口1-持续监听指定的pod状态
    public void watchStatus(String userId, String podName) {
        SchedulerContext userContext = userContexts.get(userId);
        Map<String, ScheduledFuture<?>> taskMap = userContext.getTaskMap();

        final CountDownLatch closeLatch = new CountDownLatch(1);
        Watch watch = K8sClient.pods().inNamespace("default").withName(podName).watch(new Watcher<Pod>() {
            @Override
            public void eventReceived(Action action, Pod pod) {
                String phase = pod.getStatus().getPhase();
                log.info("action: " + action +" phase：" + phase);
                switch (action) {
                    case ADDED:
                    case MODIFIED: {
                        //把创建好的Pod实例保存在redis中
                        if(phase.equals("Running")) {
                            log.info("启动接口：已启动的Pod名称-" + podName);
                            Container container = Container.builder()
                                    .containerName(podName)
                                    .userId(Integer.valueOf(userId))
                                    .createTime(LocalDateTime.now())
                                    .build();
                            String containerKey = userId + ":" +podName;
                            redisTemplate.opsForValue().set(containerKey, container);
                            log.info("启动接口：容器实例已记录到Redis中-" + podName);
                            try {
                                collectLogs(userId, podName, "/var/log/secAI/test.txt");
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        } else if (phase.equals("Succeeded")) {
                            deleteSingle(userId, podName);
                            stopLoggingTask(podName, taskMap);
                        } else if (phase.equals("Failed")) {
                            deleteSingle(userId, podName);
                            stopLoggingTask(podName, taskMap);
                        }
                        break;
                    }
                    case DELETED: {
                        stopLoggingTask(podName, taskMap);
                        closeLatch.countDown();
                        break;
                    }
                }
            }

            @Override
            public void onClose(WatcherException cause) {

            }
        });
        try {
            closeLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            cleanupUserContext(userId, userContext);
            watch.close();
        }
    }

    //监听接口2-只输出一次pod的状态
    public String getStatus(String podName) {
        Pod pod = K8sClient.pods().inNamespace("default").withName(podName).get();
        if (pod != null) {
            // 输出 Pod 的 Phase
            return pod.getStatus().getPhase();
        } else {
            return "该Pod不存在";
        }
    }

    //日志接口-搜集指定pod的日志，并记录到指定的文件中
    public void collectLogs(String userId, String podName, String filePath) throws IOException {
        SchedulerContext userContext = userContexts.get(userId);
        ScheduledExecutorService scheduler = userContext.getScheduler();
        Map<String, ScheduledFuture<?>> taskMap = userContext.getTaskMap();
        if(taskMap.containsKey(podName)){
            return;
        }

        //开启定时任务，记录日志
        Runnable logTask = ()-> {
            try {
                log.info("开始记录日志");
                String logContent = K8sClient.pods().inNamespace("default").withName(podName).getLog();
                System.out.println("logContent: " + logContent);
                System.out.println("filePath: " + filePath);
                File file = new File(filePath);
                if(!file.exists()){
                    file.createNewFile();
                }
                //以追加方式打开文件
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                    writer.append(logContent);
                    writer.newLine();
                    writer.flush();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(logTask, 0, 1, TimeUnit.SECONDS);
        taskMap.put(podName, task);

    }

    //停止记录日志任务
    private void stopLoggingTask(String podName, Map<String, ScheduledFuture<?>> taskMap) {
        ScheduledFuture<?> task = taskMap.remove(podName);
        if (task != null) {
            task.cancel(true);
            log.info("停止日志记录任务：" + podName);
        }
    }

    // 清理用户上下文
    private void cleanupUserContext(String userId, SchedulerContext userContext) {
        // 当所有任务完成后，清理相关的上下文
        if (userContext.getTaskMap().isEmpty()) {
            userContexts.remove(userId);
            userContext.getScheduler().shutdownNow();
            log.info("已移除用户 " + userId + " 的上下文并关闭定时任务线程池");
        }
    }


    //回收接口1-删除指定用户的所有pod
    public void deleteAll(String userId) {
        //获取该用户下的所有key
        String pattern = userId+":*";
        Set<String> keys = redisTemplate.keys(pattern);
        if(keys == null || keys.isEmpty()) {
            return;
        }
        //保存待删除的容器列表
        List<Container> removeList = new ArrayList<>();
        keys.forEach(key -> {
            Object container =  redisTemplate.opsForValue().get(key);
            if(container != null) {
                removeList.add(objectMapper.convertValue(container,Container.class));
            }
            redisTemplate.delete(key);
        });
        log.info("回收接口：待删除的容器列表-" + removeList.toString());
        //逐个删除容器
        removeList.forEach(container -> {
            K8sClient.pods().inNamespace("default").withName(container.getContainerName()).delete();
            log.info("回收接口：已删除Pod-" + container.getContainerName());
        });
    }

    //回收接口2-删除用户的单个pod
    public void deleteSingle(String userId, String containerName) {
        String containerKey = userId + ":" + containerName;
        redisTemplate.delete(containerKey);
        K8sClient.pods().inNamespace("default").withName(containerName).delete();
        log.info("回收接口：已删除Pod-" + containerName);
    }



}
