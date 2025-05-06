package com.example.secaicontainerengine.controller;

import com.example.secaicontainerengine.common.BaseResponse;
import com.example.secaicontainerengine.common.ResultUtils;
import com.example.secaicontainerengine.pojo.dto.result.ModelEvaluationResult;
import com.example.secaicontainerengine.service.modelEvaluation.ResultService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/result")
@Slf4j
public class ResultController {
    @Autowired
    private ResultService resultService;

    @GetMapping("/{modelId}")
    public String getAttackResult(@PathVariable("modelId") Long modelId) {
        try {
            return resultService.getResult(modelId);
        } catch (Exception e) {
            log.error("Error getting attack result", e);
            return "Error getting attack result";
        }
    }

    @PostMapping()
    public BaseResponse<String> updateAttackResult(@RequestBody ModelEvaluationResult modelEvaluationResult) {
        log.info(String.valueOf(modelEvaluationResult));
        try {
            resultService.updateResult(modelEvaluationResult);
        } catch (Exception e) {
            log.error("Error updating attack result", e);
            return ResultUtils.error(50001, "数据传输失败！");
        }
        return ResultUtils.success("数据传输成功！");
    }
    
}


