package com.example.secaicontainerengine.config;


import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;

@Configuration
public class K8sConfiguration {

    @Bean
    public KubernetesClient K8sClient() {
        InputStream kubeConfig = getClass().getClassLoader().getResourceAsStream(".kube/config");
        return new KubernetesClientBuilder()
                .withConfig(kubeConfig)
                .build();
    }
}
