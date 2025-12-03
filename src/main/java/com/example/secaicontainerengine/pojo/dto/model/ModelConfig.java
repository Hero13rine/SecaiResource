package com.example.secaicontainerengine.pojo.dto.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ModelConfig {

    // 模型任务描述
    private String description;

    // 模型网络名称
    private String modelNetName;

    // 模型权重文件名
    private String weightFileName;

    // 模型网络文件名
    private String modelNetFileName;

    // 模型框架
    private String framework;

    // 模型任务类型
    private String task;

    // 类别数目 - 前端使用下划线命名 nb_classes
    @JsonProperty("nb_classes")
    private Integer nbClasses;

    // 输入数据形状（字符串格式）- 前端使用下划线命名 input_shape
    // 格式: "[3,32,32]" 或 "[channels,height,width]"
    @JsonProperty("input_shape")
    private String inputShape;

    // 输入数据形状 - 图像通道数（解析 inputShape 后设置）
    private Integer inputChannels;

    // 输入数据形状 - 图像高度（解析 inputShape 后设置）
    private Integer inputHeight;

    // 输入数据形状 - 图像宽度（解析 inputShape 后设置）
    private Integer inputWidth;

}
