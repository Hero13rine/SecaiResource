# 鲁棒性指标解析方法修改说明

## 问题描述

原来的 `parseAndStoreRobustness` 方法使用**白名单机制**，只存储少数几个指标，导致大量重要指标被丢弃。

### 旧方法存储的字段

**Adversarial（对抗攻击）**：
- ✅ map_drop_rate
- ✅ miss_rate
- ✅ false_detection_rate
- ❌ clean_map（丢失）
- ❌ adversarial_map（丢失）
- ❌ clean_miss_rate（丢失）
- ❌ clean_false_detection_rate（丢失）
- ❌ per_class_clean_map（丢失）
- ❌ per_class_adversarial_map（丢失）

**Corruption（腐败测试）**：
- ✅ perturbation_magnitude
- ✅ performance_drop_rate
- ✅ perturbation_tolerance

### 问题示例

**输入 JSON**：
```json
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
        "per_class_clean_map": {"1": 0.98, "2": 0.85},
        "per_class_adversarial_map": {"1": 0.97, "2": 0.78}
      }
    }
  ]
}
```

**旧方法存储结果**（只有3个字段）：
```json
{
  "map_drop_rate_fgsm_eps_0.001": "0.058",
  "miss_rate_fgsm_eps_0.001": "0.25",
  "false_detection_rate_fgsm_eps_0.001": "0.44"
}
```

**6个重要指标全部丢失！**

---

## 解决方案

修改 `parseAndStoreRobustness` 方法，使用**黑名单机制**替代白名单机制。

### 核心改动

#### 1. Adversarial 处理逻辑

**旧代码**（src/main/java/.../EvaluationResultServiceImpl.java:410-444）：
```java
// 白名单：只存储这3个字段
Set<String> adversarialMetrics = new HashSet<>(Arrays.asList(
    "map_drop_rate", "miss_rate", "false_detection_rate"
));

// 只遍历白名单中的字段
for (String metricName : adversarialMetrics) {
    if (metricsNode.has(metricName)) {
        // 存储...
    }
}
```

**新代码**：
```java
// 黑名单：只排除配置参数
Set<String> excludedFields = new HashSet<>(Arrays.asList(
    "attack", "eps", "attack_name"
));

// 遍历所有字段
Iterator<Map.Entry<String, JsonNode>> fields = metricsNode.fields();
while (fields.hasNext()) {
    Map.Entry<String, JsonNode> field = fields.next();
    String metricName = field.getKey();

    // 跳过配置参数
    if (excludedFields.contains(metricName)) {
        continue;
    }

    // 处理嵌套对象（如 per_class_clean_map）
    if (valueNode.isObject()) {
        String value = objectMapper.writeValueAsString(valueNode);
        // 存储为 JSON 字符串
    } else {
        // 存储普通值
    }
}
```

#### 2. Corruption 处理逻辑

同样从白名单改为黑名单：

```java
// 黑名单：只排除配置参数
Set<String> excludedFields = new HashSet<>(Arrays.asList(
    "corruption_name", "corruption_key", "severity"
));

// 遍历所有字段并存储
```

### 新方法存储结果

使用相同的输入 JSON，新方法存储**所有9个字段**：

```json
{
  "clean_map_fgsm_eps_0.001": "0.86",
  "adversarial_map_fgsm_eps_0.001": "0.81",
  "map_drop_rate_fgsm_eps_0.001": "0.058",
  "miss_rate_fgsm_eps_0.001": "0.25",
  "false_detection_rate_fgsm_eps_0.001": "0.44",
  "clean_miss_rate_fgsm_eps_0.001": "0.23",
  "clean_false_detection_rate_fgsm_eps_0.001": "0.45",
  "per_class_clean_map_fgsm_eps_0.001": "{\"1\":0.98,\"2\":0.85}",
  "per_class_adversarial_map_fgsm_eps_0.001": "{\"1\":0.97,\"2\":0.78}"
}
```

---

## 关键特性

### 1. ✅ 保留所有评测指标

不再限制存储哪些字段，只要不是配置参数（attack、eps等），全部存储。

### 2. ✅ 处理嵌套对象

对于 `per_class_clean_map`、`per_class_adversarial_map` 这种嵌套对象，序列化为 JSON 字符串存储：

```json
{
  "per_class_clean_map_fgsm_eps_0.001": "{\"1\":0.98,\"2\":0.85}"
}
```

使用时可以反序列化：
```java
JsonNode perClassMap = objectMapper.readTree(value);
double class1Value = perClassMap.get("1").asDouble();
```

### 3. ✅ 兼容多种输入格式

支持两种攻击名称格式：
- `{"attack": "fgsm", "eps": 0.001}` → `fgsm_eps_0.001`
- `{"attack_name": "fgsm_eps_0.001"}` → `fgsm_eps_0.001`

支持 metrics 在对象内部或直接在顶层：
- `{"attack": "fgsm", "metrics": {...}}`
- `{"attack": "fgsm", "map_drop_rate": 0.058, ...}`

