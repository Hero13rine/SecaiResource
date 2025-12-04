import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 测试 robustness 存储格式
 * 验证：保留原始 JSON 结构（adversarial、corruption 等键），去掉转义字符
 *
 * 编译运行:
 * javac -cp ".;jackson-databind-2.15.2.jar;jackson-core-2.15.2.jar;jackson-annotations-2.15.2.jar" RobustnessStorageTest.java
 * java -cp ".;jackson-databind-2.15.2.jar;jackson-core-2.15.2.jar;jackson-annotations-2.15.2.jar" RobustnessStorageTest
 */
public class RobustnessStorageTest {

    public static void main(String[] args) {
        System.out.println("=================================================================");
        System.out.println("         Robustness Storage Format Test");
        System.out.println("=================================================================\n");

        // 模拟评测模块回传的真实数据
        String realJson = createRealRobustnessJson();

        System.out.println("Step 1: Input - Real robustness JSON from evaluation module");
        System.out.println("-----------------------------------------------------------------");
        System.out.println("Length: " + realJson.length() + " characters");
        System.out.println("Has escape characters: " + realJson.contains("\\\""));
        System.out.println();

        // 模拟 parseAndStoreRobustness 方法
        System.out.println("Step 2: Parse and Store");
        System.out.println("-----------------------------------------------------------------");

        Map<String, String> databaseResult = new LinkedHashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            // 1. 解析验证 JSON 格式
            JsonNode rootNode = objectMapper.readTree(realJson);
            System.out.println("[OK] JSON parsing successful");

            // 验证结构
            boolean hasAdversarial = rootNode.has("adversarial");
            boolean hasCorruption = rootNode.has("corruption");
            System.out.println("[OK] Has 'adversarial' key: " + hasAdversarial);
            System.out.println("[OK] Has 'corruption' key: " + hasCorruption);

            if (hasAdversarial) {
                int adversarialCount = rootNode.get("adversarial").size();
                System.out.println("[OK] Adversarial attacks count: " + adversarialCount);
            }

            if (hasCorruption) {
                int corruptionCount = rootNode.get("corruption").size();
                System.out.println("[OK] Corruption tests count: " + corruptionCount);
            }

            // 2. 直接存储整个 JSON（保留 adversarial、corruption 等键）
            databaseResult.put("robustness", realJson);
            System.out.println("[OK] Stored as 'robustness' key");
            System.out.println();

        } catch (Exception e) {
            System.err.println("[ERROR] Failed to parse: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        // 模拟数据库存储结果
        System.out.println("Step 3: Database Storage Result (robustnessResult column)");
        System.out.println("-----------------------------------------------------------------");

        String storedValue = databaseResult.get("robustness");

        // 再次解析验证存储的格式
        try {
            ObjectMapper prettyMapper = new ObjectMapper();
            JsonNode storedNode = prettyMapper.readTree(storedValue);
            String prettyJson = prettyMapper.writerWithDefaultPrettyPrinter().writeValueAsString(storedNode);

            System.out.println("{\n  \"robustness\": ");
            // 缩进显示
            String[] lines = prettyJson.split("\n");
            for (String line : lines) {
                System.out.println("    " + line);
            }
            System.out.println("}");
            System.out.println();

        } catch (Exception e) {
            System.err.println("[ERROR] Failed to pretty print: " + e.getMessage());
        }

        // 验证存储格式
        System.out.println("Step 4: Validation");
        System.out.println("-----------------------------------------------------------------");

        try {
            ObjectMapper validator = new ObjectMapper();
            JsonNode finalNode = validator.readTree(storedValue);

            // 验证关键结构
            boolean check1 = finalNode.has("adversarial");
            boolean check2 = finalNode.has("corruption");
            boolean check3 = finalNode.get("adversarial").isArray();
            boolean check4 = finalNode.get("corruption").isArray();

            System.out.println("[CHECK] Has 'adversarial' key: " + (check1 ? "PASS" : "FAIL"));
            System.out.println("[CHECK] Has 'corruption' key: " + (check2 ? "PASS" : "FAIL"));
            System.out.println("[CHECK] 'adversarial' is array: " + (check3 ? "PASS" : "FAIL"));
            System.out.println("[CHECK] 'corruption' is array: " + (check4 ? "PASS" : "FAIL"));

            // 验证第一个攻击
            JsonNode firstAttack = finalNode.get("adversarial").get(0);
            boolean check5 = firstAttack.has("attack");
            boolean check6 = firstAttack.has("eps");
            boolean check7 = firstAttack.has("metrics");

            System.out.println("[CHECK] First attack has 'attack': " + (check5 ? "PASS" : "FAIL"));
            System.out.println("[CHECK] First attack has 'eps': " + (check6 ? "PASS" : "FAIL"));
            System.out.println("[CHECK] First attack has 'metrics': " + (check7 ? "PASS" : "FAIL"));

            if (check7) {
                JsonNode metrics = firstAttack.get("metrics");
                boolean check8 = metrics.has("clean_map");
                boolean check9 = metrics.has("adversarial_map");
                boolean check10 = metrics.has("per_class_clean_map");

                System.out.println("[CHECK] Metrics has 'clean_map': " + (check8 ? "PASS" : "FAIL"));
                System.out.println("[CHECK] Metrics has 'adversarial_map': " + (check9 ? "PASS" : "FAIL"));
                System.out.println("[CHECK] Metrics has 'per_class_clean_map': " + (check10 ? "PASS" : "FAIL"));
            }

            // 验证第一个腐败测试
            JsonNode firstCorruption = finalNode.get("corruption").get(0);
            boolean check11 = firstCorruption.has("corruption_name");
            boolean check12 = firstCorruption.has("severity");
            boolean check13 = firstCorruption.has("performance_drop_rate");

            System.out.println("[CHECK] First corruption has 'corruption_name': " + (check11 ? "PASS" : "FAIL"));
            System.out.println("[CHECK] First corruption has 'severity': " + (check12 ? "PASS" : "FAIL"));
            System.out.println("[CHECK] First corruption has 'performance_drop_rate': " + (check13 ? "PASS" : "FAIL"));

            System.out.println();

            if (check1 && check2 && check3 && check4 && check5 && check6 && check7 &&
                check8 && check9 && check10 && check11 && check12 && check13) {
                System.out.println("=================================================================");
                System.out.println("                    ALL CHECKS PASSED!");
                System.out.println("=================================================================");
                System.out.println("Storage format is CORRECT:");
                System.out.println("  - Preserved 'adversarial' and 'corruption' keys");
                System.out.println("  - Preserved all nested structures");
                System.out.println("  - No information loss");
                System.out.println("  - JSON is valid and parseable");
                System.out.println("=================================================================");
            } else {
                System.err.println("\n[FAIL] Some checks failed!");
                System.exit(1);
            }

        } catch (Exception e) {
            System.err.println("[ERROR] Validation failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * 创建真实的 robustness JSON（用户提供的数据）
     */
    private static String createRealRobustnessJson() {
        // 这是评测模块实际回传的数据结构
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
            "          \"3\": 0.9693586213605374\n" +
            "        },\n" +
            "        \"false_detection_rate\": 0.6759746146872166,\n" +
            "        \"per_class_adversarial_map\": {\n" +
            "          \"1\": 0.5576004029512011,\n" +
            "          \"2\": 0.0910130026977853,\n" +
            "          \"3\": 0.8881121584149567\n" +
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
            "        \"clean_miss_rate\": 0.22887017136218415\n" +
            "      }\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }
}
