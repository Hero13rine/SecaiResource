package com.example.secaicontainerengine.service.container;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.secaicontainerengine.mapper.ContainerMapper;
import com.example.secaicontainerengine.pojo.entity.Container;
import com.example.secaicontainerengine.pojo.entity.ModelMessage;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.*;

import static com.example.secaicontainerengine.util.YamlUtil.getName;
import static com.example.secaicontainerengine.util.YamlUtil.renderTemplate;

@Service(value = "k8sContainerImpl")
@Slf4j
public class K8sImpl extends ServiceImpl<ContainerMapper, Container> implements ContainerService{

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

    @Value("${k8s.yaml}")
    private String k8sYaml;

    @Value("${k8s.adversarial-yaml}")
    private String k8sAdversarialYaml;

    @Value("${k8s.adversarial-gpu-yaml}")
    private String k8sAdversarialGpuYaml;

    @Value("${nfs.rootPath}")
    private String rootPath;

    @Value("${sftp.host}")
    private String nfsIp;

    @Autowired
    private ContainerMapper containerMapper;


    @Value("${nfs.userData}")
    private String userData;

    @Value("${nfs.systemData}")
    private String systemData;

    @Value("${nfs.evaluationData}")
    private String evaluationData;

    @Value("${docker.registryHost}")
    private String registryHost;

    //初始化接口
    public List<ByteArrayInputStream> init(Long userId, Map<String, String> imageUrl, Map<String, Map> imageParam) throws IOException, TemplateException {
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
            String yamlContent = renderTemplate(k8sYaml, values);
            ByteArrayInputStream ymlStream = new ByteArrayInputStream(yamlContent.getBytes());
            streams.add(ymlStream);
        }
        return streams;
    }

    //初始化接口
    public List<ByteArrayInputStream> initNew(ModelMessage modelMessage, List<String> evaluationTypes, String imageName) throws IOException, TemplateException {
        List<ByteArrayInputStream> streams = new ArrayList<>();
        for (String evaluationType : evaluationTypes) {
            //pod命名方式：url+用户id
            String podName = modelMessage.getUserId() + "-" + modelMessage.getId() + "-" + evaluationType.toLowerCase();
            log.info("初始化接口：Pod的名称-" + podName);
            boolean podExist = redisTemplate.hasKey(modelMessage.getUserId()+":"+podName);
            //如果已存在，继续创建下一个pod
            if(podExist) {
                log.info("初始化接口：该Pod之前已启动-" + podName);
                return null;
            }
            if(imageName == null){

                // 测试
//                imageName = "nginx:latest";
//                imageName = "10.195.9.104:5000/sec_ai_image";
                imageName = registryHost + "/" + modelMessage.getId();

            }
            //准备模板变量
            Map<String, String> values = new HashMap<>();
            values.put("podName", podName);
            values.put("containerName", podName);
            values.put("imageName", imageName);
            values.put("userData", userData);
            values.put("evaluationData", evaluationData);
            values.put("evaluationType", evaluationType);
            values.put("systemData", systemData);
            values.put("nfsIP",nfsIp);
            values.put("rootPath",rootPath);
            values.put("userId",String.valueOf(modelMessage.getUserId()));
            values.put("modelId",String.valueOf(modelMessage.getId()));

            //生成填充好的yml文件字节流
            String yamlContent = renderTemplate(k8sAdversarialGpuYaml, values);
//            String yamlContent = renderTemplate(k8sAdversarialYaml, values);
            ByteArrayInputStream ymlStream = new ByteArrayInputStream(yamlContent.getBytes());
            streams.add(ymlStream);


        }
        return streams;
    }

    //启动接口
    public void start(Long userId, Long modelId, List<ByteArrayInputStream> streams) throws IOException {
        for (ByteArrayInputStream stream : streams) {
            //获取容器的名字
            String containerName = getName(stream);
            taskExecutor.execute(() -> {
                //创建容器
                HasMetadata metadata = K8sClient.resource(stream).inNamespace("default").create();
                watchStatus(userId, modelId, containerName);
            });
        }
    }

    //监听接口1-持续监听指定的容器状态
    public void watchStatus(Long userId, Long modelId, String containerName) {
        final CountDownLatch closeLatch = new CountDownLatch(1);
        Watch watch = K8sClient.pods().inNamespace("default").withName(containerName).watch(new Watcher<Pod>() {
            @Override
            public void eventReceived(Action action, Pod pod) {
                String phase = pod.getStatus().getPhase();
                log.info("action: " + action +" phase：" + phase);

                Container container = Container.builder()
                        .containerName(containerName)
                        .nameSpace(pod.getMetadata().getNamespace())
                        .status(pod.getStatus().getPhase())
                        .restarts(0)
                        .AGE(String.valueOf(Duration.between(
                                OffsetDateTime.parse(pod.getMetadata().getCreationTimestamp()).toInstant(),
                                Instant.now()).getSeconds()))
                        .nodeName(pod.getStatus().getNominatedNodeName())
                        .imageId(0L)
                        .modelId(modelId)
                        .updateTime(LocalDateTime.now())
                        .build();

                switch (action) {
                    case ADDED:
                    case MODIFIED: {
                        if(phase.equals("Running")) {
                            log.info("启动接口：已启动的Pod名称-" + containerName);
                            String containerKey = userId + ":" +containerName;
                            //1.保存容器实例到redis中
                            redisTemplate.opsForValue().set(containerKey, container);
                            log.info("启动接口：容器实例已记录到Redis中-" + containerName);
                            //2.保存容器实例到mysql中
                            Container existContainer = containerMapper.selectOne(new LambdaQueryWrapper<Container>()
                                    .eq(Container::getContainerName, containerName));
                            if(existContainer != null) {
                                //如果当前容器实例已存在，则更新
                                containerMapper.update(container, new LambdaQueryWrapper<Container>()
                                        .eq(Container::getContainerName, containerName));
                            }else {
                                //如果当前容器实例不存在，则插入一条新的容器实例
                                containerMapper.insert(container);
                            }
//                        } else if (phase.equals("Succeeded") || phase.equals("Failed")) {
                        } else if (phase.equals("Succeeded")) {
                            deleteSingle(userId, containerName);
                            Container existContainer = containerMapper.selectOne(new LambdaQueryWrapper<Container>()
                                    .eq(Container::getContainerName, containerName));
                            if(existContainer != null) {
                                //如果当前容器实例已存在，则更新
                                containerMapper.update(container, new LambdaQueryWrapper<Container>()
                                        .eq(Container::getContainerName, containerName));
                            }else {
                                //如果当前容器实例不存在，则插入一条新的容器实例
                                containerMapper.insert(container);
                            }
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
    }

    //监听接口2-只输出一次容器的状态
    public String getStatus(String containerName) {
        String name = containerMapper.getStatusByContainerName(containerName);
        if (name == null)
            return containerName+"容器不存在";
        return name;
    }

    //回收接口1-删除指定用户的所有容器
    public void deleteAll(Long userId) {
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

    //回收接口2-删除用户的单个容器
    public void deleteSingle(Long userId, String containerName) {
        String containerKey = userId + ":" + containerName;
        redisTemplate.delete(containerKey);
        K8sClient.pods().inNamespace("default").withName(containerName).delete();
        log.info("回收接口：已删除Pod-" + containerName);
    }


    public List<String> getContainersByModelId(Long modelId) {
        return containerMapper.getContainerNameByModelId(modelId);
    }


}
