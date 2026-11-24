package com.undercurrent.domain;

import lombok.Data;

/**
 * 字典项：流派 / 频道 / 插件分类共用。
 * 用 kind 区分来源表，避免为三个几乎一样的结构建三个类。
 */
@Data
public class Dict {
    private Integer id;
    private String name;
    private String slug;
    private Integer sortOrder;
}
