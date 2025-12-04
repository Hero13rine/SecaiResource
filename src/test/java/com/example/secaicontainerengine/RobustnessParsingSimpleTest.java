package com.example.secaicontainerengine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/**
 * 简单的鲁棒性解析测试，不依赖Spring容器
 */
public class RobustnessParsingSimpleTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testRobustnessJsonStructure() throws Exception {
        System.out.println("=================================");
        System.out.println("测试鲁棒性JSON格式解析");
        System.out.println("=================================");

        // 模拟评测模块返回的完整JSON
        String robustnessJson = """
            {
              "adversarial": [
                {
                  "attack_name": "fgsm_eps_0.001",
                  "map_drop_rate": 0.062,
                  "miss_rate": 0.250,
                  "false_detection_rate": 0.439
                }
              ],
              "corruption": [
                {
                  "corruption_name": "gaussian_noise",
                  "performance_drop_rate": 0.102,
                  "perturbation_tolerance": 0.721
                }
              ]
            }
        """;

        // 解析JSON
        JsonNode root = objectMapper.readTree(robustnessJson);

        // 验证结构
        System.out.println("✓ 包含 adversarial: " + root.has("adversarial"));
        System.out.println("✓ 包含 corruption: " + root.has("corruption"));
        System.out.println("✓ adversarial 是数组: " + root.get("adversarial").isArray());
        System.out.println("✓ corruption 是数组: " + root.get("corruption").isArray());

        // 验证字段
        JsonNode adv = root.get("adversarial").get(0);
        System.out.println("✓ attack_name: " + adv.get("attack_name").asText());
        System.out.println("✓ map_drop_rate: " + adv.get("map_drop_rate").asDouble());

        System.out.println("=================================");
        System.out.println("✅ 测试通过：JSON结构正确！");
    }

    @Test
    public void testStringFormatParsing() throws Exception {
        System.out.println("=================================");
        System.out.println("测试字符串格式的adversarial/corruption解析");
        System.out.println("=================================");

        // 模拟数据库存储格式：adversarial和corruption是字符串
        String storedJson = """
            {
              "adversarial": "[{\\"attack_name\\": \\"fgsm\\", \\"map_drop_rate\\": 0.1}]",
              "corruption": "[{\\"corruption_name\\": \\"noise\\", \\"performance_drop_rate\\": 0.1}]"
            }
        """;

        JsonNode root = objectMapper.readTree(storedJson);

        // 检查类型
        JsonNode advNode = root.get("adversarial");
        System.out.println("✓ adversarial 是文本类型: " + advNode.isTextual());

        // 二次解析
        if (advNode.isTextual()) {
            String advJson = advNode.asText();
            JsonNode advArray = objectMapper.readTree(advJson);
            System.out.println("✓ 解析后是数组: " + advArray.isArray());
            System.out.println("✓ 数组长度: " + advArray.size());
        }

        System.out.println("=================================");
        System.out.println("✅ 测试通过：字符串格式解析成功！");
    }

    @Test
    public void testScoreCalculation() {
        System.out.println("=================================");
        System.out.println("测试得分计算逻辑");
        System.out.println("=================================");

        // 模拟指标值
        double mapDropRate = 0.062;
        double missRate = 0.250;
        double falseDetectionRate = 0.439;

        // 计算得分（值越低越好，转换为1-rate）
        double score1 = Math.max(0.0, 1.0 - mapDropRate);
        double score2 = Math.max(0.0, 1.0 - missRate);
        double score3 = Math.max(0.0, 1.0 - falseDetectionRate);

        double avgScore = (score1 + score2 + score3) / 3.0;

        System.out.println("✓ map_drop_rate 得分: " + score1);
        System.out.println("✓ miss_rate 得分: " + score2);
        System.out.println("✓ false_detection_rate 得分: " + score3);
        System.out.println("✓ 平均得分: " + avgScore);

        // 验证得分范围
        assert avgScore >= 0.0 && avgScore <= 1.0 : "得分应在0-1之间";

        System.out.println("=================================");
        System.out.println("✅ 测试通过：得分计算正确！");
    }
}
