package com.example.secaicontainerengine.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 调度任务表，用于保存所有被调度的任务
 * </p>
 *
 * @author CFZ
 * @since 2025-05-08
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("scheduled_table")
@ApiModel(value="ScheduledTable对象", description="调度任务表，用于保存所有被调度的任务")
public class ScheduledTable implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "自增主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "该任务关联的模型id")
    @TableField("modelId")
    private Long modelId;

    @ApiModelProperty(value = "当前任务的状态（0代表还未调度，1代表已经调度）")
    @TableField("status")
    private Integer status;

    @ApiModelProperty(value = "该任务的创建时间")
    @TableField("createTime")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "调度时间")
    @TableField("scheduleTime")
    private LocalDateTime scheduleTime;


}
