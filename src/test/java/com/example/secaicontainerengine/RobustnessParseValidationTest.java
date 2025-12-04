package com.example.secaicontainerengine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.*;

/**
 * 验证鲁棒性数据解析逻辑
 * 使用真实数据测试，不依赖数据库
 */
public class RobustnessParseValidationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testRealDataParsing() throws Exception {
        System.out.println("==========================================================");
        System.out.println("测试真实鲁棒性数据解析");
        System.out.println("==========================================================\n");

        // 真实的 robustness JSON 数据（用户提供的）
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
                  "attack_name": "fgsm_eps_0.002",
                  "map_drop_rate": 0.10537477311678743,
                  "miss_rate": 0.26982282892826026,
                  "false_detection_rate": 0.5499462943071965
                },
                {
                  "attack_name": "pgd_steps_1",
                  "map_drop_rate": 0.6015525431754909,
                  "miss_rate": 0.4766192274179495,
                  "false_detection_rate": 0.6757826556315222
                },
                {
                  "attack_name": "pgd_steps_2",
                  "map_drop_rate": 0.972964678836281,
                  "miss_rate": 0.7987220447284346,
                  "false_detection_rate": 0.9483875772696805
                }
              ],
              "corruption": [
                {
                  "corruption_key": "gaussian_noise_severity_1",
                  "corruption_name": "gaussian_noise",
                  "severity": 1,
                  "perturbation_magnitude": 0.03959418974071741,
                  "performance_drop_rate": 0.10181578335851295,
                  "perturbation_tolerance": 0.7211733952948011
                },
                {
                  "corruption_key": "gaussian_noise_severity_2",
                  "corruption_name": "gaussian_noise",
                  "severity": 2,
                  "perturbation_magnitude": 0.07822283889353275,
                  "performance_drop_rate": 0.3574626101893228,
                  "perturbation_tolerance": 0.6491431890792914
                },
                {
                  "corruption_key": "gaussian_blur_severity_1",
                  "corruption_name": "gaussian_blur",
                  "severity": 1,
                  "perturbation_magnitude": 0.01627748817531392,
                  "performance_drop_rate": 0.03834184270923333,
                  "perturbation_tolerance": 0.746732500726111
                }
              ]
            }
        """;

        System.out.println("【步骤1】解析 JSON");
        JsonNode rootNode = objectMapper.readTree(robustnessJson);

        // 模拟数据库存储
        Map<String, String> flatData = new LinkedHashMap<>();
        int totalFields = 0;

        // ==================== 处理 adversarial ====================
        System.out.println("\n【步骤2】处理 adversarial 数组");
        if (rootNode.has("adversarial") && rootNode.get("adversarial").isArray()) {
            JsonNode adversarialArray = rootNode.get("adversarial");

            // 白名单：只存储这3个指标
            Set<String> adversarialMetrics = new HashSet<>(Arrays.asList(
                "map_drop_rate", "miss_rate", "false_detection_rate"
            ));

            for (JsonNode attack : adversarialArray) {
                // 获取攻击名称
                if (!attack.has("attack_name")) {
                    System.out.println("  ⚠ 警告：adversarial 攻击缺少 attack_name 字段，跳过");
                    continue;
                }
                String attackName = attack.get("attack_name").asText();
                System.out.println("\n  处理攻击: " + attackName);

                // 只展开白名单中的指标
                Iterator<Map.Entry<String, JsonNode>> fields = attack.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    String fieldName = field.getKey();

                    // 只存储预定义的指标字段
                    if (!adversarialMetrics.contains(fieldName)) {
                        System.out.println("    跳过配置参数: " + fieldName);
                        continue;
                    }

                    // 构造键名：指标名_攻击名
                    String key = fieldName + "_" + attackName;
                    String value = field.getValue().asText();

                    flatData.put(key, value);
                    totalFields++;
                    System.out.println("    ✓ " + key + ": " + value);
                }
            }
        }

        // ==================== 处理 corruption ====================
        System.out.println("\n【步骤3】处理 corruption 数组");
        if (rootNode.has("corruption") && rootNode.get("corruption").isArray()) {
            JsonNode corruptionArray = rootNode.get("corruption");

            // 白名单：只存储这3个指标
            Set<String> corruptionMetrics = new HashSet<>(Arrays.asList(
                "perturbation_magnitude", "performance_drop_rate", "perturbation_tolerance"
            ));

            for (JsonNode corruption : corruptionArray) {
                // 获取腐败类型和严重程度
                if (!corruption.has("corruption_name")) {
                    System.out.println("  ⚠ 警告：corruption 测试缺少 corruption_name 字段，跳过");
                    continue;
                }
                String corruptionName = corruption.get("corruption_name").asText();
                String severity = corruption.has("severity") ?
                    corruption.get("severity").asText() : "";
                String suffix = corruptionName + (severity.isEmpty() ? "" : "_" + severity);

                System.out.println("\n  处理腐败测试: " + suffix);

                // 只展开白名单中的指标
                Iterator<Map.Entry<String, JsonNode>> fields = corruption.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    String fieldName = field.getKey();

                    // 只存储预定义的指标字段
                    if (!corruptionMetrics.contains(fieldName)) {
                        System.out.println("    跳过配置参数: " + fieldName);
                        continue;
                    }

                    // 构造键名：指标名_腐败类型_严重程度
                    String key = fieldName + "_" + suffix;
                    String value = field.getValue().asText();

                    flatData.put(key, value);
                    totalFields++;
                    System.out.println("    ✓ " + key + ": " + value);
                }
            }
        }

        // ==================== 结果验证 ====================
        System.out.println("\n==========================================================");
        System.out.println("【解析结果】");
        System.out.println("==========================================================");
        System.out.println("总共解析了 " + totalFields + " 个指标字段\n");

        System.out.println("最终存储格式（模拟数据库）:");
        System.out.println("{");
        int count = 0;
        for (Map.Entry<String, String> entry : flatData.entrySet()) {
            count++;
            String comma = count < flatData.size() ? "," : "";
            System.out.println("  \"" + entry.getKey() + "\": \"" + entry.getValue() + "\"" + comma);
        }
        System.out.println("}");

        // ==================== 断言验证 ====================
        System.out.println("\n【验证检查】");

        // 验证 adversarial 指标
        assert flatData.containsKey("map_drop_rate_fgsm_eps_0.001") : "应该有 map_drop_rate_fgsm_eps_0.001";
        assert flatData.containsKey("miss_rate_fgsm_eps_0.001") : "应该有 miss_rate_fgsm_eps_0.001";
        assert flatData.containsKey("false_detection_rate_fgsm_eps_0.001") : "应该有 false_detection_rate_fgsm_eps_0.001";
        System.out.println("✓ fgsm_eps_0.001 的3个指标都存在");

        assert flatData.containsKey("map_drop_rate_pgd_steps_2") : "应该有 map_drop_rate_pgd_steps_2";
        System.out.println("✓ pgd_steps_2 的指标存在");

        // 验证 corruption 指标
        assert flatData.containsKey("perturbation_magnitude_gaussian_noise_1") : "应该有 perturbation_magnitude_gaussian_noise_1";
        assert flatData.containsKey("performance_drop_rate_gaussian_noise_1") : "应该有 performance_drop_rate_gaussian_noise_1";
        assert flatData.containsKey("perturbation_tolerance_gaussian_noise_1") : "应该有 perturbation_tolerance_gaussian_noise_1";
        System.out.println("✓ gaussian_noise_1 的3个指标都存在");

        assert flatData.containsKey("performance_drop_rate_gaussian_blur_1") : "应该有 performance_drop_rate_gaussian_blur_1";
        System.out.println("✓ gaussian_blur_1 的指标存在");

        // 验证不应该存在的字段
        assert !flatData.containsKey("corruption_key_gaussian_noise_1") : "不应该有 corruption_key";
        assert !flatData.containsKey("attack_name_fgsm_eps_0.001") : "不应该有 attack_name";
        assert !flatData.containsKey("severity_gaussian_noise_1") : "不应该有 severity";
        System.out.println("✓ 配置参数都被正确过滤");

        // 验证总数
        int expectedAdversarial = 4 * 3; // 4个攻击 × 3个指标
        int expectedCorruption = 3 * 3;  // 3个corruption × 3个指标
        int expectedTotal = expectedAdversarial + expectedCorruption;
        assert totalFields == expectedTotal : "总字段数应为 " + expectedTotal + "，实际为 " + totalFields;
        System.out.println("✓ 总字段数正确: " + totalFields);

        System.out.println("\n==========================================================");
        System.out.println("✅ 所有验证通过！解析逻辑正确！");
        System.out.println("==========================================================");
    }
}
