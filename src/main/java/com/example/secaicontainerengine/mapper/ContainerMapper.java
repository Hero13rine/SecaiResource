package com.example.secaicontainerengine.mapper;

import com.example.secaicontainerengine.pojo.entity.Container;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ContainerMapper {

    @Insert("insert into container(id, container_name, image_id, user_id, create_time) " +
            "values (#{id}, #{containerName}, #{imageId}, #{userId}, #{createTime})")
    void insert(Container container);


}
