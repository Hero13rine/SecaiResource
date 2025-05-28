package com.example.secaicontainerengine.pojo.dto.result.detail;

import lombok.Data;

@Data
public class EvaluateResourceDetail {

    private Long totalTime;

    private Long cpuMemoryUse;

    private Long gpuMemoryUse;
}
