package com.undercurrent.controller;

import com.undercurrent.common.ApiResponse;
import com.undercurrent.dto.UpdateProfileRequest;
import com.undercurrent.dto.UserLinkRequest;
import com.undercurrent.service.UserService;
import com.undercurrent.vo.UserLinkVO;
import com.undercurrent.vo.UserProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    /** 上传头像（JSON base64，仅本人） */
    @PostMapping("/{id}/avatar")
    public ApiResponse<UserProfileVO> uploadAvatar(
            @PathVariable Long id,
            @RequestBody com.undercurrent.dto.AvatarUploadRequest req,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ApiResponse.ok(userService.uploadAvatar(id, req, userId));
    }

    /** 移除头像，回退到色块占位 */
    @DeleteMapping("/{id}/avatar")
    public ApiResponse<UserProfileVO> removeAvatar(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ApiResponse.ok(userService.removeAvatar(id, userId));
    }

    /** 编辑自己的资料：昵称 / 简介 / 城市（仅本人） */
    @PutMapping("/{id}")
    public ApiResponse<UserProfileVO> update(
            @PathVariable Long id,
            @RequestBody UpdateProfileRequest req,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ApiResponse.ok(userService.updateProfile(id, req, userId));
    }

    /** 个人主页链接：本人可查看 / 添加 / 删除 */
    @GetMapping("/{id}/links")
    public ApiResponse<List<UserLinkVO>> links(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ApiResponse.ok(userService.listLinks(id, userId));
    }

    @PostMapping("/{id}/links")
    public ApiResponse<UserLinkVO> addLink(
            @PathVariable Long id,
            @RequestBody UserLinkRequest req,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ApiResponse.ok(userService.addLink(id, req, userId));
    }

    @DeleteMapping("/{id}/links/{linkId}")
    public ApiResponse<Map<String, Object>> removeLink(
            @PathVariable Long id,
            @PathVariable Long linkId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        userService.removeLink(id, linkId, userId);
        return ApiResponse.ok(Map.of());
    }

    @PostMapping("/{id}/follow")
    public ApiResponse<Map<String, Object>> follow(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ApiResponse.ok(userService.toggleFollow(id, userId));
    }
}
