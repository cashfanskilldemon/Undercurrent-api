package com.undercurrent.controller;

import com.undercurrent.common.ApiResponse;
import com.undercurrent.common.PageResult;
import com.undercurrent.dto.CreateThreadRequest;
import com.undercurrent.service.ThreadService;
import com.undercurrent.vo.ThreadVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/threads")
@RequiredArgsConstructor
public class ThreadController {

    private final ThreadService threadService;

    /**
     * 讨论列表
     * GET /api/threads?channel=作品反馈&keyword=&tag=&page=1&size=20
     * 「全部」或省略 channel 表示不按频道过滤
     */
    @GetMapping
    public ApiResponse<PageResult<ThreadVO>> list(
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false, defaultValue = "false") boolean following,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ApiResponse.ok(threadService.list(channel, keyword, tag, userId,
                following, normPage(page), normSize(size)));
    }

    @GetMapping("/{id}")
    public ApiResponse<ThreadVO> detail(@PathVariable Long id,
                                        @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ApiResponse.ok(threadService.detail(id, userId));
    }

    @PostMapping
    public ApiResponse<ThreadVO> create(@Valid @RequestBody CreateThreadRequest req,
                                        @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ApiResponse.ok(threadService.create(req, userId));
    }

    /** 点赞 / 取消点赞 */
    @PostMapping("/{id}/like")
    public ApiResponse<Map<String, Object>> like(@PathVariable Long id,
                                                 @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ApiResponse.ok(threadService.toggleLike(id, userId));
    }

    /** 收藏 / 取消收藏 */
    @PostMapping("/{id}/save")
    public ApiResponse<Map<String, Object>> save(@PathVariable Long id,
                                                 @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ApiResponse.ok(threadService.toggleSave(id, userId));
    }

    private int normPage(int page) { return Math.max(1, page); }

    private int normSize(int size) { return Math.min(100, Math.max(1, size)); }
}
