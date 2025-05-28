package com.example.secaicontainerengine.pojo.dto.result;


import com.example.secaicontainerengine.pojo.dto.result.detail.EvaluateResourceDetail;
import com.example.secaicontainerengine.pojo.dto.result.detail.EvaluateTimeDetail;
import com.example.secaicontainerengine.pojo.dto.result.detail.FileUploadDetail;
import lombok.Data;

import java.util.Map;

@Data
public class ModelEvaluationDetail {

    private FileUploadDetail fileUploadDetail;

    private Map<String, EvaluateTimeDetail> evaluateTimeDetail;

    private Map<String, EvaluateResourceDetail> evaluateResourceDetail;
}
