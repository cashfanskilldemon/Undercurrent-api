package com.undercurrent.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 插件 */
@Data
public class PluginVO {
    private Long id;
    private String name;
    private UserVO maker;
    private String category;
    private BigDecimal price;
    private BigDecimal rating;
    private Integer reviews;
    private Integer downloads;
    private List<String> formats;
    private List<String> os;
    private String version;
    private String size;
    private Integer hue;
    private Integer knobs;
    private String tagline;
    private String description;
    private LocalDateTime updatedAt;
}
