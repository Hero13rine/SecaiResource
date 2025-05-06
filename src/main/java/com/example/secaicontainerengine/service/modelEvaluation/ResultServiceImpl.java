package com.example.secaicontainerengine.service.modelEvaluation;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.secaicontainerengine.mapper.AttackResultMapper;
import com.example.secaicontainerengine.pojo.dto.result.ModelEvaluationResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ResultServiceImpl extends ServiceImpl< AttackResultMapper, ModelEvaluationResult>  implements ResultService {

    @Autowired
    private AttackResultMapper attackResultMapper;

    @Autowired
    private ObjectMapper objectMapper;  // 用于处理 JSON

    public String getResult(Long modelId) throws Exception {
        String jsonResult = attackResultMapper.getByModelId(modelId);
    
        // 为空时返回空 JSON
        if (jsonResult == null || jsonResult.isEmpty()) {
            return "{}";
        }
    
        // 直接返回 JSON
        return jsonResult;
    }
    
    

    public void updateResult(ModelEvaluationResult modelEvaluationResult) {
        String existingResultJson = attackResultMapper.getResultByModelId(modelEvaluationResult.getModelId(), modelEvaluationResult.getResultType());
        String newResultJson = modelEvaluationResult.getResult();
        // 解析 JSON
        ObjectNode mergedResult;
        try {
            if (existingResultJson != null && !existingResultJson.isEmpty()) {
                JsonNode existingResultNode = objectMapper.readTree(existingResultJson);
                JsonNode newResultNode = objectMapper.readTree(newResultJson);

                // 合并 JSON（只更新已有 key，不删除原数据）
                mergedResult = (ObjectNode) existingResultNode;
                newResultNode.fields().forEachRemaining(entry -> 
                    mergedResult.set(entry.getKey(), entry.getValue()) // 添加或更新 key
                );
            } else {
                // 直接使用新的 result
                mergedResult = (ObjectNode) objectMapper.readTree(newResultJson);
            }

            // 更新数据库
            attackResultMapper.upsertById(modelEvaluationResult.getModelId(),modelEvaluationResult.getUserId(), modelEvaluationResult.getResultType(), objectMapper.writeValueAsString(mergedResult));
        } catch (JsonProcessingException e) {
            log.error("Error processing JSON", e);
            throw new RuntimeException("Error processing JSON", e);
        }
    }
    
}
