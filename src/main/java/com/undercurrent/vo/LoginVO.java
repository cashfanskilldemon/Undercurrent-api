package com.undercurrent.vo;

import lombok.Data;

/** 登录出参：token + 有效期 + 用户资料 */
@Data
public class LoginVO {
    /** 放在 Authorization: Bearer 后面 */
    private String token;
    /** 有效期（秒） */
    private Long expiresIn;
    private UserProfileVO user;
}
