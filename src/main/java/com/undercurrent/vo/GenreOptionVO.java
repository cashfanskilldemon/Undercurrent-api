package com.undercurrent.vo;

import lombok.Data;

/** 流派字典的一行：名称 + 所属分类（引导页泡泡云按分类分组用） */
@Data
public class GenreOptionVO {
    private String name;
    private String category;
}
