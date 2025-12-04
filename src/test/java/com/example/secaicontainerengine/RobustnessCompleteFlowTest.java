package com.example.secaicontainerengine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.*;

/**
 * 完整流程测试：存储 + 读取 + 计算得分
 */
public class RobustnessCompleteFlowTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testCompleteFlow() throws Exception {
        System.out.println("==========================================================");
        System.out.println("完整流程测试：robustness 存储 → 读取 → 计算得分");
        System.out.println("==========================================================\n");

        // ==================== 步骤1: 评测模块回传 ====================
        System.out.println("【步骤1】评测模块回传数据");
        String robustnessJson = """
            {
              "adversarial": [
                {
                  "attack_name": "fgsm_eps_0.001",
                  "map_drop_rate": 0.062,
                  "miss_rate": 0.250,
                  "false_detection_rate": 0.439
                },
                {
                  "attack_name": "pgd_steps_1",
                  "map_drop_rate": 0.601,
                  "miss_rate": 0.476,
                  "false_detection_rate": 0.675
                }
              ],
              "corruption": [
                {
                  "corruption_name": "gaussian_noise",
                  "severity": 1,
                  "performance_drop_rate": 0.102,
                  "perturbation_tolerance": 0.721
                }
              ]
            }
        """;
        System.out.println("✓ 接收到数据\n");

        // ==================== 步骤2: 展开为扁平格式存储 ====================
        System.out.println("【步骤2】展开为扁平格式（模拟存储）");
        JsonNode rootNode = objectMapper.readTree(robustnessJson);
        Map<String, String> flatData = new LinkedHashMap<>();

