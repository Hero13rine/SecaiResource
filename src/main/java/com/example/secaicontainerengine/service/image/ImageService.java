package com.example.secaicontainerengine.service.image;

import java.util.List;
import java.util.Map;

public interface ImageService {

    Map<String, String> getUrlByName(List<String> containers);

}
