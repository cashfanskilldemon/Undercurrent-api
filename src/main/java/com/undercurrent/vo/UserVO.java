package com.undercurrent.vo;

import lombok.Data;

/** 前端头像块 + 昵称行所需的最小用户信息 */
@Data
public class UserVO {
    private Long id;
    private String name;
    private String handle;
    private Integer hue;
    /** 头像图片路径；null 表示色块占位 */
    private String avatar;
    private String city;
    private String role;
    private Boolean verified;
    private Integer followers;
}
