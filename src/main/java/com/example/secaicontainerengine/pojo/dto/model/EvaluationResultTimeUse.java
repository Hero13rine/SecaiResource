package com.example.secaicontainerengine.pojo.dto.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EvaluationResultTimeUse {

    private Long totalTime;

    private Long createImageTime;

    private Long containerCreatingTime;

    private Long runningTime;
}
