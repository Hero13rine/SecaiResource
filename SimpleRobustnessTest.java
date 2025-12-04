import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

public class SimpleRobustnessTest {
    public static void main(String[] args) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        System.out.println("=== 测试鲁棒性数据解析 ===\n");

        String robustnessJson = "{\"adversarial\": [{\"attack_name\": \"fgsm_eps_0.001\", \"map_drop_rate\": 0.062, \"miss_rate\": 0.250, \"false_detection_rate\": 0.439}], \"corruption\": [{\"corruption_name\": \"gaussian_noise\", \"severity\": 1, \"perturbation_magnitude\": 0.040, \"performance_drop_rate\": 0.102, \"perturbation_tolerance\": 0.721}]}";

        JsonNode rootNode = objectMapper.readTree(robustnessJson);
        Map<String, String> flatData = new LinkedHashMap<>();

        // 处理 adversarial
        Set<String> adversarialMetrics = new HashSet<>(Arrays.asList(
            "map_drop_rate", "miss_rate", "false_detection_rate"
        ));

        JsonNode adversarialArray = rootNode.get("adversarial");
        for (JsonNode attack : adversarialArray) {
            String attackName = attack.get("attack_name").asText();
            System.out.println("处理攻击: " + attackName);

            Iterator<Map.Entry<String, JsonNode>> fields = attack.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String fieldName = field.getKey();

                if (adversarialMetrics.contains(fieldName)) {
                    String key = fieldName + "_" + attackName;
                    String value = field.getValue().asText();
                    flatData.put(key, value);
                    System.out.println("  OK " + key + ": " + value);
                }
            }
        }

        System.out.println("\n=== 最终结果 ===");
        System.out.println("总共 " + flatData.size() + " 个字段");

        System.out.println("\n✅ 测试完成！");
    }
}
