package com.example.secaicontainerengine.constant;

import java.io.File;

public interface FileConstant {

    //指定nfs挂载目录
    String FILE_BASE_PATH = System.getProperty("user.dir") + File.separator + "src" + File.separator + "main" + File.separator + "resources";
    long FILE_MAX_SIZE = 1024L * 1024L * 1024L * 20L; //20G
}
