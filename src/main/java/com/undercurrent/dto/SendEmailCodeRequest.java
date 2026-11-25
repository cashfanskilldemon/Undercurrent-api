package com.undercurrent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** 发送邮箱验证码：scene = register（注册） / reset（重置密码） */
@Data
public class SendEmailCodeRequest {

    @NotBlank(message = "请输入邮箱")
    @Pattern(regexp = "^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$", message = "请输入正确的邮箱")
    private String email;

    @NotBlank(message = "缺少场景")
    @Pattern(regexp = "^(register|reset)$", message = "场景只能是 register 或 reset")
    private String scene;
}
