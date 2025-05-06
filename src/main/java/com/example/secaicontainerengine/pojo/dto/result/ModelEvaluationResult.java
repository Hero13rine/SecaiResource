package com.example.secaicontainerengine.pojo.dto.result;

import lombok.Data;

@Data
public class ModelEvaluationResult {
    
    private Long modelId;

    private Long userId;

    private String resultType; // 内容为 backdoorAttackResult or adversarialAttackResult

    private String result;

}
