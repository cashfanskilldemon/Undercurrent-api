package com.undercurrent.domain;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 插件（效果器 / 鼓机 / 采样包等） */
@Data
public class Plugin {
    private Long id;
    private Long makerId;
    private String name;
    private Integer categoryId;
    private BigDecimal price;
    private BigDecimal rating;
    private Integer reviewsCount;
    private Integer downloads;
    private String formats;
    private String os;
    private String version;
    private String fileSize;
    private Integer hue;
    private Integer knobs;
    private String tagline;
    private String description;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;
}
