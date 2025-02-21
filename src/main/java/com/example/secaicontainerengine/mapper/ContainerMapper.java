package com.example.secaicontainerengine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.secaicontainerengine.pojo.entity.Container;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ContainerMapper extends BaseMapper<Container> {

    @Select("select containerName from container where modelId = #{modelId}")
    List<String> getContainerNameByModelId(Long modelId);
}
