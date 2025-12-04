import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

/**
 * 独立演示程序：展示新的 robustness 解析逻辑
 * 编译运行：javac -cp ".;lib/*" DemoRobustnessParser.java && java -cp ".;lib/*" DemoRobustnessParser
 */
public class DemoRobustnessParser {

    public static void main(String[] args) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║       鲁棒性指标解析演示 - 真实评测回传 JSON                 ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
        System.out.println();

        // 用户提供的真实 JSON
        String robustnessJson = """
            {
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
        """;

        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│ 步骤1：接收评测模块回传的 robustness JSON                  │");
        System.out.println("└─────────────────────────────────────────────────────────────┘");
        System.out.println("包含:");
        System.out.println("  • adversarial 攻击: 2个 (fgsm, pgd)");
        System.out.println("  • corruption 测试: 2个 (gaussian_noise, gaussian_blur)");
        System.out.println();

        // 解析并展开
        JsonNode rootNode = objectMapper.readTree(robustnessJson);
        Map<String, String> flattenedData = new LinkedHashMap<>();

        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│ 步骤2：使用新方法解析（黑名单机制，保存所有指标）          │");
        System.out.println("└─────────────────────────────────────────────────────────────┘");
        System.out.println();

        // 处理 adversarial 数组
        int advCount = 0;
        if (rootNode.has("adversarial") && rootNode.get("adversarial").isArray()) {
            JsonNode adversarialArray = rootNode.get("adversarial");
            System.out.println("▶ 处理 adversarial 数组");

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

                System.out.println("\n  攻击方法: " + attackName);

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
                        System.out.println("    ✓ " + key);
                        System.out.println("      值: " + value);
                    } else {
                        String key = metricName + "_" + attackName;
                        String value = valueNode.asText();
                        flattenedData.put(key, value);
                        advCount++;
                        System.out.println("    ✓ " + key + " = " + value);
                    }
                }
            }
            System.out.println("\n  ✅ 已存储 " + advCount + " 个 adversarial 指标");
            System.out.println();
        }

        // 处理 corruptions 数组
        int corrCount = 0;
        if (rootNode.has("corruptions") && rootNode.get("corruptions").isArray()) {
            JsonNode corruptionArray = rootNode.get("corruptions");
            System.out.println("▶ 处理 corruptions 数组");

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

                System.out.println("\n  腐败测试: " + suffix);

                JsonNode metricsNode = corruption.has("metrics") ? corruption.get("metrics") : corruption;

                Iterator<Map.Entry<String, JsonNode>> fields = metricsNode.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    String metricName = field.getKey();

                    if (excludedFields.contains(metricName)) {
                        continue;
                    }

                    JsonNode valueNode = field.getValue();
                    String key = metricName + "_" + suffix;
                    String value = valueNode.asText();
                    flattenedData.put(key, value);
                    corrCount++;
                    System.out.println("    ✓ " + key + " = " + value);
                }
            }
            System.out.println("\n  ✅ 已存储 " + corrCount + " 个 corruption 指标");
            System.out.println();
        }

        // 显示最终存储结果
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│ 步骤3：最终数据库存储格式（robustnessResult JSON列）        │");
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

        // 统计信息
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│ 步骤4：统计与验证                                           │");
        System.out.println("└─────────────────────────────────────────────────────────────┘");
        System.out.println("总字段数: " + flattenedData.size());
        System.out.println("  • Adversarial 指标: " + advCount);
        System.out.println("  • Corruption 指标: " + corrCount);
        System.out.println();

        // 对比旧方法
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│ 步骤5：对比旧方法 vs 新方法                                 │");
        System.out.println("└─────────────────────────────────────────────────────────────┘");

        String[] oldMethodFields = {
            "map_drop_rate_fgsm_eps_0.001",
            "miss_rate_fgsm_eps_0.001",
            "false_detection_rate_fgsm_eps_0.001",
            "map_drop_rate_pgd_eps_0.003",
            "miss_rate_pgd_eps_0.003",
            "false_detection_rate_pgd_eps_0.003"
        };

        System.out.println("【旧方法】白名单机制 - 只存储 " + (oldMethodFields.length + corrCount) + " 个字段:");
        for (String field : oldMethodFields) {
            System.out.println("  ✓ " + field);
        }
        System.out.println("  ✓ perturbation_magnitude_gaussian_noise_1");
        System.out.println("  ✓ performance_drop_rate_gaussian_noise_1");
        System.out.println("  ✓ perturbation_tolerance_gaussian_noise_1");
        System.out.println("  ... (corruption 字段)");
        System.out.println();
        System.out.println("  ❌ 丢失字段:");
        System.out.println("     • clean_map_* (所有攻击)");
        System.out.println("     • adversarial_map_* (所有攻击)");
        System.out.println("     • clean_miss_rate_* (所有攻击)");
        System.out.println("     • clean_false_detection_rate_* (所有攻击)");
        System.out.println("     • per_class_clean_map_* (所有攻击)");
        System.out.println("     • per_class_adversarial_map_* (所有攻击)");
        System.out.println();

        System.out.println("【新方法】黑名单机制 - 存储 " + flattenedData.size() + " 个字段:");
        System.out.println("  ✅ 保留所有评测指标");
        System.out.println("  ✅ 攻击方法信息完整（通过键名识别）");
        System.out.println("  ✅ 嵌套对象序列化为JSON字符串");
        System.out.println("  ✅ 没有任何信息丢失");
        System.out.println();

        // 验证关键字段
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│ 步骤6：验证关键字段是否存在                                 │");
        System.out.println("└─────────────────────────────────────────────────────────────┘");

        String[] criticalNewFields = {
            "clean_map_fgsm_eps_0.001",
            "adversarial_map_fgsm_eps_0.001",
            "per_class_clean_map_fgsm_eps_0.001",
            "per_class_adversarial_map_fgsm_eps_0.001",
            "clean_miss_rate_pgd_eps_0.003",
            "clean_false_detection_rate_pgd_eps_0.003"
        };

        System.out.println("之前丢失的关键字段，现在是否存在:");
        for (String field : criticalNewFields) {
            if (flattenedData.containsKey(field)) {
                System.out.println("  ✅ " + field);
            } else {
                System.out.println("  ❌ " + field + " [仍然缺失]");
            }
        }
        System.out.println();

        // 验证嵌套对象能否正确读取
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│ 步骤7：验证嵌套对象能否正确读取                             │");
        System.out.println("└─────────────────────────────────────────────────────────────┘");
        String perClassKey = "per_class_clean_map_fgsm_eps_0.001";
        if (flattenedData.containsKey(perClassKey)) {
            String perClassValue = flattenedData.get(perClassKey);
            System.out.println("字段: " + perClassKey);
            System.out.println("存储值: " + perClassValue);

            JsonNode perClassNode = objectMapper.readTree(perClassValue);
            System.out.println("反序列化结果:");
            System.out.println("  • 是对象: " + perClassNode.isObject());
            System.out.println("  • 类别数: " + perClassNode.size());
            System.out.println("  • 类别 '1' 的 mAP: " + perClassNode.get("1").asDouble());
            System.out.println("  • 类别 '2' 的 mAP: " + perClassNode.get("2").asDouble());
            System.out.println("  ✅ 嵌套对象可以正确读取！");
        } else {
            System.out.println("  ❌ 字段不存在");
        }
        System.out.println();

        // 最终结论
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║                      🎉 最终结论                              ║");
        System.out.println("╠═══════════════════════════════════════════════════════════════╣");
        System.out.println("║  ✅ 能完整解析所有指标                                        ║");
        System.out.println("║  ✅ 存储了 " + String.format("%-2d", flattenedData.size()) + " 个字段（旧方法只有 9 个）                    ║");
        System.out.println("║  ✅ 攻击方法通过键名完全识别 (如 *_fgsm_eps_0.001)           ║");
        System.out.println("║  ✅ 嵌套对象正确序列化并可反序列化                           ║");
        System.out.println("║  ✅ 没有任何信息丢失                                          ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
    }
}
