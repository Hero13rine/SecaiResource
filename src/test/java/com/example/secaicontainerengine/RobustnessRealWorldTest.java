package com.example.secaicontainerengine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.*;

/**
 * 使用真实的评测回传 JSON 测试解析逻辑
 */
public class RobustnessRealWorldTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testRealWorldJson() throws Exception {
        System.out.println("=================================");
        System.out.println("真实场景测试：完整的评测回传 JSON");
        System.out.println("=================================\n");

        // 用户提供的真实 JSON（完整版本）
        String fullJson = """
            {
              "modelId": 123,
              "resultColumn": "robustnessResult",
              "result": {
                "robustness": {
                  "adversarial": [
                    {
                      "attack": "fgsm",
                      "eps": 0.001,
                      "metrics": {
                        "clean_map": 0.86,
                        "adversarial_map": 0.81,
                        "map_drop_rate": 0.058,
                        "miss_rate": 0.25,
                        "false_detection_rate": 0.44,
                        "clean_miss_rate": 0.23,
                        "clean_false_detection_rate": 0.45,
                        "per_class_clean_map": {
                          "1": 0.98,
                          "2": 0.85
                        },
                        "per_class_adversarial_map": {
                          "1": 0.97,
                          "2": 0.78
                        }
                      }
                    },
                    {
                      "attack": "pgd",
                      "eps": 0.003,
                      "metrics": {
                        "clean_map": 0.86,
                        "adversarial_map": 0.75,
                        "map_drop_rate": 0.128,
                        "miss_rate": 0.32,
                        "false_detection_rate": 0.51,
                        "clean_miss_rate": 0.23,
                        "clean_false_detection_rate": 0.45,
                        "per_class_clean_map": {
                          "1": 0.98,
                          "2": 0.85
                        },
                        "per_class_adversarial_map": {
                          "1": 0.91,
                          "2": 0.68
                        }
                      }
                    }
                  ],
                  "corruptions": [
                    {
                      "corruption_name": "gaussian_noise",
                      "severity": 1,
                      "metrics": {
                        "perturbation_magnitude": 0.04,
                        "performance_drop_rate": 0.06,
                        "perturbation_tolerance": 0.92
                      }
                    },
                    {
                      "corruption_name": "gaussian_blur",
                      "severity": 3,
                      "metrics": {
                        "perturbation_magnitude": 0.034,
                        "performance_drop_rate": 0.377,
                        "perturbation_tolerance": 0.497
                      }
                    }
                  ]
                }
              }
            }
        """;

        System.out.println("【步骤1】接收到的完整 JSON");
        System.out.println("modelId: 123");
        System.out.println("resultColumn: robustnessResult");
        System.out.println("包含 adversarial 攻击: 2个");
        System.out.println("包含 corruptions 测试: 2个\n");

        // 解析 JSON
        JsonNode fullNode = objectMapper.readTree(fullJson);
        JsonNode resultNode = fullNode.get("result");
        String robustnessJsonStr = resultNode.get("robustness").toString();

        System.out.println("【步骤2】提取 robustness 字段");
        System.out.println(robustnessJsonStr);
        System.out.println();

        // 模拟 parseAndStoreRobustness 的逻辑
        JsonNode rootNode = objectMapper.readTree(robustnessJsonStr);
        Map<String, String> flattenedData = new LinkedHashMap<>();

        System.out.println("【步骤3】开始解析并展开存储");
        System.out.println();

