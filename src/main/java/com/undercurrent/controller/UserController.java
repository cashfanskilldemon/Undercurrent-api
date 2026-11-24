package com.undercurrent.controller;

import com.undercurrent.common.ApiResponse;
import com.undercurrent.service.UserService;
import com.undercurrent.vo.UserProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 发现页推荐关注 / 艺人搜索 */
    @GetMapping("/suggested")
    public ApiResponse<List<UserProfileVO>> suggested(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "12") int limit,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ApiResponse.ok(userService.suggested(userId, q, limit));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserProfileVO> detail(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ApiResponse.ok(userService.detail(id, userId));
    }

    /** 由 handle 查资料，对应前端 /u/:handle */
    @GetMapping("/by-handle/{handle}")
    public ApiResponse<UserProfileVO> byHandle(
            @PathVariable String handle,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ApiResponse.ok(userService.byHandle(handle, userId));
    }

    @PostMapping("/{id}/follow")
    public ApiResponse<Map<String, Object>> follow(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ApiResponse.ok(userService.toggleFollow(id, userId));
    }
}
