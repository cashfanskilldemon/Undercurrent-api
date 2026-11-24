package com.undercurrent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 忘记密码：只提交账号，由管理员人工处理（demo 环境没有邮件 / 短信服务） */
@Data
public class ForgotPasswordRequest {

    @NotBlank(message = "请输入账号")
    private String account;
}
