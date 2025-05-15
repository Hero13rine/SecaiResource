package com.example.secaicontainerengine.service.container;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.secaicontainerengine.mapper.ContainerMapper;
import com.example.secaicontainerengine.mapper.EvaluationMethodMapper;
import com.example.secaicontainerengine.mapper.EvaluationResultMapper;
import com.example.secaicontainerengine.pojo.dto.model.BusinessConfig;
import com.example.secaicontainerengine.pojo.dto.model.ResourceConfig;
import com.example.secaicontainerengine.pojo.entity.*;
import com.example.secaicontainerengine.service.modelEvaluation.EvaluationResultService;
import com.example.secaicontainerengine.service.modelEvaluation.ModelEvaluationService;
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
import org.springframework.context.annotation.Lazy;
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

    @Autowired
    private EvaluationResultService evaluationResultService;

    @Autowired
    private EvaluationMethodMapper evaluationMethodMapper;
    @Autowired
    private EvaluationResultMapper evaluationResultMapper;
    @Lazy
    @Autowired
    private ModelEvaluationService modelEvaluationService;

    @Value("${k8s.evaluation-resources.limits.gpu-memory}")
    private Integer evaluationGpuMemory;

    @Value("${k8s.evaluation-resources.limits.gpu-core}")
    private Integer evaluationGpuCore;

    @Value("${k8s.evaluation-resources.limits.gpu-num}")
    private Integer evaluationGpuNum;

    //初始化接口
    public List<ByteArrayInputStream> init(Long userId, Map<String, String> imageUrl, Map<String, Map> imageParam) throws IOException, TemplateException {
        List<ByteArrayInputStream> streams = new ArrayList<>();
        for(String value: imageUrl.values()){
            //pod命名方式：url+用户id
            String podName = value+userId;
            log.info("初始化接口：Pod的名称-" + podName);
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
    public List<ByteArrayInputStream> initNew(ModelMessage modelMessage, List<String> evaluationTypes) throws IOException, TemplateException {
        List<ByteArrayInputStream> streams = new ArrayList<>();
        for (String evaluationType : evaluationTypes) {
            //pod命名方式：模型id-用户id-评测方法
            String podName = modelMessage.getUserId() + "-" + modelMessage.getId() + "-" + evaluationType.toLowerCase();
            log.info("初始化接口：Pod的名称-" + podName);
            String imageName = registryHost + "/" + modelMessage.getId();

            ResourceConfig podResourceLimits = calculatePodResourceFromModel(modelMessage);
            String gpuCoreLimits = podResourceLimits.getGpuCoreRequired().toString();
            String gpuMemoryLimits = podResourceLimits.getGpuMemoryRequired().toString();
            String gpuNumLimits = podResourceLimits.getGpuNumRequired().toString();

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
            values.put("gpuCoreLimits",gpuCoreLimits);
            values.put("gpuMemoryLimits",gpuMemoryLimits);
            values.put("gpuNumLimits",gpuNumLimits);

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

        // 1.初始化CountDownLatch数量
        CountDownLatch latch = new CountDownLatch(streams.size());


        for (ByteArrayInputStream stream : streams) {
            // 2.获取容器的名字
            String containerName = getName(stream);

            // 3.获取到pod对应的评测任务对应的评测方法
            String evaluateMethod = containerName.split("-")[2];
            QueryWrapper<EvaluationMethod> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("methodName", evaluateMethod);
            EvaluationMethod evaluationMethod = evaluationMethodMapper.selectOne(queryWrapper);
            Long evaluationMethodId = evaluationMethod.getId();

            // 4.启动评测任务
            taskExecutor.execute(() -> {
                // 记录开始时间
                long startTime = System.currentTimeMillis();

                // 修改单次评测任务表评测状态
                EvaluationResult evaluationResult = EvaluationResult.builder()
                        .evaluateMethodId(evaluationMethodId)
                        .userId(userId)
                        .modelId(modelId)
                        .status("评测中")
                        .build();
                evaluationResultMapper.insert(evaluationResult);

                // 创建容器
                HasMetadata metadata = K8sClient.resource(stream).inNamespace("default").create();

                // 记录结束时间
                long endTime = System.currentTimeMillis();

                // 评测任务开启到创建pod命令发出的时间
                long executionTime = endTime - startTime;

                System.out.println("开启pod花费时间: " + executionTime + " 毫秒");
                watchStatus(userId, modelId, containerName);
                latch.countDown();
            });
        }
        try {
            // 最多等待60分钟
            boolean allCompleted = latch.await(60, TimeUnit.MINUTES);

            if (allCompleted) {
                // 7. 所有Pod完成后，执行统计分数逻辑
                evaluationResultService.calculateAndUpdateScores(modelId);
            } else {
                // 处理超时（如终止未完成的Pod、记录警告）
                modelEvaluationService.handleTimeout(modelId);
            }
        } catch (InterruptedException e) {
            // 处理中断（恢复中断状态）
            Thread.currentThread().interrupt();
            log.error("等待Pod完成时被中断");
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
//                            latch.countDown();
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

    //回收接口2-删除用户的单个容器
    public void deleteSingle(Long userId, String containerName) {
        K8sClient.pods().inNamespace("default").withName(containerName).delete();
        log.info("回收接口：已删除Pod-" + containerName);
    }


    public List<String> getContainersByModelId(Long modelId) {
        return containerMapper.getContainerNameByModelId(modelId);
    }

    @Override
    // 从用户填入的模型运行需要的资源信息计算模型评测开启的pod的资源信息
    public ResourceConfig calculatePodResourceFromModel(ModelMessage modelMessage){

        // 获取到具体的资源信息
        String resourceConfigStr = modelMessage.getResourceConfig();
        ResourceConfig resourceConfig = JSONUtil.toBean(resourceConfigStr, ResourceConfig.class);
        Integer gpuNumRequired = resourceConfig.getGpuNumRequired();
        Integer gpuMemoryRequired = resourceConfig.getGpuMemoryRequired();
        Integer gpuCoreRequired = resourceConfig.getGpuCoreRequired();

        // 计算pod相关yaml文件中limits资源
        Integer gpuMemoryLimits = gpuMemoryRequired + evaluationGpuMemory;
        Integer gpuCoreLimits = gpuCoreRequired + evaluationGpuCore;
        Integer gpuNumLimits = gpuNumRequired + evaluationGpuNum;

        // 返回
        ResourceConfig podResourceLimits = ResourceConfig.builder()
                .gpuMemoryRequired(gpuMemoryLimits)
                .gpuCoreRequired(gpuCoreLimits)
                .gpuNumRequired(gpuNumLimits)
                .build();

        return podResourceLimits;
    }


}
