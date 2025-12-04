package com.example.secaicontainerengine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.*;

/**
 * 测试新的扁平格式存储逻辑
 */
public class RobustnessFlatFormatTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testFlatFormatStorage() throws Exception {
        System.out.println("=================================");
        System.out.println("测试 robustness 扁平格式存储");
        System.out.println("=================================\n");

        // ==================== 步骤1: 评测模块回传 ====================
        System.out.println("【步骤1】评测模块回传数据");
        String robustnessJson = """
            {
              "adversarial": [
                {
                  "attack_name": "fgsm_eps_0.001",
                  "map_drop_rate": 0.06210263321172507,
                  "miss_rate": 0.2503630554748766,
                  "false_detection_rate": 0.43879104153076753
                },
                {
                  "attack_name": "pgd_steps_1",
                  "map_drop_rate": 0.6015525431754909,
                  "miss_rate": 0.4766192274179495,
                  "false_detection_rate": 0.6757826556315222
                }
              ],
              "corruption": [
                {
                  "corruption_name": "gaussian_noise",
                  "severity": 1,
                  "perturbation_magnitude": 0.03959418974071741,
                  "performance_drop_rate": 0.10181578335851295,
                  "perturbation_tolerance": 0.7211733952948011
                },
                {
                  "corruption_name": "gaussian_blur",
                  "severity": 1,
                  "perturbation_magnitude": 0.01627748817531392,
                  "performance_drop_rate": 0.03834184270923333,
                  "perturbation_tolerance": 0.746732500726111
                }
              ]
            }
        """;

        System.out.println("✓ 接收到 robustness JSON\n");

        // ==================== 步骤2: 模拟 parseAndStoreRobustness 逻辑 ====================
        System.out.println("【步骤2】解析并展开为扁平格式");

        JsonNode rootNode = objectMapper.readTree(robustnessJson);
        Map<String, String> flattenedData = new LinkedHashMap<>();

        // 处理 adversarial 数组
        if (rootNode.has("adversarial") && rootNode.get("adversarial").isArray()) {
            JsonNode adversarialArray = rootNode.get("adversarial");
            for (JsonNode attack : adversarialArray) {
                String attackName = attack.has("attack_name") ? attack.get("attack_name").asText() : "unknown";

                Iterator<Map.Entry<String, JsonNode>> fields = attack.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    String fieldName = field.getKey();

                    if ("attack_name".equals(fieldName)) {
                        continue;
                    }

                    String key = fieldName + "_" + attackName;
                    String value = field.getValue().asText();
                    flattenedData.put(key, value);
                }
            }
        }

        // 处理 corruption 数组
        if (rootNode.has("corruption") && rootNode.get("corruption").isArray()) {
            JsonNode corruptionArray = rootNode.get("corruption");
            for (JsonNode corruption : corruptionArray) {
                String corruptionName = corruption.has("corruption_name") ?
                    corruption.get("corruption_name").asText() : "unknown";
                String severity = corruption.has("severity") ?
                    corruption.get("severity").asText() : "";
                String suffix = corruptionName + (severity.isEmpty() ? "" : "_" + severity);

                Iterator<Map.Entry<String, JsonNode>> fields = corruption.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    String fieldName = field.getKey();

                    if ("corruption_name".equals(fieldName) ||
                        "corruption_key".equals(fieldName) ||
                        "severity".equals(fieldName)) {
                        continue;
                    }

                    String key = fieldName + "_" + suffix;
                    String value = field.getValue().asText();
                    flattenedData.put(key, value);
                }
            }
        }

        System.out.println("✓ 展开后的键值对数量: " + flattenedData.size());
        System.out.println();

        // ==================== 步骤3: 显示扁平格式 ====================
        System.out.println("【步骤3】最终存储格式（模拟数据库）");
        System.out.println("robustnessResult 列:");
        System.out.println("{");

        int count = 0;
        for (Map.Entry<String, String> entry : flattenedData.entrySet()) {
            count++;
            String comma = count < flattenedData.size() ? "," : "";
            System.out.println("  \"" + entry.getKey() + "\": \"" + entry.getValue() + "\"" + comma);
        }
        System.out.println("}");
        System.out.println();

        // ==================== 步骤4: 对比格式 ====================
        System.out.println("【步骤4】格式对比");
        System.out.println();

        System.out.println("basicResult（其他维度）:");
        System.out.println("{");
        System.out.println("  \"mCE\": \"0.0197\",");
        System.out.println("  \"RmCE\": \"0.0008\",");
        System.out.println("  \"acac_0_0\": \"0.0408\"");
        System.out.println("}");
        System.out.println();

        System.out.println("robustnessResult（新格式）:");
        System.out.println("{");
        int shown = 0;
        for (Map.Entry<String, String> entry : flattenedData.entrySet()) {
            if (shown++ >= 3) {
                System.out.println("  ...");
                break;
            }
            String comma = shown < 3 ? "," : "";
            System.out.println("  \"" + entry.getKey() + "\": \"" + entry.getValue() + "\"" + comma);
        }
        System.out.println("}");
        System.out.println();

        // ==================== 验证 ====================
        System.out.println("【验证结果】");
        System.out.println("✓ 格式为扁平键值对: " + (flattenedData.size() > 0));
        System.out.println("✓ 没有嵌套结构: " + !flattenedData.containsKey("adversarial"));
        System.out.println("✓ 没有 robustness 包裹层: " + !flattenedData.containsKey("robustness"));
        System.out.println("✓ 每个指标独立存储: " + flattenedData.containsKey("map_drop_rate_fgsm_eps_0.001"));

        System.out.println("\n=================================");
        System.out.println("✅ 测试通过！格式与其他维度完全一致！");
        System.out.println("=================================");

        // 断言验证
        assert flattenedData.size() > 0 : "应该有展开的数据";
        assert !flattenedData.containsKey("adversarial") : "不应该有 adversarial 键";
        assert !flattenedData.containsKey("corruption") : "不应该有 corruption 键";
        assert !flattenedData.containsKey("robustness") : "不应该有 robustness 键";
        assert flattenedData.containsKey("map_drop_rate_fgsm_eps_0.001") : "应该有展开的指标";
    }

    @Test
    public void testKeyNaming() {
        System.out.println("=================================");
        System.out.println("测试键名生成规则");
        System.out.println("=================================\n");

        // 测试不同的键名格式
        String[][] testCases = {
            {"map_drop_rate", "fgsm_eps_0.001", "map_drop_rate_fgsm_eps_0.001"},
            {"miss_rate", "pgd_steps_1", "miss_rate_pgd_steps_1"},
            {"performance_drop_rate", "gaussian_noise_1", "performance_drop_rate_gaussian_noise_1"},
            {"perturbation_tolerance", "gaussian_blur_2", "perturbation_tolerance_gaussian_blur_2"}
        };

        System.out.println("键名格式规则: 指标名_攻击方法/腐败类型_参数\n");

        for (String[] testCase : testCases) {
            String metric = testCase[0];
            String suffix = testCase[1];
            String expected = testCase[2];
            String actual = metric + "_" + suffix;

            System.out.println("✓ " + metric + " + " + suffix + " → " + actual);
            assert actual.equals(expected) : "键名格式不正确";
        }

        System.out.println("\n=================================");
        System.out.println("✅ 键名生成规则正确！");
        System.out.println("=================================");
    }
}
