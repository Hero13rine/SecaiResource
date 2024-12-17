package com.example.secaicontainerengine.service.image;

import com.example.secaicontainerengine.mapper.ImageMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service(value = "k8sImageImpl")
@Slf4j
public class K8sImpl implements ImageService {

    @Autowired
    private ImageMapper imageMapper;


    @Override
    public Map<String, String> getUrlByName(List<String> images) {
        Map<String, String> imageUrl = new HashMap<>();
        for (String image : images) {
            String url = imageMapper.getUrlByName(image);
            imageUrl.put(image, url);
        }
        return imageUrl;
    }
}
