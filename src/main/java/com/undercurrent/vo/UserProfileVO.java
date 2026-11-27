package com.undercurrent.vo;

import lombok.Data;

import java.util.List;

/** 用户资料 / 发现页艺人卡片出参：在最小 UserVO 基础上补简介与关注态 */
@Data
public class UserProfileVO {
    private Long id;
    private String name;
    private String handle;
    private Integer hue;
    /** 头像图片路径，如 /avatars/11.jpg；null 表示前端用 hue 色块占位 */
    private String avatar;
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
    private Integer posts;
    /** 个人主页配置的外部链接 */
    private List<UserLinkVO> links;
}
