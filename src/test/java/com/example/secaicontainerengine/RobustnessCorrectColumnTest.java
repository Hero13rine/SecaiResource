package com.example.secaicontainerengine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.*;

/**
 * 测试使用正确的 resultColumn = "robustnessResult" 时的解析效果
 */
public class RobustnessCorrectColumnTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testWithCorrectResultColumn() throws Exception {
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║   测试：使用正确的 resultColumn = \"robustnessResult\"         ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
        System.out.println();

        // 完整的评测回传 JSON（使用正确的 resultColumn）
        String requestJson = """
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
                        "clean_false_detection_rate": 0.45
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
                    }
                  ]
                }
              }
            }
        """;

        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│ 步骤1：解析请求 JSON                                         │");
        System.out.println("└─────────────────────────────────────────────────────────────┘");

        JsonNode requestNode = objectMapper.readTree(requestJson);
        Long modelId = requestNode.get("modelId").asLong();
        String resultColumn = requestNode.get("resultColumn").asText();
        JsonNode resultNode = requestNode.get("result");

        System.out.println("modelId: " + modelId);
        System.out.println("resultColumn: " + resultColumn);
        System.out.println();

        // 模拟 Spring 将 result 转换为 Map<String, String>
        Map<String, String> result = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = resultNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String key = entry.getKey();
            String value = entry.getValue().toString(); // 序列化为字符串
            result.put(key, value);
        }

        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│ 步骤2：检查是否触发特殊解析逻辑                              │");
        System.out.println("└─────────────────────────────────────────────────────────────┘");

        // 模拟 updateResult 方法的判断逻辑（第377行）
        boolean triggerSpecialParsing = "robustnessResult".equals(resultColumn) && result.containsKey("robustness");

        System.out.println("判断条件 1: \"robustnessResult\".equals(resultColumn)");
        System.out.println("  resultColumn = \"" + resultColumn + "\"");
        System.out.println("  判断结果: " + "robustnessResult".equals(resultColumn));
        System.out.println();

        System.out.println("判断条件 2: result.containsKey(\"robustness\")");
        System.out.println("  result 的键: " + result.keySet());
        System.out.println("  判断结果: " + result.containsKey("robustness"));
        System.out.println();

        System.out.println("综合判断: " + (triggerSpecialParsing ? "✅ 触发特殊解析" : "❌ 不触发"));
        System.out.println();

        if (!triggerSpecialParsing) {
            System.out.println("❌ 测试失败：没有触发特殊解析逻辑！");
            assert false : "应该触发特殊解析";
            return;
        }

        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│ 步骤3：执行 parseAndStoreRobustness 逻辑                     │");
        System.out.println("└─────────────────────────────────────────────────────────────┘");

        String robustnessJsonStr = result.get("robustness");
        System.out.println("提取 robustness 字段:");
        System.out.println(robustnessJsonStr.substring(0, Math.min(100, robustnessJsonStr.length())) + "...");
        System.out.println();

        // 解析并展开
        JsonNode rootNode = objectMapper.readTree(robustnessJsonStr);
        Map<String, String> flattenedData = new LinkedHashMap<>();

        System.out.println("开始展开存储...");
        System.out.println();

        // 处理 adversarial 数组
        int advCount = 0;
        if (rootNode.has("adversarial") && rootNode.get("adversarial").isArray()) {
            JsonNode adversarialArray = rootNode.get("adversarial");
            System.out.println("▶ 处理 adversarial 数组（共 " + adversarialArray.size() + " 个攻击）");

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

                System.out.println("  攻击: " + attackName);

                JsonNode metricsNode = attack.has("metrics") ? attack.get("metrics") : attack;

                Iterator<Map.Entry<String, JsonNode>> metricsFields = metricsNode.fields();
                while (metricsFields.hasNext()) {
                    Map.Entry<String, JsonNode> field = metricsFields.next();
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
                    } else {
                        String key = metricName + "_" + attackName;
                        String value = valueNode.asText();
                        flattenedData.put(key, value);
                        advCount++;
                    }
                }
            }
            System.out.println("  ✅ 已存储 " + advCount + " 个 adversarial 指标");
            System.out.println();
        }

        // 处理 corruptions 数组
        int corrCount = 0;
        if (rootNode.has("corruptions") && rootNode.get("corruptions").isArray()) {
            JsonNode corruptionArray = rootNode.get("corruptions");
            System.out.println("▶ 处理 corruptions 数组（共 " + corruptionArray.size() + " 个测试）");

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

                System.out.println("  腐败测试: " + suffix);

                JsonNode metricsNode = corruption.has("metrics") ? corruption.get("metrics") : corruption;

                Iterator<Map.Entry<String, JsonNode>> metricsFields = metricsNode.fields();
                while (metricsFields.hasNext()) {
                    Map.Entry<String, JsonNode> field = metricsFields.next();
                    String metricName = field.getKey();

                    if (excludedFields.contains(metricName)) {
                        continue;
                    }

                    JsonNode valueNode = field.getValue();
                    String key = metricName + "_" + suffix;
                    String value = valueNode.asText();
                    flattenedData.put(key, value);
                    corrCount++;
                }
            }
            System.out.println("  ✅ 已存储 " + corrCount + " 个 corruption 指标");
            System.out.println();
        }

        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│ 步骤4：最终数据库存储结果（robustnessResult 列）             │");
        System.out.println("└─────────────────────────────────────────────────────────────┘");

        System.out.println("{");
        int count = 0;
        for (Map.Entry<String, String> entry : flattenedData.entrySet()) {
            count++;
            String comma = count < flattenedData.size() ? "," : "";
            String displayValue = entry.getValue();
            if (displayValue.length() > 40) {
                displayValue = displayValue.substring(0, 37) + "...";
            }
            System.out.println("  \"" + entry.getKey() + "\": \"" + displayValue + "\"" + comma);
        }
        System.out.println("}");
        System.out.println();

        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│ 步骤5：验证结果                                              │");
        System.out.println("└─────────────────────────────────────────────────────────────┘");

        System.out.println("✅ 总字段数: " + flattenedData.size());
        System.out.println("✅ Adversarial 指标: " + advCount);
        System.out.println("✅ Corruption 指标: " + corrCount);
        System.out.println();

        // 验证关键字段
        String[] criticalFields = {
            "clean_map_fgsm_eps_0.001",
            "adversarial_map_fgsm_eps_0.001",
            "map_drop_rate_fgsm_eps_0.001",
            "per_class_clean_map_fgsm_eps_0.001",
            "clean_map_pgd_eps_0.003",
            "adversarial_map_pgd_eps_0.003",
            "perturbation_magnitude_gaussian_noise_1",
            "performance_drop_rate_gaussian_noise_1"
        };

        System.out.println("关键字段验证:");
        boolean allPresent = true;
        for (String field : criticalFields) {
            boolean present = flattenedData.containsKey(field);
            System.out.println("  " + (present ? "✅" : "❌") + " " + field);
            if (!present) allPresent = false;
        }
        System.out.println();

        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║                    🎉 测试结果                                ║");
        System.out.println("╠═══════════════════════════════════════════════════════════════╣");
        if (allPresent && flattenedData.size() == 23) {
            System.out.println("║  ✅✅✅ 测试完全通过！                                        ║");
            System.out.println("║  ✅ resultColumn 正确触发解析                                ║");
            System.out.println("║  ✅ 所有 " + String.format("%-2d", flattenedData.size()) + " 个指标完整存储                                 ║");
            System.out.println("║  ✅ 攻击方法完全识别                                         ║");
            System.out.println("║  ✅ 没有任何信息丢失                                         ║");
        } else {
            System.out.println("║  ⚠️ 部分测试通过                                            ║");
            System.out.println("║  预期: 23 个字段                                            ║");
            System.out.println("║  实际: " + String.format("%-2d", flattenedData.size()) + " 个字段                                            ║");
        }
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");

        // 断言
        assert triggerSpecialParsing : "应该触发特殊解析";
        assert flattenedData.size() == 23 : "应该有23个字段（2个攻击×7-9指标 + 1个腐败×3指标）";
        assert allPresent : "所有关键字段都应该存在";
    }
}
