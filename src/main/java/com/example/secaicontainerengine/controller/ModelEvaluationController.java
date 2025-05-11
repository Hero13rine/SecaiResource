package com.example.secaicontainerengine.controller;


import com.example.secaicontainerengine.common.BaseResponse;
import com.example.secaicontainerengine.common.ResultUtils;
import com.example.secaicontainerengine.exception.BusinessException;
import com.example.secaicontainerengine.pojo.dto.model.*;
import com.example.secaicontainerengine.pojo.entity.ModelMessage;
import com.example.secaicontainerengine.service.modelEvaluation.ModelEvaluationService;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ExecutorService;

import static com.example.secaicontainerengine.common.ErrorCode.SYSTEM_ERROR;

@RestController
@RequestMapping("/api/evaluation")
@Slf4j
public class ModelEvaluationController {

    @Autowired
    private ModelEvaluationService modelEvaluationService;

    @Autowired
    private ExecutorService taskExecutor;


    @PostMapping("/start")
    public BaseResponse<?> startModelEvaluation(@RequestBody ModelEvaluationRequest modelEvaluationRequest) {
        Long modelId = modelEvaluationRequest.getModelId();

//         测试

//        modelId = 1889145615706112001L;


        ModelMessage modelMessage = modelEvaluationService.getById(modelId);


        // 使用新线程异步执行任务
        taskExecutor.submit(() -> {
            try {
                // 根据 BusinessConfig 配置启动对应的 Pod
                modelEvaluationService.startEvaluationPod(modelMessage);

            } catch (Exception e) {
                // 处理异常，捕获可能发生的错误
                throw new BusinessException(SYSTEM_ERROR, e.getMessage());
            }
        });

        // 立即返回响应，告诉前端评测任务已启动
        return ResultUtils.success("系统正在评测中...");

    }
}
