package com.undercurrent.controller;

import com.undercurrent.common.ApiResponse;
import com.undercurrent.common.TokenService;
import com.undercurrent.dto.ForgotPasswordRequest;
import com.undercurrent.dto.LoginRequest;
import com.undercurrent.dto.RegisterRequest;
import com.undercurrent.service.AuthService;
import com.undercurrent.service.UserService;
import com.undercurrent.vo.LoginVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final TokenService tokenService;

    /**
     * 账号密码登录：成功返回签名 token + 用户资料。
     * 前端后续请求带 {@code Authorization: Bearer <token>}，不再依赖 X-User-Id。
     */
    @PostMapping("/login")
    public ApiResponse<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        Long userId = authService.authenticate(request.getAccount(), request.getPassword());
        return ApiResponse.ok(loginResult(userId));
    }

    /** 注册：成功后直接返回 token，省一次登录 */
    @PostMapping("/register")
    public ApiResponse<LoginVO> register(@Valid @RequestBody RegisterRequest request) {
        Long userId = authService.register(request.getAccount(), request.getPassword(), request.getNickname());
        return ApiResponse.ok(loginResult(userId));
    }

    /**
     * 忘记密码申请：不返回账号是否存在，统一提示已提交，
     * 实际处理方式是给管理员发一条站内通知，由人工重置。
     */
    @PostMapping("/forgot-password")
    public ApiResponse<Map<String, Object>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.requestPasswordReset(request.getAccount());
        return ApiResponse.ok(Map.of("ok", true, "message", "已提交申请，管理员会尽快处理"));
    }

    private LoginVO loginResult(Long userId) {
        LoginVO vo = new LoginVO();
        vo.setToken(tokenService.issue(userId));
        vo.setExpiresIn(tokenService.getTtlSeconds());
        vo.setUser(userService.detail(userId, userId));
        return vo;
    }
}