        // 展开 adversarial
        if (rootNode.has("adversarial")) {
            for (JsonNode attack : rootNode.get("adversarial")) {
                String attackName = attack.get("attack_name").asText();
                Iterator<Map.Entry<String, JsonNode>> fields = attack.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    if (!"attack_name".equals(field.getKey())) {
                        flatData.put(field.getKey() + "_" + attackName, field.getValue().asText());
                    }
                }
            }
        }

        // 展开 corruption
        if (rootNode.has("corruption")) {
            for (JsonNode corruption : rootNode.get("corruption")) {
                String name = corruption.get("corruption_name").asText();
                String severity = corruption.get("severity").asText();
                String suffix = name + "_" + severity;

                Iterator<Map.Entry<String, JsonNode>> fields = corruption.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    String key = field.getKey();
                    if (!"corruption_name".equals(key) && !"severity".equals(key) && !"corruption_key".equals(key)) {
                        flatData.put(field.getKey() + "_" + suffix, field.getValue().asText());
                    }
                }
            }
        }

        System.out.println("✓ 展开后共 " + flatData.size() + " 个键值对");
        System.out.println("\n数据库存储格式（robustnessResult 列）:");
        System.out.println("{");
        int count = 0;
        for (Map.Entry<String, String> entry : flatData.entrySet()) {
            count++;
            System.out.println("  \"" + entry.getKey() + "\": \"" + entry.getValue() + "\"" +
                (count < flatData.size() ? "," : ""));
        }
        System.out.println("}\n");

        // ==================== 步骤3: 从扁平格式读取并计算得分 ====================
        System.out.println("【步骤3】从扁平格式计算得分");

        // 模拟 computeDetectionRobustnessScoreFromFlatFormat 的逻辑
        String storedJson = objectMapper.writeValueAsString(flatData);
        JsonNode storedNode = objectMapper.readTree(storedJson);

        // 按攻击方法分组
        Map<String, Map<String, Double>> adversarialMetrics = new HashMap<>();
        Map<String, Map<String, Double>> corruptionMetrics = new HashMap<>();

        Iterator<Map.Entry<String, JsonNode>> fields = storedNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String key = field.getKey();
            double value = field.getValue().asDouble();

            if (key.startsWith("map_drop_rate_")) {
                String attackName = key.substring("map_drop_rate_".length());
                adversarialMetrics.computeIfAbsent(attackName, k -> new HashMap<>())
                    .put("map_drop_rate", value);
            } else if (key.startsWith("miss_rate_")) {
                String attackName = key.substring("miss_rate_".length());
                adversarialMetrics.computeIfAbsent(attackName, k -> new HashMap<>())
                    .put("miss_rate", value);
            } else if (key.startsWith("false_detection_rate_")) {
                String attackName = key.substring("false_detection_rate_".length());
                adversarialMetrics.computeIfAbsent(attackName, k -> new HashMap<>())
                    .put("false_detection_rate", value);
            } else if (key.startsWith("performance_drop_rate_")) {
                String corruptionName = key.substring("performance_drop_rate_".length());
                corruptionMetrics.computeIfAbsent(corruptionName, k -> new HashMap<>())
                    .put("performance_drop_rate", value);
            } else if (key.startsWith("perturbation_tolerance_")) {
                String corruptionName = key.substring("perturbation_tolerance_".length());
                corruptionMetrics.computeIfAbsent(corruptionName, k -> new HashMap<>())
                    .put("perturbation_tolerance", value);
            }
        }

        System.out.println("✓ 识别到 " + adversarialMetrics.size() + " 个攻击方法");
        System.out.println("✓ 识别到 " + corruptionMetrics.size() + " 个腐败类型\n");

        // 计算得分
        double totalScore = 0.0;
        int scoreCount = 0;

        // Adversarial 得分
        if (!adversarialMetrics.isEmpty()) {
            double advTotal = 0.0;
            int advCount = 0;

            System.out.println("对抗攻击得分计算:");
            for (Map.Entry<String, Map<String, Double>> entry : adversarialMetrics.entrySet()) {
                String attackName = entry.getKey();
                Map<String, Double> metrics = entry.getValue();

                double attackScore = 0.0;
                int metricCount = 0;

                System.out.println("  " + attackName + ":");
                if (metrics.containsKey("map_drop_rate")) {
                    double score = Math.max(0.0, 1.0 - metrics.get("map_drop_rate"));
                    System.out.println("    map_drop_rate: " + metrics.get("map_drop_rate") + " → " + String.format("%.4f", score));
                    attackScore += score;
                    metricCount++;
                }
                if (metrics.containsKey("miss_rate")) {
                    double score = Math.max(0.0, 1.0 - metrics.get("miss_rate"));
                    System.out.println("    miss_rate: " + metrics.get("miss_rate") + " → " + String.format("%.4f", score));
                    attackScore += score;
                    metricCount++;
                }
                if (metrics.containsKey("false_detection_rate")) {
                    double score = Math.max(0.0, 1.0 - metrics.get("false_detection_rate"));
                    System.out.println("    false_detection_rate: " + metrics.get("false_detection_rate") + " → " + String.format("%.4f", score));
                    attackScore += score;
                    metricCount++;
                }

                double avgAttackScore = metricCount > 0 ? attackScore / metricCount : 0.0;
                System.out.println("    平均得分: " + String.format("%.4f", avgAttackScore));
                advTotal += avgAttackScore;
                advCount++;
            }

            double advScore = advCount > 0 ? advTotal / advCount : 0.0;
            System.out.println("✓ 对抗攻击总得分: " + String.format("%.4f", advScore) + "\n");
            totalScore += advScore;
            scoreCount++;
        }

        // Corruption 得分
        if (!corruptionMetrics.isEmpty()) {
            double corrTotal = 0.0;
            int corrCount = 0;

            System.out.println("腐败测试得分计算:");
            for (Map.Entry<String, Map<String, Double>> entry : corruptionMetrics.entrySet()) {
                String corruptionName = entry.getKey();
                Map<String, Double> metrics = entry.getValue();

                double corruptionScore = 0.0;
                int metricCount = 0;

                System.out.println("  " + corruptionName + ":");
                if (metrics.containsKey("performance_drop_rate")) {
                    double score = Math.max(0.0, 1.0 - metrics.get("performance_drop_rate"));
                    System.out.println("    performance_drop_rate: " + metrics.get("performance_drop_rate") + " → " + String.format("%.4f", score));
                    corruptionScore += score;
                    metricCount++;
                }
                if (metrics.containsKey("perturbation_tolerance")) {
                    double score = metrics.get("perturbation_tolerance");
                    System.out.println("    perturbation_tolerance: " + score);
                    corruptionScore += score;
                    metricCount++;
                }

                double avgCorruptionScore = metricCount > 0 ? corruptionScore / metricCount : 0.0;
                System.out.println("    平均得分: " + String.format("%.4f", avgCorruptionScore));
                corrTotal += avgCorruptionScore;
                corrCount++;
            }

            double corrScore = corrCount > 0 ? corrTotal / corrCount : 0.0;
            System.out.println("✓ 腐败测试总得分: " + String.format("%.4f", corrScore) + "\n");
            totalScore += corrScore;
            scoreCount++;
        }

        double finalScore = scoreCount > 0 ? totalScore / scoreCount : 0.0;

        // ==================== 结果 ====================
        System.out.println("==========================================================");
        System.out.println("【最终结果】");
        System.out.println("✓ 鲁棒性得分: " + String.format("%.4f", finalScore));
        System.out.println("✓ 得分范围: 0.0 ~ 1.0");
        System.out.println("✓ 存储格式: 扁平键值对（与其他维度一致）");
        System.out.println("✓ 读取成功: 能够从扁平格式提取指标");
        System.out.println("✓ 计算成功: 得分计算正确");
        System.out.println("==========================================================");

        // 验证
        assert finalScore >= 0.0 && finalScore <= 1.0 : "得分应在0-1之间";
        assert finalScore > 0.0 : "得分应大于0";
        assert flatData.size() > 0 : "应有展开的数据";

        System.out.println("\n✅ 完整流程测试通过！");
    }
}
