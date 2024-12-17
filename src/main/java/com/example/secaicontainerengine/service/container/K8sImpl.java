package com.example.secaicontainerengine.service.container;

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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

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
            String yamlContent = renderTemplate("template/Pod.yml", values);
            ByteArrayInputStream ymlStream = new ByteArrayInputStream(yamlContent.getBytes());
            streams.add(ymlStream);
        }
        return streams;
    }

    //启动接口
    public void start(String userId, List<ByteArrayInputStream> streams) throws IOException {
        for (ByteArrayInputStream stream : streams) {
            //获取pod的名字
            String podName = getName(stream);

            taskExecutor.execute(() -> {
                final CountDownLatch closeLatch = new CountDownLatch(1);
                //创建Pod
                HasMetadata metadata = K8sClient.resource(stream).inNamespace("default").create();
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
                                            .containerName(metadata.getMetadata().getName())
                                            .userId(Integer.valueOf(userId))
                                            .createTime(LocalDateTime.now())
                                            .build();
                                    String containerKey = userId + ":" +podName;
                                    redisTemplate.opsForValue().set(containerKey, container);
                                    log.info("启动接口：容器实例已记录到Redis中-" + metadata.getMetadata().getName());
                                } else if (phase.equals("Succeeded")) {
                                    deleteSingle(userId, podName);
                                } else if (phase.equals("Failed")) {
                                    deleteSingle(userId, podName);
                                }
                                break;
                            }
                            case DELETED: {
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
                    watch.close();
                }
            });
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
    public void deleteSingle(String userId, String podName) {
        String containerKey = userId + ":" +podName;
        redisTemplate.delete(containerKey);
        K8sClient.pods().inNamespace("default").withName(podName).delete();
        log.info("回收接口：已删除Pod-" + podName);
    }


}
