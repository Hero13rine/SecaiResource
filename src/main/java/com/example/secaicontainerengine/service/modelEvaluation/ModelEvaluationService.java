package com.example.secaicontainerengine.service.modelEvaluation;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.secaicontainerengine.pojo.dto.model.BusinessConfig;
import com.example.secaicontainerengine.pojo.entity.ModelMessage;

public interface ModelEvaluationService extends IService<ModelMessage> {

    void startEvaluationPod(ModelMessage modelMessage) throws Exception;
}
