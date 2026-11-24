package com.undercurrent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 登录请求：account 传用户 handle（唯一用户名） */
@Data
public class LoginRequest {

    @NotBlank(message = "请输入账号")
    private String account;

    @NotBlank(message = "请输入密码")
    private String password;
}
