package com.undercurrent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 用邮箱 + 验证码重置密码 */
@Data
public class ResetPasswordRequest {

    @NotBlank(message = "请输入邮箱")
    @Pattern(regexp = "^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$", message = "请输入正确的邮箱")
    private String email;

    @NotBlank(message = "请输入验证码")
    @Pattern(regexp = "^\\d{6}$", message = "验证码为 6 位数字")
    private String code;

    @NotBlank(message = "请输入新密码")
    @Size(min = 6, max = 64, message = "密码长度需为 6-64 位")
    private String newPassword;
}
