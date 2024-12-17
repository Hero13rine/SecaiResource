package com.example.secaicontainerengine.util;

import com.example.secaicontainerengine.pojo.dto.RequestDTO;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class RequestDTOUtil {

    public static List<String> getImagesName(RequestDTO requestDTO) {
        List<String> resultList = new ArrayList<>();

        List<Object> images = requestDTO.getImages();
        images.forEach(image ->{
            HashMap ct = (HashMap) image;
            resultList.add((String) ct.get("name"));
        });

        return resultList;
    }

    public static Map getImagesParam(RequestDTO requestDTO) {
        Map<String, HashMap> resultMap = new HashMap<>();

        List<Object> images = requestDTO.getImages();
        images.forEach(image ->{
            HashMap ct = (HashMap) image;
            String name = (String) ct.get("name");
            HashMap param = (HashMap) ct.get("param");
            resultMap.put(name, param);
        });

        return resultMap;
    }

    public static Map getResourceLimit(RequestDTO requestDTO) {
        Map<String, HashMap> resultMap = new HashMap<>();

        List<Object> images = requestDTO.getImages();
        images.forEach(image ->{
            HashMap ct = (HashMap) image;
            String name = (String) ct.get("name");
            HashMap limit = (HashMap) ct.get("limit");
            resultMap.put(name, limit);
        });

        return resultMap;
    }




}
