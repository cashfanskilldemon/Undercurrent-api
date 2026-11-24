package com.undercurrent.vo;

import lombok.Data;

/** 用户资料 / 发现页艺人卡片出参：在最小 UserVO 基础上补简介与关注态 */
@Data
public class UserProfileVO {
    private Long id;
    private String name;
    private String handle;
    private Integer hue;
    private String city;
    private String role;
    private String bio;
    private Boolean verified;
    private Integer followers;
    private Integer following;
    /** 当前查看者是否已关注该用户 */
    private Boolean followed;
    /** 该用户的作品数 / 帖子数，个人主页统计用 */
    private Integer tracks;
    private Integer threads;
}
