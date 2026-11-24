package com.undercurrent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 注册请求：account 存进 app_user.handle */
@Data
public class RegisterRequest {

    /** 账号注册时必填；手机号注册时留空 */
    @Pattern(regexp = "^$|^[a-zA-Z0-9_.]{3,20}$", message = "账号为 3-20 位字母、数字、下划线或点")
    private String account;

    /** 手机号注册时必填（11 位大陆手机号）；账号注册时留空 */
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "请输入正确的手机号")
    private String phone;

    @NotBlank(message = "请输入密码")
    @Size(min = 6, max = 64, message = "密码长度需为 6-64 位")
    private String password;

    /** 手机号注册时的短信验证码（账号注册不需要） */
    @Pattern(regexp = "^$|^\\d{6}$", message = "验证码为 6 位数字")
    private String smsCode;

    /** 昵称，留空则默认用账号 */
    @Size(max = 20, message = "昵称最多 20 个字")
    private String nickname;
}
