package com.undercurrent.vo;

import lombok.Data;

/** 前端头像块 + 昵称行所需的最小用户信息 */
@Data
public class UserVO {
    private Long id;
    private String name;
    private String handle;
    private Integer hue;
    private String city;
    private String role;
    private Boolean verified;
    private Integer followers;
}
