package com.example.secaicontainerengine.pojo.dto.model;

import lombok.Data;

@Data
public class TaskResetRequest {
    private Long userId;
    private Long modelId;
    /**
     * 评测维度，可为空：
     * - 为空时表示按 modelId 删除所有评测 Pod
     * - 不为空时按 userId-modelId-evaluationType 构造 Pod 名称
     */
    private String evaluationType;
    /**
     * type=0: 重置（删 Pod，保留用户文件）
     * type=1: 删除任务（删 Pod+文件）
     */
    private Integer type;
}
