package com.undercurrent.controller;

import com.undercurrent.common.ApiResponse;
import com.undercurrent.dto.LoginRequest;
import com.undercurrent.service.AuthService;
import com.undercurrent.service.UserService;
import com.undercurrent.vo.UserProfileVO;
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

    /** 账号密码登录，成功后返回用户资料；前端拿 id 放进 X-User-Id 头继续后续请求 */
    @PostMapping("/login")
    public ApiResponse<UserProfileVO> login(@Valid @RequestBody LoginRequest request) {
        Long userId = authService.authenticate(request.getAccount(), request.getPassword());
        return ApiResponse.ok(userService.detail(userId, userId));
    }
}
