package com.example.secaicontainerengine.controller;

import com.example.secaicontainerengine.pojo.dto.result.ModelEvaluationDetail;
import com.example.secaicontainerengine.pojo.dto.result.detail.EvaluateResourceDetail;
import com.example.secaicontainerengine.pojo.dto.result.detail.EvaluateTimeDetail;
import com.example.secaicontainerengine.pojo.dto.result.detail.FileUploadDetail;
import com.example.secaicontainerengine.pojo.entity.ModelMessage;
import com.example.secaicontainerengine.service.modelEvaluation.EvaluationResultService;
import com.example.secaicontainerengine.service.modelEvaluation.ModelEvaluationService;
import com.example.secaicontainerengine.service.modelEvaluation.ModelMessageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/detail")
@Slf4j
public class DetailController {

    @Autowired
    private ModelMessageService modelMessageService;

    @Autowired
    private ModelEvaluationService modelEvaluationService;

    @Autowired
    private EvaluationResultService evaluationResultService;

    @Autowired
    private ObjectMapper objectMapper;


    @GetMapping("/{modelId}")
    public ModelEvaluationDetail getContainersByModelId(@PathVariable Long modelId) throws JsonProcessingException {
        //1.查询文件上传时间
        //1.1从model_message表中查询文件上传耗费时间和上传完毕时间点
        ModelMessage modelMessage = modelMessageService.getById(modelId);
        Long uploadCostTime = modelMessage.getUploadcostTime();
        LocalDateTime uploadFinishedTime = modelMessage.getUploadfinishedTime();
        //1.2组装成FileUploadDetail对象
        FileUploadDetail fileUploadDetail = FileUploadDetail.builder()
                .uploadCostTime(uploadCostTime)
                .uploadFinishedTime(uploadFinishedTime)
                .build();

        //2.返回每个Pod的三个时间
        //2.1从model_evaluation表中查询出创建镜像耗费的时间
        Long createImageTime = modelEvaluationService.getCreateImageTimeByModelId(modelId);
        //2.2从evaluation_result表中查询
        List<Map<String, Object>> result = evaluationResultService.getEvaluationDetailByModelId(modelId);
        //2.3组装成EvaluateTimeDetail对象数组
        Map<String, EvaluateTimeDetail> evaluateTimeDetailHashMap= new HashMap<>();
        for (Map<String, Object> item : result) {
            EvaluateTimeDetail evaluateTimeDetail=new EvaluateTimeDetail();
            evaluateTimeDetail.setCreateImageTime(createImageTime);
            String timeUse = (String) item.get("timeUse");
            if(timeUse!=null){
                Map<String, Object> timeUseMap = objectMapper.readValue(timeUse, Map.class);
                evaluateTimeDetail.setContainerCreatingTime(((Number) timeUseMap.get("containerCreatingTime")).longValue());
                evaluateTimeDetail.setRunningTime(((Number) timeUseMap.get("runningTime")).longValue());
            }
            evaluateTimeDetailHashMap.put((String) item.get("methodName"), evaluateTimeDetail);
        }

        //3.返回每个Pod的资源使用情况
        Map<String, EvaluateResourceDetail> evaluateResourceDetailMap=new HashMap<>();
        for (Map<String, Object> item : result) {
            EvaluateResourceDetail evaluateResourceDetail=new EvaluateResourceDetail();
            String timeUse = (String) item.get("timeUse");
            if(timeUse!=null){
                Map<String, Object> timeUseMap = objectMapper.readValue(timeUse, Map.class);
                evaluateResourceDetail.setTotalTime(((Number) timeUseMap.get("totalTime")).longValue());
            }
            evaluateResourceDetail.setCpuMemoryUse((Long) item.get("cpuMemoryUse"));
            evaluateResourceDetail.setGpuMemoryUse((Long) item.get("gpuMemoryUse"));
            evaluateResourceDetailMap.put((String) item.get("methodName"), evaluateResourceDetail);
        }

        ModelEvaluationDetail modelEvaluationDetail=new ModelEvaluationDetail();
        modelEvaluationDetail.setFileUploadDetail(fileUploadDetail);
        modelEvaluationDetail.setEvaluateTimeDetail(evaluateTimeDetailHashMap);
        modelEvaluationDetail.setEvaluateResourceDetail(evaluateResourceDetailMap);
        return modelEvaluationDetail;
    }
}
