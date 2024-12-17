package com.example.secaicontainerengine.service.container;

import freemarker.template.TemplateException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface ContainerService {

    //初始化接口
    List<ByteArrayInputStream> init(String userId, Map<String, String> imageUrl, Map<String, Map> imageParam) throws IOException, TemplateException;

    //启动接口
    void start(String userId, List<ByteArrayInputStream> streams) throws IOException;

    //回收接口1-删除指定用户的所有pod
    void deleteAll(String userId);

    //回收接口2-删除用户的单个pod
    void deleteSingle(String userId, String podName);

}