### 4. ✅ 保持扁平化格式

与其他维度（basicResult、interpretabilityResult等）格式一致，都是扁平的键值对。

---

## 测试验证

创建了 `RobustnessFullFieldsTest.java` 测试用例，验证：

1. ✅ 所有 adversarial 指标都被存储（9个字段）
2. ✅ 所有 corruption 指标都被存储（3个字段）
3. ✅ 嵌套对象正确序列化和反序列化
4. ✅ 键名格式正确（指标名_攻击方法_参数）

---

## 对现有代码的影响

### 无需修改的部分

1. **数据库结构**：不需要修改，仍然存储在 `robustnessResult` 列的 JSON 字段中
2. **调用方式**：`updateResult()` 方法的调用方式不变
3. **存储格式**：仍然是扁平的键值对，与其他维度一致

### 可能需要更新的部分

**得分计算逻辑**（`computeRobustnessScore` 方法）：

目前代码已经支持扁平格式，在 `computeDetectionRobustnessScoreFromFlatFormat()` 方法中：

```java
// 提取 adversarial 指标
if (key.startsWith("map_drop_rate_")) {
    String attackName = key.substring("map_drop_rate_".length());
    adversarialMetrics.computeIfAbsent(attackName, k -> new HashMap<>())
        .put("map_drop_rate", value);
}
```

这个逻辑会自动识别并使用新存储的所有指标。

---

## 实际示例对比

### 场景：目标检测模型的鲁棒性评测

**评测模块回传**：
```json
{
  "adversarial": [
    {
      "attack": "fgsm", "eps": 0.001,
      "metrics": {
        "clean_map": 0.86, "adversarial_map": 0.81,
        "map_drop_rate": 0.058, "miss_rate": 0.25,
        "false_detection_rate": 0.44,
        "clean_miss_rate": 0.23,
        "clean_false_detection_rate": 0.45,
        "per_class_clean_map": {"person": 0.98, "car": 0.85},
        "per_class_adversarial_map": {"person": 0.97, "car": 0.78}
      }
    }
  ],
  "corruptions": [
    {
      "corruption_name": "gaussian_noise", "severity": 1,
      "metrics": {
        "perturbation_magnitude": 0.04,
        "performance_drop_rate": 0.06,
        "perturbation_tolerance": 0.92
      }
    }
  ]
}
```

**旧方法存储**（6个字段丢失）：
```json
{
  "map_drop_rate_fgsm_eps_0.001": "0.058",
  "miss_rate_fgsm_eps_0.001": "0.25",
  "false_detection_rate_fgsm_eps_0.001": "0.44",
  "perturbation_magnitude_gaussian_noise_1": "0.04",
  "performance_drop_rate_gaussian_noise_1": "0.06",
  "perturbation_tolerance_gaussian_noise_1": "0.92"
}
```
❌ **问题**：无法获知攻击前后的 mAP 变化、无法查看每个类别的性能

**新方法存储**（完整保留）：
```json
{
  "clean_map_fgsm_eps_0.001": "0.86",
  "adversarial_map_fgsm_eps_0.001": "0.81",
  "map_drop_rate_fgsm_eps_0.001": "0.058",
  "miss_rate_fgsm_eps_0.001": "0.25",
  "false_detection_rate_fgsm_eps_0.001": "0.44",
  "clean_miss_rate_fgsm_eps_0.001": "0.23",
  "clean_false_detection_rate_fgsm_eps_0.001": "0.45",
  "per_class_clean_map_fgsm_eps_0.001": "{\"person\":0.98,\"car\":0.85}",
  "per_class_adversarial_map_fgsm_eps_0.001": "{\"person\":0.97,\"car\":0.78}",
  "perturbation_magnitude_gaussian_noise_1": "0.04",
  "performance_drop_rate_gaussian_noise_1": "0.06",
  "perturbation_tolerance_gaussian_noise_1": "0.92"
}
```
✅ **优势**：可以完整分析攻击效果、查看每个类别的鲁棒性表现

---

## 总结

| 对比项 | 旧方法（白名单） | 新方法（黑名单） |
|--------|------------------|------------------|
| **存储字段数** | 6个（固定） | 12+个（全部） |
| **信息丢失** | ❌ 严重丢失 | ✅ 完整保留 |
| **扩展性** | ❌ 需要修改代码 | ✅ 自动支持新字段 |
| **嵌套对象** | ❌ 不支持 | ✅ 序列化为JSON |
| **格式一致性** | ✅ 扁平化 | ✅ 扁平化 |

**修改位置**：
- 文件：`src/main/java/com/example/secaicontainerengine/service/modelEvaluation/EvaluationResultServiceImpl.java`
- 方法：`parseAndStoreRobustness()`（399-538行）
- 测试：`src/test/java/com/example/secaicontainerengine/RobustnessFullFieldsTest.java`

**核心思想**：从"只存储指定字段"改为"存储所有字段（排除配置参数）"
