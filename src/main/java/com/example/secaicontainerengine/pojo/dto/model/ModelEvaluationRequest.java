package com.example.secaicontainerengine.pojo.dto.model;

import lombok.Data;

@Data
public class ModelEvaluationRequest {

    private Long modelId;

    private ModelConfig modelConfig;

    private ResourceConfig resourceConfig;

    private BusinessConfig businessConfig;

    private static final long serialVersionUID = 1L;
}
