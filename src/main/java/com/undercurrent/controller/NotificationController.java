package com.undercurrent.controller;

import com.undercurrent.common.ApiResponse;
import com.undercurrent.service.NotificationService;
import com.undercurrent.vo.NotificationPageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<NotificationPageVO> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        int p = Math.max(1, page);
        int s = Math.min(100, Math.max(1, size));
        return ApiResponse.ok(notificationService.list(userId, p, s));
    }

    @PostMapping("/{id}/read")
    public ApiResponse<Map<String, Object>> read(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        notificationService.markRead(userId, id);
        return ApiResponse.ok(Map.of("ok", true));
    }

    @PostMapping("/read-all")
    public ApiResponse<Map<String, Object>> readAll(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        int changed = notificationService.markAllRead(userId);
        return ApiResponse.ok(Map.of("ok", true, "changed", changed));
    }
}
