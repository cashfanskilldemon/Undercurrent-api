package com.undercurrent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** 发送短信验证码：scene = register（注册） / reset（重置密码） */
@Data
public class SendSmsCodeRequest {

    @NotBlank(message = "请输入手机号")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的手机号")
    private String phone;

    @NotBlank(message = "缺少场景")
    @Pattern(regexp = "^(register|reset)$", message = "场景只能是 register 或 reset")
    private String scene;
}
