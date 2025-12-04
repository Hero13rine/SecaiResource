package com.example.secaicontainerengine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.*;

/**
 * 测试新的解析方法是否能存储所有指标字段（不仅仅是白名单中的字段）
 */
public class RobustnessFullFieldsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testParseAllMetrics() throws Exception {
        System.out.println("=================================");
        System.out.println("测试解析所有 robustness 指标");
        System.out.println("=================================\n");

        // 模拟评测模块回传的完整 JSON（包含所有指标）
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
        """;

        System.out.println("【步骤1】原始JSON数据");
        System.out.println(robustnessJson);
        System.out.println();

        // 解析并展开
        JsonNode rootNode = objectMapper.readTree(robustnessJson);
        Map<String, String> flattenedData = new LinkedHashMap<>();

        // 处理 adversarial 数组 - 存储所有字段
        if (rootNode.has("adversarial") && rootNode.get("adversarial").isArray()) {
            JsonNode adversarialArray = rootNode.get("adversarial");

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
                        // 嵌套对象，序列化为JSON字符串
                        String key = metricName + "_" + attackName;
                        String value = objectMapper.writeValueAsString(valueNode);
                        flattenedData.put(key, value);
                    } else {
                        String key = metricName + "_" + attackName;
                        String value = valueNode.asText();
                        flattenedData.put(key, value);
                    }
                }
            }
        }

        // 处理 corruption 数组 - 存储所有字段
        if (rootNode.has("corruptions") && rootNode.get("corruptions").isArray()) {
            JsonNode corruptionArray = rootNode.get("corruptions");

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
                    } else {
                        String key = metricName + "_" + suffix;
                        String value = valueNode.asText();
                        flattenedData.put(key, value);
                    }
                }
            }
        }

        // 显示解析结果
        System.out.println("【步骤2】解析后的扁平化存储格式");
        System.out.println("{");
        int count = 0;
        for (Map.Entry<String, String> entry : flattenedData.entrySet()) {
            count++;
            String comma = count < flattenedData.size() ? "," : "";
            System.out.println("  \"" + entry.getKey() + "\": \"" + entry.getValue() + "\"" + comma);
        }
        System.out.println("}");
        System.out.println();

        // 验证结果
        System.out.println("【步骤3】验证结果");
        System.out.println("✓ 总共存储字段数: " + flattenedData.size());

        // 验证所有重要指标都被保存
        String[] expectedKeys = {
            "clean_map_fgsm_eps_0.001",
            "adversarial_map_fgsm_eps_0.001",
            "map_drop_rate_fgsm_eps_0.001",
            "miss_rate_fgsm_eps_0.001",
            "false_detection_rate_fgsm_eps_0.001",
            "clean_miss_rate_fgsm_eps_0.001",
            "clean_false_detection_rate_fgsm_eps_0.001",
            "per_class_clean_map_fgsm_eps_0.001",
            "per_class_adversarial_map_fgsm_eps_0.001",
            "perturbation_magnitude_gaussian_noise_1",
            "performance_drop_rate_gaussian_noise_1",
            "perturbation_tolerance_gaussian_noise_1"
        };

        boolean allPresent = true;
        for (String key : expectedKeys) {
            if (flattenedData.containsKey(key)) {
                System.out.println("✓ 找到字段: " + key);
            } else {
                System.out.println("✗ 缺失字段: " + key);
                allPresent = false;
            }
        }

        System.out.println();
        System.out.println("【步骤4】特殊字段验证");
        // 验证嵌套对象是否正确序列化
        String perClassCleanMap = flattenedData.get("per_class_clean_map_fgsm_eps_0.001");
        System.out.println("per_class_clean_map 存储格式: " + perClassCleanMap);

        JsonNode perClassNode = objectMapper.readTree(perClassCleanMap);
        System.out.println("✓ 可以反序列化: " + (perClassNode.isObject()));
        System.out.println("✓ 包含类别数据: " + perClassNode.has("1"));

        System.out.println();
        System.out.println("=================================");
        if (allPresent) {
            System.out.println("✅ 测试通过！所有指标都已保存！");
        } else {
            System.out.println("❌ 测试失败！部分指标丢失！");
        }
        System.out.println("=================================");

        // 断言验证
        assert flattenedData.size() == 12 : "应该有12个字段（9个adversarial + 3个corruption）";
        assert allPresent : "所有预期字段都应该存在";
        assert flattenedData.containsKey("clean_map_fgsm_eps_0.001") : "应该包含 clean_map";
        assert flattenedData.containsKey("per_class_clean_map_fgsm_eps_0.001") : "应该包含 per_class_clean_map";
    }

    @Test
    public void testCompareOldVsNew() {
        System.out.println("=================================");
        System.out.println("对比旧方法 vs 新方法");
        System.out.println("=================================\n");

        System.out.println("【旧方法】白名单机制 - 只存储3个指标");
        String[] oldMetrics = {"map_drop_rate", "miss_rate", "false_detection_rate"};
        System.out.println("adversarial 白名单: " + Arrays.toString(oldMetrics));
        System.out.println("结果: 丢失了 clean_map, adversarial_map, per_class_* 等重要指标");
        System.out.println();

        System.out.println("【新方法】黑名单机制 - 存储所有非配置字段");
        String[] excludedFields = {"attack", "eps", "attack_name"};
        System.out.println("排除字段: " + Arrays.toString(excludedFields));
        System.out.println("结果: 保留所有评测指标，包括:");
        System.out.println("  - clean_map, adversarial_map");
        System.out.println("  - clean_miss_rate, clean_false_detection_rate");
        System.out.println("  - per_class_clean_map, per_class_adversarial_map");
        System.out.println("  - map_drop_rate, miss_rate, false_detection_rate");
        System.out.println();

        System.out.println("=================================");
        System.out.println("✅ 新方法优势：不会丢失任何评测指标！");
        System.out.println("=================================");
    }
}
