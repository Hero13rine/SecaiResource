package com.example.secaicontainerengine.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.secaicontainerengine.common.BaseResponse;
import com.example.secaicontainerengine.common.ResultUtils;
import com.example.secaicontainerengine.exception.BusinessException;
import com.example.secaicontainerengine.pojo.entity.Container;
import com.example.secaicontainerengine.pojo.entity.EvaluationResult;
import com.example.secaicontainerengine.pojo.entity.ModelMessage;
import com.example.secaicontainerengine.service.container.ContainerService;
import com.example.secaicontainerengine.service.modelEvaluation.EvaluationResultService;
import com.example.secaicontainerengine.service.modelEvaluation.ModelEvaluationService;
import com.example.secaicontainerengine.service.modelEvaluation.ModelMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ExecutorService;

import static com.example.secaicontainerengine.common.ErrorCode.SYSTEM_ERROR;

/**
 * <p>
 * 调度任务接口，给若依平台调用
 * </p>
 *
 * @author CFZ
 * @since 2025-05-08
 */
@Slf4j
@RestController
@RequestMapping("/scheduled-task")
public class ScheduledTaskController {

    @Autowired
    private ExecutorService taskExecutor;

    @Autowired
    private ModelMessageService modelMessageService;

    @Autowired
    private ModelEvaluationService modelEvaluationService;

    @Autowired
    private ContainerService containerService;

    @Autowired
    private EvaluationResultService evaluationResultService;



    /**
     * 返回与modelId关联的Pod是否全部执行完毕
     * @param modelId
     * @return 0代表还有Pod没有执行完毕，1代表Pod全部执行成功，2代表有Pod执行失败
     */
    @GetMapping
    public String allFinished(@RequestParam Long modelId){
        // 2.1检查所有容器是否都已经完成
        List<EvaluationResult> pods = evaluationResultService.list(
                new QueryWrapper<EvaluationResult>().eq("modelId", modelId)
        );
        boolean notFinished = pods==null || pods.isEmpty() || pods.stream()
                .anyMatch(pod -> {
                    String status = pod.getStatus();
                    return "评测中".equals(status);
                });
        if(notFinished){
            return String.valueOf(1);
        }
        List<Long> ids = evaluationResultService.list(
                new QueryWrapper<EvaluationResult>()
                        .eq("modelId", modelId)
                        .eq("status", "失败")
        ).stream().map(EvaluationResult::getId).toList();
        if(ids.isEmpty()){
            return String.valueOf(2);
        }else {
            return String.valueOf(3);
        }
    }

    /**
     * 计算modelId的评测结果
     * @param modelId
     */
    @PostMapping("/result/{modelId}")
    public void computeResult(@PathVariable Long modelId){
        evaluationResultService.calculateAndUpdateScores(modelId);
    }




}
