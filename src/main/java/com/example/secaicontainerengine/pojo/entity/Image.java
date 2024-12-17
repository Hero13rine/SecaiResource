package com.example.secaicontainerengine.pojo.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Image implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer id;

    private String imageName;

    private String url;

    private String creationTime;

    private Long size;
}
