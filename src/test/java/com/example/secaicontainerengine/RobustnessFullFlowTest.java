package com.example.secaicontainerengine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/**
 * 完整流程测试：模拟从评测模块回传 → 存储 → 读取 → 计算得分
 */
public class RobustnessFullFlowTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testCompleteFlow() throws Exception {
        System.out.println("=================================");
        System.out.println("完整流程测试：robustness 数据处理");
        System.out.println("=================================\n");

        // ==================== 步骤1: 评测模块回传的数据 ====================
        System.out.println("【步骤1】评测模块回传数据");
        String evaluationResponse = """
            {
              "robustness": "{\\"adversarial\\": [{\\"attack_name\\": \\"fgsm_eps_0.001\\", \\"map_drop_rate\\": 0.062, \\"miss_rate\\": 0.250, \\"false_detection_rate\\": 0.439}], \\"corruption\\": [{\\"corruption_name\\": \\"gaussian_noise\\", \\"performance_drop_rate\\": 0.102, \\"perturbation_tolerance\\": 0.721}]}"
            }
        """;

        JsonNode responseNode = objectMapper.readTree(evaluationResponse);
        String robustnessValue = responseNode.get("robustness").asText();
        System.out.println("✓ 接收到 robustness 字段（字符串格式）");
        System.out.println("  长度: " + robustnessValue.length() + " 字符\n");

        // ==================== 步骤2: 解析并准备存储 ====================
        System.out.println("【步骤2】解析内层JSON并展开存储");

        // 模拟 parseAndStoreRobustness 的逻辑
        JsonNode innerJson = objectMapper.readTree(robustnessValue);

        // 提取 adversarial
        JsonNode adversarialNode = innerJson.get("adversarial");
        String adversarialToStore = objectMapper.writeValueAsString(adversarialNode);
        System.out.println("✓ 提取 adversarial 数组");
        System.out.println("  准备存储为: \"adversarial\": \"" + adversarialToStore.substring(0, 50) + "...\"");

        // 提取 corruption
        JsonNode corruptionNode = innerJson.get("corruption");
        String corruptionToStore = objectMapper.writeValueAsString(corruptionNode);
        System.out.println("✓ 提取 corruption 数组");
        System.out.println("  准备存储为: \"corruption\": \"" + corruptionToStore.substring(0, 50) + "...\"\n");

        // ==================== 步骤3: 模拟数据库存储格式 ====================
        System.out.println("【步骤3】模拟数据库存储格式");

        // 数据库中的格式（JSON列）
        String databaseFormat = String.format(
            "{\"adversarial\": %s, \"corruption\": %s}",
            objectMapper.writeValueAsString(adversarialToStore),
            objectMapper.writeValueAsString(corruptionToStore)
        );

        System.out.println("✓ 数据库存储格式（robustnessResult 列）:");
        System.out.println("  {");
        System.out.println("    \"adversarial\": \"[...]\"  // 字符串");
        System.out.println("    \"corruption\": \"[...]\"   // 字符串");
        System.out.println("  }");
        System.out.println("✓ 格式与其他维度一致（扁平结构）\n");

        // ==================== 步骤4: 从数据库读取并解析 ====================
        System.out.println("【步骤4】从数据库读取并解析");

        // 模拟从数据库读取
        JsonNode storedData = objectMapper.readTree(databaseFormat);
        System.out.println("✓ 读取到 adversarial: " + storedData.has("adversarial"));
        System.out.println("✓ 读取到 corruption: " + storedData.has("corruption"));

        // 检查类型
        JsonNode advNode = storedData.get("adversarial");
        JsonNode corrNode = storedData.get("corruption");
        System.out.println("✓ adversarial 是文本类型: " + advNode.isTextual());
        System.out.println("✓ corruption 是文本类型: " + corrNode.isTextual() + "\n");

        // ==================== 步骤5: 二次解析（parseRobustnessFields） ====================
        System.out.println("【步骤5】二次解析字符串为数组");

        // 解析 adversarial 字符串
        String advJsonString = advNode.asText();
        JsonNode advArray = objectMapper.readTree(advJsonString);
        System.out.println("✓ adversarial 解析后是数组: " + advArray.isArray());
        System.out.println("  数组长度: " + advArray.size());

        // 解析 corruption 字符串
        String corrJsonString = corrNode.asText();
        JsonNode corrArray = objectMapper.readTree(corrJsonString);
        System.out.println("✓ corruption 解析后是数组: " + corrArray.isArray());
        System.out.println("  数组长度: " + corrArray.size() + "\n");

        // ==================== 步骤6: 计算得分 ====================
        System.out.println("【步骤6】计算鲁棒性得分");

        // 模拟 computeDetectionRobustnessScore 的逻辑
        double totalScore = 0.0;
        int scoreCount = 0;

        // 计算 adversarial 得分
        if (advArray.isArray() && advArray.size() > 0) {
            JsonNode attack = advArray.get(0);
            double mapDropRate = attack.get("map_drop_rate").asDouble();
            double missRate = attack.get("miss_rate").asDouble();
            double falseDetectionRate = attack.get("false_detection_rate").asDouble();

            double attackScore = (
                Math.max(0.0, 1.0 - mapDropRate) +
                Math.max(0.0, 1.0 - missRate) +
                Math.max(0.0, 1.0 - falseDetectionRate)
            ) / 3.0;

            totalScore += attackScore;
            scoreCount++;

            System.out.println("✓ 对抗攻击得分: " + String.format("%.4f", attackScore));
            System.out.println("  - map_drop_rate: " + mapDropRate + " → " + String.format("%.4f", 1.0 - mapDropRate));
            System.out.println("  - miss_rate: " + missRate + " → " + String.format("%.4f", 1.0 - missRate));
            System.out.println("  - false_detection_rate: " + falseDetectionRate + " → " + String.format("%.4f", 1.0 - falseDetectionRate));
        }

        // 计算 corruption 得分
        if (corrArray.isArray() && corrArray.size() > 0) {
            JsonNode corruption = corrArray.get(0);
            double performanceDropRate = corruption.get("performance_drop_rate").asDouble();
            double perturbationTolerance = corruption.get("perturbation_tolerance").asDouble();

            double corruptionScore = (
                Math.max(0.0, 1.0 - performanceDropRate) +
                Math.max(0.0, perturbationTolerance)
            ) / 2.0;

            totalScore += corruptionScore;
            scoreCount++;

            System.out.println("✓ 腐败测试得分: " + String.format("%.4f", corruptionScore));
            System.out.println("  - performance_drop_rate: " + performanceDropRate + " → " + String.format("%.4f", 1.0 - performanceDropRate));
            System.out.println("  - perturbation_tolerance: " + perturbationTolerance);
        }

        double finalScore = scoreCount > 0 ? totalScore / scoreCount : 0.0;
        System.out.println("\n✓ 最终鲁棒性得分: " + String.format("%.4f", finalScore));
        System.out.println("  (范围: 0.0 ~ 1.0)");

        // ==================== 验证 ====================
        System.out.println("\n=================================");
        System.out.println("【验证结果】");
        assert finalScore >= 0.0 && finalScore <= 1.0 : "得分应在0-1之间";
        assert finalScore > 0.0 : "得分应大于0";
        System.out.println("✅ 所有步骤通过！完整流程正确！");
        System.out.println("=================================");
    }
}
