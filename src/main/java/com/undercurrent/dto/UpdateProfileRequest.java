package com.undercurrent.dto;

import lombok.Data;

/** 个人主页“编辑主页”：只允许改昵称、简介、城市 */
@Data
public class UpdateProfileRequest {
    private String nickname;
    private String bio;
    private String city;
}
