package com.example.secaicontainerengine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.secaicontainerengine.pojo.dto.result.ModelEvaluationResult;
import io.lettuce.core.dynamic.annotation.Param;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AttackResultMapper extends BaseMapper<ModelEvaluationResult> {


    @Select("SELECT JSON_OBJECT('backdoorAttackResult', backdoorAttackResult, 'adversarialAttackResult', adversarialAttackResult) " +
        "FROM model_evaluation WHERE modelId = #{modelId}")
    String getByModelId(Long modelId);


    void upsertById(@Param("modelId") Long modelId, 
                    @Param("userId") Long userId, 
                    @Param("resultType") String resultType, 
                    @Param("result") String result);


    String getResultByModelId(@Param("modelId") Long modelId, @Param("resultType") String resultType);
    
}
