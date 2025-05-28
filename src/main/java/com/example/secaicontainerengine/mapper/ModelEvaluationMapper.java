package com.example.secaicontainerengine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.secaicontainerengine.pojo.entity.ModelEvaluation;
import org.apache.ibatis.annotations.Select;

public interface ModelEvaluationMapper extends BaseMapper<ModelEvaluation> {

    @Select("select createImageTime from model_evaluation where modelId=#{modelId}")
    Long getCreateImageTimeByModelId(Long modelId);
}