        // 处理 adversarial 数组
        int advCount = 0;
        if (rootNode.has("adversarial") && rootNode.get("adversarial").isArray()) {
            JsonNode adversarialArray = rootNode.get("adversarial");
            System.out.println(">> 处理 adversarial 数组（共 " + adversarialArray.size() + " 个攻击）");

            Set<String> excludedFields = new HashSet<>(Arrays.asList(
                "attack", "eps", "attack_name"
            ));

            for (JsonNode attack : adversarialArray) {
                String attackName;
                if (attack.has("attack") && attack.has("eps")) {
                    String attackType = attack.get("attack").asText();
                    String eps = attack.get("eps").asText();
                    attackName = attackType + "_eps_" + eps;
                } else if (attack.has("attack_name")) {
                    attackName = attack.get("attack_name").asText();
                } else {
                    continue;
                }

                System.out.println("   攻击: " + attackName);

                JsonNode metricsNode = attack.has("metrics") ? attack.get("metrics") : attack;

                Iterator<Map.Entry<String, JsonNode>> fields = metricsNode.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    String metricName = field.getKey();

                    if (excludedFields.contains(metricName)) {
                        continue;
                    }

                    JsonNode valueNode = field.getValue();

                    if (valueNode.isObject()) {
                        String key = metricName + "_" + attackName;
                        String value = objectMapper.writeValueAsString(valueNode);
                        flattenedData.put(key, value);
                        advCount++;
                        System.out.println("      ✓ " + key + " = " + value);
                    } else {
                        String key = metricName + "_" + attackName;
                        String value = valueNode.asText();
                        flattenedData.put(key, value);
                        advCount++;
                        System.out.println("      ✓ " + key + " = " + value);
                    }
                }
                System.out.println();
            }
            System.out.println("   已展开存储 " + advCount + " 个 adversarial 指标\n");
        }

        // 处理 corruptions 数组
        int corrCount = 0;
        if (rootNode.has("corruptions") && rootNode.get("corruptions").isArray()) {
            JsonNode corruptionArray = rootNode.get("corruptions");
            System.out.println(">> 处理 corruptions 数组（共 " + corruptionArray.size() + " 个测试）");

            Set<String> excludedFields = new HashSet<>(Arrays.asList(
                "corruption_name", "corruption_key", "severity"
            ));

            for (JsonNode corruption : corruptionArray) {
                if (!corruption.has("corruption_name")) {
                    continue;
                }
                String corruptionName = corruption.get("corruption_name").asText();
                String severity = corruption.has("severity") ?
                    corruption.get("severity").asText() : "";
                String suffix = corruptionName + (severity.isEmpty() ? "" : "_" + severity);

                System.out.println("   腐败测试: " + suffix);

                JsonNode metricsNode = corruption.has("metrics") ? corruption.get("metrics") : corruption;

                Iterator<Map.Entry<String, JsonNode>> fields = metricsNode.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    String metricName = field.getKey();

                    if (excludedFields.contains(metricName)) {
                        continue;
                    }

                    JsonNode valueNode = field.getValue();

                    if (valueNode.isObject()) {
                        String key = metricName + "_" + suffix;
                        String value = objectMapper.writeValueAsString(valueNode);
                        flattenedData.put(key, value);
                        corrCount++;
                        System.out.println("      ✓ " + key + " = " + value);
                    } else {
                        String key = metricName + "_" + suffix;
                        String value = valueNode.asText();
                        flattenedData.put(key, value);
                        corrCount++;
                        System.out.println("      ✓ " + key + " = " + value);
                    }
                }
                System.out.println();
            }
            System.out.println("   已展开存储 " + corrCount + " 个 corruption 指标\n");
        }

        // 显示最终存储结果
        System.out.println("【步骤4】最终数据库存储格式（robustnessResult 列）");
        System.out.println("{");
        int count = 0;
        for (Map.Entry<String, String> entry : flattenedData.entrySet()) {
            count++;
            String comma = count < flattenedData.size() ? "," : "";
            String displayValue = entry.getValue();
            if (displayValue.length() > 50) {
                displayValue = displayValue.substring(0, 47) + "...";
            }
            System.out.println("  \"" + entry.getKey() + "\": \"" + displayValue + "\"" + comma);
        }
        System.out.println("}");
        System.out.println();

        // 验证结果
        System.out.println("【步骤5】验证解析结果");
        System.out.println("✓ 总共存储字段数: " + flattenedData.size());
        System.out.println("✓ Adversarial 指标: " + advCount);
        System.out.println("✓ Corruption 指标: " + corrCount);
        System.out.println();

        // 验证关键字段
        String[] criticalFields = {
            "clean_map_fgsm_eps_0.001",
            "adversarial_map_fgsm_eps_0.001",
            "map_drop_rate_fgsm_eps_0.001",
            "miss_rate_fgsm_eps_0.001",
            "false_detection_rate_fgsm_eps_0.001",
            "clean_miss_rate_fgsm_eps_0.001",
            "clean_false_detection_rate_fgsm_eps_0.001",
            "per_class_clean_map_fgsm_eps_0.001",
            "per_class_adversarial_map_fgsm_eps_0.001",
            "clean_map_pgd_eps_0.003",
            "adversarial_map_pgd_eps_0.003",
            "perturbation_magnitude_gaussian_noise_1",
            "performance_drop_rate_gaussian_noise_1",
            "perturbation_tolerance_gaussian_noise_1",
            "perturbation_magnitude_gaussian_blur_3",
            "performance_drop_rate_gaussian_blur_3",
            "perturbation_tolerance_gaussian_blur_3"
        };

        System.out.println("【步骤6】关键字段检查");
        boolean allPresent = true;
        for (String field : criticalFields) {
            if (flattenedData.containsKey(field)) {
                System.out.println("  ✓ " + field);
            } else {
                System.out.println("  ✗ " + field + " [缺失]");
                allPresent = false;
            }
        }
        System.out.println();

        // 验证嵌套对象
        System.out.println("【步骤7】嵌套对象验证");
        String perClassMap = flattenedData.get("per_class_clean_map_fgsm_eps_0.001");
        System.out.println("per_class_clean_map_fgsm_eps_0.001 = " + perClassMap);

        JsonNode perClassNode = objectMapper.readTree(perClassMap);
        System.out.println("  ✓ 是对象: " + perClassNode.isObject());
        System.out.println("  ✓ 包含类别 '1': " + perClassNode.has("1"));
        System.out.println("  ✓ 类别 '1' 的值: " + perClassNode.get("1").asDouble());
        System.out.println();

        // 总结
        System.out.println("=================================");
        if (allPresent && flattenedData.size() == 24) {
            System.out.println("✅✅✅ 测试完全通过！");
            System.out.println("✅ 所有 " + flattenedData.size() + " 个指标完整存储");
            System.out.println("✅ 攻击方法信息完整保留");
            System.out.println("✅ 嵌套对象正确序列化");
            System.out.println("✅ 没有任何信息丢失");
        } else {
            System.out.println("❌ 测试失败");
            System.out.println("预期: 24 个字段");
            System.out.println("实际: " + flattenedData.size() + " 个字段");
        }
        System.out.println("=================================");

        // 断言
        assert flattenedData.size() == 24 : "应该有24个字段（2个攻击×9指标 + 2个腐败×3指标）";
        assert allPresent : "所有关键字段都应该存在";
    }
}
