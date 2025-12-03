package com.example.secaicontainerengine.pojo.dto.model;

import lombok.Data;

import java.util.List;

@Data
public class BusinessConfig {

    // 每个维度的配置信息
    private List<EvaluationDimensionConfig> evaluateMethods;

    @Data
    public static class EvaluationDimensionConfig {
        private String dimension; // 如 basic、robustness 等
        private List<MethodMetricPair> methodMetricMap; // 测试方法和其对应的指标
    }

    @Data
    public static class MethodMetricPair {
        private String method;           // 测试方法名称
        private List<String> metrics;    // 对应的指标

        // 鲁棒性-对抗攻击专用字段（仅当 method="adversarial" 时使用）
        private List<String> attacks;    // 攻击方法列表，如 ["fgsm", "pgd"]
        private String fgsmEps;          // FGSM的epsilon参数范围，格式: "[start,end,step]"，如 "[0.001,0.01,0.001]"
        private String pgdSteps;         // PGD的迭代步数范围，格式: "[start,end,step]"，如 "[5,20,5]"

        // 鲁棒性-扰动攻击专用字段（仅当 method="corruption" 时使用）
        private List<String> corruptions; // 扰动方法列表，如 ["gaussian_noise", "gaussian_blur"]
    }

}
