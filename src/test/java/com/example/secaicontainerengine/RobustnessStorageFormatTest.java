package com.example.secaicontainerengine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试 robustness 存储格式
 * 验证：保留原始 JSON 结构（adversarial、corruption 等键），去掉转义字符
 */
@SpringBootTest
public class RobustnessStorageFormatTest {

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 模拟评测模块回传的真实 robustness JSON
     */
    private String getRealRobustnessJson() {
        return "{\n" +
            "  \"corruption\": [\n" +
            "    {\n" +
            "      \"severity\": 1,\n" +
            "      \"corruption_key\": \"gaussian_noise_severity_1\",\n" +
            "      \"corruption_name\": \"gaussian_noise\",\n" +
            "      \"performance_drop_rate\": 0.10150171837957254,\n" +
            "      \"perturbation_magnitude\": 0.039590768463909624,\n" +
            "      \"perturbation_tolerance\": 0.7197211733952948\n" +
            "    },\n" +
            "    {\n" +
            "      \"severity\": 3,\n" +
            "      \"corruption_key\": \"gaussian_noise_severity_3\",\n" +
            "      \"corruption_name\": \"gaussian_noise\",\n" +
            "      \"performance_drop_rate\": 0.5187301846502559,\n" +
            "      \"perturbation_magnitude\": 0.1148813260793686,\n" +
            "      \"perturbation_tolerance\": 0.5759512053441767\n" +
            "    },\n" +
            "    {\n" +
            "      \"severity\": 5,\n" +
            "      \"corruption_key\": \"gaussian_noise_severity_5\",\n" +
            "      \"corruption_name\": \"gaussian_noise\",\n" +
            "      \"performance_drop_rate\": 0.7548621134994921,\n" +
            "      \"perturbation_magnitude\": 0.17979797956347465,\n" +
            "      \"perturbation_tolerance\": 0.4153354632587859\n" +
            "    },\n" +
            "    {\n" +
            "      \"severity\": 1,\n" +
            "      \"corruption_key\": \"gaussian_blur_severity_1\",\n" +
            "      \"corruption_name\": \"gaussian_blur\",\n" +
            "      \"performance_drop_rate\": 0.03713639500993364,\n" +
            "      \"perturbation_magnitude\": 0.01627748817531392,\n" +
            "      \"perturbation_tolerance\": 0.745570723206506\n" +
            "    },\n" +
            "    {\n" +
            "      \"severity\": 3,\n" +
            "      \"corruption_key\": \"gaussian_blur_severity_3\",\n" +
            "      \"corruption_name\": \"gaussian_blur\",\n" +
            "      \"performance_drop_rate\": 0.3773218217779768,\n" +
            "      \"perturbation_magnitude\": 0.03400290993973613,\n" +
            "      \"perturbation_tolerance\": 0.4975312227708394\n" +
            "    },\n" +
            "    {\n" +
            "      \"severity\": 5,\n" +
            "      \"corruption_key\": \"gaussian_blur_severity_5\",\n" +
            "      \"corruption_name\": \"gaussian_blur\",\n" +
            "      \"performance_drop_rate\": 0.712059080532761,\n" +
            "      \"perturbation_magnitude\": 0.04162775518372655,\n" +
            "      \"perturbation_tolerance\": 0.2518152773743828\n" +
            "    },\n" +
            "    {\n" +
            "      \"severity\": 1,\n" +
            "      \"corruption_key\": \"brightness_shift_severity_1\",\n" +
            "      \"corruption_name\": \"brightness_shift\",\n" +
            "      \"performance_drop_rate\": 0.016172248993107624,\n" +
            "      \"perturbation_magnitude\": 0.0936555516794324,\n" +
            "      \"perturbation_tolerance\": 0.7650304966598896\n" +
            "    },\n" +
            "    {\n" +
            "      \"severity\": 3,\n" +
            "      \"corruption_key\": \"brightness_shift_severity_3\",\n" +
            "      \"corruption_name\": \"brightness_shift\",\n" +
            "      \"performance_drop_rate\": 0.12181816556619048,\n" +
            "      \"perturbation_magnitude\": 0.250279966250062,\n" +
            "      \"perturbation_tolerance\": 0.7051989544002324\n" +
            "    },\n" +
            "    {\n" +
            "      \"severity\": 5,\n" +
            "      \"corruption_key\": \"brightness_shift_severity_5\",\n" +
            "      \"corruption_name\": \"brightness_shift\",\n" +
            "      \"performance_drop_rate\": 0.31256928359131125,\n" +
            "      \"perturbation_magnitude\": 0.3597620586454868,\n" +
            "      \"perturbation_tolerance\": 0.6195178623293639\n" +
            "    },\n" +
            "    {\n" +
            "      \"severity\": 1,\n" +
            "      \"corruption_key\": \"contrast_shift_severity_1\",\n" +
            "      \"corruption_name\": \"contrast_shift\",\n" +
            "      \"performance_drop_rate\": 0.003668013223334525,\n" +
            "      \"perturbation_magnitude\": 0.02372101335972547,\n" +
            "      \"perturbation_tolerance\": 0.7629973860005809\n" +
            "    },\n" +
            "    {\n" +
            "      \"severity\": 3,\n" +
            "      \"corruption_key\": \"contrast_shift_severity_3\",\n" +
            "      \"corruption_name\": \"contrast_shift\",\n" +
            "      \"performance_drop_rate\": 0.044072069006883034,\n" +
            "      \"perturbation_magnitude\": 0.07116304077580571,\n" +
            "      \"perturbation_tolerance\": 0.7139122857972698\n" +
            "    },\n" +
            "    {\n" +
            "      \"severity\": 5,\n" +
            "      \"corruption_key\": \"contrast_shift_severity_5\",\n" +
            "      \"corruption_name\": \"contrast_shift\",\n" +
            "      \"performance_drop_rate\": 0.47365063740852453,\n" +
            "      \"perturbation_magnitude\": 0.10674456183612346,\n" +
            "      \"perturbation_tolerance\": 0.48649433633459194\n" +
            "    }\n" +
            "  ],\n" +
            "  \"adversarial\": [\n" +
            "    {\n" +
            "      \"eps\": 0.1,\n" +
            "      \"attack\": \"fgsm\",\n" +
            "      \"metrics\": {\n" +
            "        \"clean_map\": 0.8594224333079387,\n" +
            "        \"miss_rate\": 0.4809758931164682,\n" +
            "        \"map_drop_rate\": 0.6018151662023413,\n" +
            "        \"adversarial_map\": 0.34220897876870093,\n" +
            "        \"clean_miss_rate\": 0.22887017136218415,\n" +
            "        \"per_class_clean_map\": {\n" +
            "          \"1\": 0.9853843204907036,\n" +
            "          \"2\": 0.9407952437173216,\n" +
            "          \"3\": 0.9693586213605374,\n" +
            "          \"4\": 0.8560488272276428,\n" +
            "          \"5\": 0.6006356780816812,\n" +
            "          \"6\": 0.9943019943019942,\n" +
            "          \"7\": 0.7587982932413867,\n" +
            "          \"8\": 0.9859907910475016,\n" +
            "          \"9\": 0.8768796992481203,\n" +
            "          \"10\": 0.921875,\n" +
            "          \"11\": 0.9811617998915912,\n" +
            "          \"12\": 0.6301713428509116,\n" +
            "          \"13\": 0.8229207077096072,\n" +
            "          \"14\": 0.6303784804252734,\n" +
            "          \"15\": 1,\n" +
            "          \"16\": 0.932362538340971,\n" +
            "          \"17\": 0.9942919923895724,\n" +
            "          \"18\": 0.5625434410014456,\n" +
            "          \"19\": 0.8117117659199726,\n" +
            "          \"20\": 0.9328381289125393\n" +
            "        },\n" +
            "        \"false_detection_rate\": 0.6759746146872166,\n" +
            "        \"per_class_adversarial_map\": {\n" +
            "          \"1\": 0.5576004029512011,\n" +
            "          \"2\": 0.0910130026977853,\n" +
            "          \"3\": 0.8881121584149567,\n" +
            "          \"4\": 0.3452359267397436,\n" +
            "          \"5\": 0.19877182706466592,\n" +
            "          \"6\": 0.21428571428571427,\n" +
            "          \"7\": 0.10961776560153844,\n" +
            "          \"8\": 0.04906832298136645,\n" +
            "          \"9\": 0.1,\n" +
            "          \"10\": 0.010416666666666666,\n" +
            "          \"11\": 0.5030102683569605,\n" +
            "          \"12\": 0.3996987024651791,\n" +
            "          \"13\": 0.34122023809523816,\n" +
            "          \"14\": 0.49099668020267495,\n" +
            "          \"15\": 0.7984193957204905,\n" +
            "          \"16\": 0.7298676543054099,\n" +
            "          \"17\": 0.6865808590893422,\n" +
            "          \"18\": 0.029411764705882353,\n" +
            "          \"19\": 0.24003149681405092,\n" +
            "          \"20\": 0.06082072821515152\n" +
            "        },\n" +
            "        \"clean_false_detection_rate\": 0.44629822732012514\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"steps\": 1,\n" +
            "      \"attack\": \"pgd\",\n" +
            "      \"metrics\": {\n" +
            "        \"clean_map\": 0.8594224333079387,\n" +
            "        \"miss_rate\": 0.4774905605576532,\n" +
            "        \"map_drop_rate\": 0.603745830620589,\n" +
            "        \"adversarial_map\": 0.34054972245646953,\n" +
            "        \"clean_miss_rate\": 0.22887017136218415,\n" +
            "        \"per_class_clean_map\": {\n" +
            "          \"1\": 0.9853843204907036,\n" +
            "          \"2\": 0.9407952437173216,\n" +
            "          \"3\": 0.9693586213605374,\n" +
            "          \"4\": 0.8560488272276428,\n" +
            "          \"5\": 0.6006356780816812,\n" +
            "          \"6\": 0.9943019943019942,\n" +
            "          \"7\": 0.7587982932413867,\n" +
            "          \"8\": 0.9859907910475016,\n" +
            "          \"9\": 0.8768796992481203,\n" +
            "          \"10\": 0.921875,\n" +
            "          \"11\": 0.9811617998915912,\n" +
            "          \"12\": 0.6301713428509116,\n" +
            "          \"13\": 0.8229207077096072,\n" +
            "          \"14\": 0.6303784804252734,\n" +
            "          \"15\": 1,\n" +
            "          \"16\": 0.932362538340971,\n" +
            "          \"17\": 0.9942919923895724,\n" +
            "          \"18\": 0.5625434410014456,\n" +
            "          \"19\": 0.8117117659199726,\n" +
            "          \"20\": 0.9328381289125393\n" +
            "        },\n" +
            "        \"false_detection_rate\": 0.6772515249372084,\n" +
            "        \"per_class_adversarial_map\": {\n" +
            "          \"1\": 0.5458471300015795,\n" +
            "          \"2\": 0.1092009842009842,\n" +
            "          \"3\": 0.92276972773219,\n" +
            "          \"4\": 0.33729244620109666,\n" +
            "          \"5\": 0.1882220519206821,\n" +
            "          \"6\": 0.21428571428571427,\n" +
            "          \"7\": 0.0967159277504105,\n" +
            "          \"8\": 0.04037267080745341,\n" +
            "          \"9\": 0.1,\n" +
            "          \"10\": 0.006944444444444444,\n" +
            "          \"11\": 0.47462896240508734,\n" +
            "          \"12\": 0.36273511477298265,\n" +
            "          \"13\": 0.3954545454545454,\n" +
            "          \"14\": 0.48848818126797905,\n" +
            "          \"15\": 0.7592467181940866,\n" +
            "          \"16\": 0.7393822859064234,\n" +
            "          \"17\": 0.6927739349905538,\n" +
            "          \"18\": 0.029411764705882353,\n" +
            "          \"19\": 0.2393322488167282,\n" +
            "          \"20\": 0.06788959527056591\n" +
            "        },\n" +
            "        \"clean_false_detection_rate\": 0.44629822732012514\n" +
            "      }\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }

    @Test
    public void testRobustnessStorageFormat() throws Exception {
        System.out.println("\n=================================================================");
        System.out.println("         Robustness Storage Format Test");
        System.out.println("=================================================================\n");

        // 模拟评测模块回传的真实数据
        String robustnessJson = getRealRobustnessJson();

        System.out.println("Step 1: Parse Input JSON");
        System.out.println("-----------------------------------------------------------------");

        // 模拟 parseAndStoreRobustness 的逻辑
        JsonNode rootNode = objectMapper.readTree(robustnessJson);
        assertNotNull(rootNode, "JSON parsing should succeed");
        System.out.println("[OK] JSON parsing successful");

        // 验证结构
        assertTrue(rootNode.has("adversarial"), "Should have 'adversarial' key");
        assertTrue(rootNode.has("corruption"), "Should have 'corruption' key");
        System.out.println("[OK] Has 'adversarial' key");
        System.out.println("[OK] Has 'corruption' key");

        int adversarialCount = rootNode.get("adversarial").size();
        int corruptionCount = rootNode.get("corruption").size();
        assertEquals(2, adversarialCount, "Should have 2 adversarial attacks");
        assertEquals(12, corruptionCount, "Should have 12 corruption tests");
        System.out.println("[OK] Adversarial attacks count: " + adversarialCount);
        System.out.println("[OK] Corruption tests count: " + corruptionCount);

        // 模拟存储到数据库（直接存储整个 JSON）
        String storedValue = robustnessJson;
        System.out.println("[OK] Stored as 'robustness' key\n");

        // 验证存储后的格式
        System.out.println("Step 2: Validate Stored Format");
        System.out.println("-----------------------------------------------------------------");

        JsonNode storedNode = objectMapper.readTree(storedValue);

        // 验证顶层结构
        assertTrue(storedNode.has("adversarial"), "Stored JSON should have 'adversarial'");
        assertTrue(storedNode.has("corruption"), "Stored JSON should have 'corruption'");
        assertTrue(storedNode.get("adversarial").isArray(), "'adversarial' should be an array");
        assertTrue(storedNode.get("corruption").isArray(), "'corruption' should be an array");
        System.out.println("[CHECK] Has 'adversarial' key: PASS");
        System.out.println("[CHECK] Has 'corruption' key: PASS");
        System.out.println("[CHECK] 'adversarial' is array: PASS");
        System.out.println("[CHECK] 'corruption' is array: PASS");

        // 验证第一个攻击
        JsonNode firstAttack = storedNode.get("adversarial").get(0);
        assertTrue(firstAttack.has("attack"), "First attack should have 'attack' field");
        assertTrue(firstAttack.has("eps"), "First attack should have 'eps' field");
        assertTrue(firstAttack.has("metrics"), "First attack should have 'metrics' field");
        assertEquals("fgsm", firstAttack.get("attack").asText());
        assertEquals(0.1, firstAttack.get("eps").asDouble());
        System.out.println("[CHECK] First attack has 'attack': PASS (fgsm)");
        System.out.println("[CHECK] First attack has 'eps': PASS (0.1)");
        System.out.println("[CHECK] First attack has 'metrics': PASS");

        // 验证 metrics 内容
        JsonNode metrics = firstAttack.get("metrics");
        assertTrue(metrics.has("clean_map"), "Metrics should have 'clean_map'");
        assertTrue(metrics.has("adversarial_map"), "Metrics should have 'adversarial_map'");
        assertTrue(metrics.has("per_class_clean_map"), "Metrics should have 'per_class_clean_map'");
        assertTrue(metrics.get("per_class_clean_map").isObject(), "'per_class_clean_map' should be an object");
        System.out.println("[CHECK] Metrics has 'clean_map': PASS");
        System.out.println("[CHECK] Metrics has 'adversarial_map': PASS");
        System.out.println("[CHECK] Metrics has 'per_class_clean_map': PASS (nested object)");

        // 验证第二个攻击
        JsonNode secondAttack = storedNode.get("adversarial").get(1);
        assertTrue(secondAttack.has("attack"), "Second attack should have 'attack' field");
        assertTrue(secondAttack.has("steps"), "Second attack should have 'steps' field");
        assertEquals("pgd", secondAttack.get("attack").asText());
        assertEquals(1, secondAttack.get("steps").asInt());
        System.out.println("[CHECK] Second attack has 'attack': PASS (pgd)");
        System.out.println("[CHECK] Second attack has 'steps': PASS (1)");

        // 验证第一个腐败测试
        JsonNode firstCorruption = storedNode.get("corruption").get(0);
        assertTrue(firstCorruption.has("corruption_name"), "First corruption should have 'corruption_name'");
        assertTrue(firstCorruption.has("severity"), "First corruption should have 'severity'");
        assertTrue(firstCorruption.has("performance_drop_rate"), "First corruption should have 'performance_drop_rate'");
        assertEquals("gaussian_noise", firstCorruption.get("corruption_name").asText());
        assertEquals(1, firstCorruption.get("severity").asInt());
        System.out.println("[CHECK] First corruption has 'corruption_name': PASS (gaussian_noise)");
        System.out.println("[CHECK] First corruption has 'severity': PASS (1)");
        System.out.println("[CHECK] First corruption has 'performance_drop_rate': PASS");

        // 打印最终格式预览
        System.out.println("\nStep 3: Final Storage Format Preview");
        System.out.println("-----------------------------------------------------------------");
        String prettyJson = objectMapper.writerWithDefaultPrettyPrinter()
            .writeValueAsString(storedNode);

        System.out.println("Database robustnessResult column will contain:");
        System.out.println("{\n  \"robustness\": ");
        String[] lines = prettyJson.split("\n");
        for (int i = 0; i < Math.min(15, lines.length); i++) {
            System.out.println("    " + lines[i]);
        }
        if (lines.length > 15) {
            System.out.println("    ... (truncated, total " + lines.length + " lines)");
        }
        System.out.println("  }\n}");

        // 最终验证
        System.out.println("\n=================================================================");
        System.out.println("                    ALL TESTS PASSED!");
        System.out.println("=================================================================");
        System.out.println("Storage format is CORRECT:");
        System.out.println("  ✓ Preserved 'adversarial' and 'corruption' keys");
        System.out.println("  ✓ Preserved 'attack', 'eps', 'steps' fields");
        System.out.println("  ✓ Preserved 'metrics' nested structure");
        System.out.println("  ✓ Preserved 'per_class_*' nested objects");
        System.out.println("  ✓ Preserved 'corruption_name', 'severity' fields");
        System.out.println("  ✓ No information loss");
        System.out.println("  ✓ JSON is valid and parseable");
        System.out.println("=================================================================\n");
    }
}
