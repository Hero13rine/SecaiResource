package com.example.secaicontainerengine.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@TableName("model_evaluation")
public class ModelEvaluation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long modelId;

    private Long userId;

    private BigDecimal backdoorAttackScore;

    private String backdoorAttackStatus;

    private BigDecimal adversarialAttackScore;

    private String adversarialAttackStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;

    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
