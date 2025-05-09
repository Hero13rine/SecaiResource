package com.example.secaicontainerengine.controller;


import cn.hutool.json.JSONUtil;
import com.example.secaicontainerengine.common.BaseResponse;
import com.example.secaicontainerengine.common.ErrorCode;
import com.example.secaicontainerengine.common.ResultUtils;
import com.example.secaicontainerengine.exception.BusinessException;
import com.example.secaicontainerengine.pojo.dto.model.BusinessConfig;
import com.example.secaicontainerengine.pojo.dto.model.ModelConfig;
import com.example.secaicontainerengine.pojo.dto.model.ModelEvaluationRequest;
import com.example.secaicontainerengine.pojo.dto.model.ResourceConfig;
import com.example.secaicontainerengine.pojo.entity.ModelMessage;
import com.example.secaicontainerengine.pojo.entity.ScheduledTable;
import com.example.secaicontainerengine.service.modelEvaluation.ModelEvaluationService;
import com.example.secaicontainerengine.service.scheduledTable.ScheduledTableService;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ExecutorService;

@RestController
@RequestMapping("/api/evaluation")
@Slf4j
public class modelEvaluationController {

    @Autowired
    private ModelEvaluationService modelEvaluationService;

    @Autowired
    private ScheduledTableService scheduledTaskService;

    @Autowired
    private ExecutorService executorService;


    @PostMapping("/start")
    public BaseResponse<?> startModelEvaluation(@RequestBody ModelEvaluationRequest modelEvaluationRequest) {
        Long modelId = modelEvaluationRequest.getModelId();

//         测试

//        modelId = 1889145615706112001L;


        ModelMessage modelMessage = modelEvaluationService.getById(modelId);

        ModelConfig modelConfig = modelEvaluationRequest.getModelConfig();
        if(modelConfig != null) {
            modelMessage.setModelConfig(JSONUtil.toJsonStr(modelConfig));
        }
        ResourceConfig resourceConfig = modelEvaluationRequest.getResourceConfig();
        if(resourceConfig != null) {
            modelMessage.setResourceConfig(JSONUtil.toJsonStr(resourceConfig));
        }
//        BusinessConfig businessConfig = modelEvaluationRequest.getBusinessConfig();

        // 测试
        BusinessConfig businessConfig = new BusinessConfig();
        businessConfig.setAdversarialAttack(true);
//        businessConfig.setBackdoorAttack(true);
        if(businessConfig != null) {
            modelMessage.setBusinessConfig(JSONUtil.toJsonStr(businessConfig));
        }

        boolean update = modelEvaluationService.updateById(modelMessage);
        if(update) {
            log.info("model evaluation update success");
        }
        else{
            log.info("model evaluation update failed");
        }

        // 使用新线程异步执行任务
        executorService.submit(() -> {
            try {
                // 根据 BusinessConfig 配置启动对应的 Pod
                modelEvaluationService.startEvaluationPod(modelMessage);

            } catch (Exception e) {
                // 处理异常，捕获可能发生的错误
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, e.getMessage());
            }
        });

        // 立即返回响应，告诉前端评测任务已启动
        return ResultUtils.success("系统正在评测中...");

    }

    // 任务调度方式启动
    @PostMapping("/startNew")
    public BaseResponse<?> startModelEvaluationNew(@RequestBody ModelEvaluationRequest modelEvaluationRequest){
        Long modelId = modelEvaluationRequest.getModelId();
        ModelMessage modelMessage = modelEvaluationService.getById(modelId);
        if(modelMessage==null){
            return ResultUtils.error(4000, "不存在该modelId");
        }
        switch (modelMessage.getStatus()){
            // 0在数据库中代表还未上传至nfs服务器
            case 0:
                return ResultUtils.error(4001,"数据正在上传至nfs服务器，请稍后再试");
            // 1在数据库中代表已经上传至nfs服务器
            case 1:
                ScheduledTable task=new ScheduledTable();
                task.setModelId(modelId);
                scheduledTaskService.save(task);
                break;
            // 2代表该模型在评测中
            case 2:
                return ResultUtils.error(4002, "该modelId正在评测中");
            // 3代表该模型已评测成功
            case 3:
                return ResultUtils.error(4003, "该modelId已评测成功");
            // 4代表该模型已评测失败
            case 4:
                return ResultUtils.error(4004, "该modelId已评测失败");
        }
        return ResultUtils.success("该modelId成功加入任务调度队列");
    }
}
