package com.example.secaicontainerengine.util;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class PodUtil {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private KubernetesClient K8sClient;

    //判断pod是否已经存在yujiqun
    public boolean isExistCluster(String podName) {
        if(podName == null) {
            return false;
        }
        PodList podList = K8sClient.pods().inNamespace("default").list();
        for (Pod pod : podList.getItems()) {
            if (pod.getMetadata().getName().equals(podName)) {
                return true;
            }
        }
        return false;
    }



}
