package com.example.secaicontainerengine.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ImageMapper {

    @Select("select url from image where image_name = #{name}")
    String getUrlByName(String name);
}
