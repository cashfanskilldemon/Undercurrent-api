package com.undercurrent.controller;

import com.undercurrent.common.ApiResponse;
import com.undercurrent.common.TokenService;
import com.undercurrent.common.VerificationCodeService;
import com.undercurrent.dto.ForgotPasswordRequest;
import com.undercurrent.dto.LoginRequest;
import com.undercurrent.dto.RegisterRequest;
import com.undercurrent.dto.ResetPasswordRequest;
import com.undercurrent.dto.SendEmailCodeRequest;
import com.undercurrent.dto.SendSmsCodeRequest;
import com.undercurrent.service.AuthService;
import com.undercurrent.service.UserService;
import com.undercurrent.vo.LoginVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final TokenService tokenService;

    /** dev 环境把验证码回传给前端方便联调，上线必须关掉 */
    @Value("${app.code.expose-code:true}")
    private boolean exposeCode;

    /**
     * 登录：account 可以是邮箱、账号（handle），也兼容老用户的手机号。
     * 成功返回签名 token + 用户资料；后续请求带 {@code Authorization: Bearer <token>}。
     */
    @PostMapping("/login")
    public ApiResponse<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        Long userId = authService.authenticate(request.getAccount(), request.getPassword());
        return ApiResponse.ok(loginResult(userId));
    }

    /** 注册：邮箱（需验证码）或账号，成功后直接返回 token */
    @PostMapping("/register")
    public ApiResponse<LoginVO> register(@Valid @RequestBody RegisterRequest request) {
        Long userId = authService.register(request.getAccount(), request.getEmail(),
                request.getPassword(), request.getNickname(), request.getEmailCode());
        return ApiResponse.ok(loginResult(userId));
    }

    /** 发送邮箱验证码（注册 / 重置密码） */
    @PostMapping("/email-code")
    public ApiResponse<Map<String, Object>> sendEmailCode(@Valid @RequestBody SendEmailCodeRequest request) {
        String code = authService.sendCode(request.getEmail(), request.getScene(), VerificationCodeService.Channel.EMAIL);
        return ApiResponse.ok(codePayload(code));
    }

    /** 发送短信验证码：留给后续「手机号实名认证」用，注册登录已不走短信 */
    @PostMapping("/sms-code")
    public ApiResponse<Map<String, Object>> sendSmsCode(@Valid @RequestBody SendSmsCodeRequest request) {
        String code = authService.sendCode(request.getPhone(), request.getScene(), VerificationCodeService.Channel.SMS);
        return ApiResponse.ok(codePayload(code));
    }

    /** 邮箱 + 验证码重置密码 */
    @PostMapping("/reset-password")
    public ApiResponse<Map<String, Object>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getEmail(), request.getCode(), request.getNewPassword());
        return ApiResponse.ok(Map.of("ok", true));
    }

    /**
     * 旧版「忘记密码申请」：给管理员发站内通知，人工重置（前端已改用上面的验证码重置，接口保留）。
     */
    @PostMapping("/forgot-password")
    public ApiResponse<Map<String, Object>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.requestPasswordReset(request.getAccount());
        return ApiResponse.ok(Map.of("ok", true, "message", "已提交申请，管理员会尽快处理"));
    }

    private Map<String, Object> codePayload(String code) {
        Map<String, Object> data = new HashMap<>();
        data.put("sent", true);
        data.put("ttlMinutes", 5);
        if (exposeCode) {
            data.put("devCode", code);
        }
        return data;
    }

    private LoginVO loginResult(Long userId) {
        LoginVO vo = new LoginVO();
        vo.setToken(tokenService.issue(userId));
        vo.setExpiresIn(tokenService.getTtlSeconds());
        vo.setUser(userService.detail(userId, userId));
        return vo;
    }
}
