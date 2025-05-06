package com.example.secaicontainerengine.service.modelEvaluation;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.secaicontainerengine.pojo.dto.result.ModelEvaluationResult;


public interface ResultService extends IService<ModelEvaluationResult> {

    String getResult(Long modelId) throws Exception;

    void updateResult(ModelEvaluationResult modelEvaluationResult) throws Exception;

}