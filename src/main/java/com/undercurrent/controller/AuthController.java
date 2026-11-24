package com.undercurrent.controller;

import com.undercurrent.common.ApiResponse;
import com.undercurrent.common.TokenService;
import com.undercurrent.dto.LoginRequest;
import com.undercurrent.service.AuthService;
import com.undercurrent.service.UserService;
import com.undercurrent.vo.LoginVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

        LoginVO vo = new LoginVO();
        vo.setToken(tokenService.issue(userId));
        vo.setExpiresIn(tokenService.getTtlSeconds());
        vo.setUser(userService.detail(userId, userId));
        return ApiResponse.ok(vo);
    }
}
