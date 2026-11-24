package com.undercurrent.controller;

import com.undercurrent.common.ApiResponse;
import com.undercurrent.common.PageResult;
import com.undercurrent.dto.CreateTrackRequest;
import com.undercurrent.service.TrackService;
import com.undercurrent.vo.TrackVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/tracks")
@RequiredArgsConstructor
public class TrackController {

    private final TrackService trackService;

    /**
     * 作品列表
     * GET /api/tracks?genre=Boom Bap&sort=latest|plays|likes&page=1&size=20
     */
    @GetMapping
    public ApiResponse<PageResult<TrackVO>> list(
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ApiResponse.ok(trackService.list(genre, keyword, sort, userId,
                Math.max(1, page), Math.min(100, Math.max(1, size))));
    }

    @GetMapping("/{id}")
    public ApiResponse<TrackVO> detail(@PathVariable Long id,
                                       @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ApiResponse.ok(trackService.detail(id, userId));
    }

    /** 上传作品 */
    @PostMapping
    public ApiResponse<TrackVO> create(@Valid @RequestBody CreateTrackRequest req,
                                       @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ApiResponse.ok(trackService.create(req, userId));
    }

    /** 播放计数 +1 */
    @PostMapping("/{id}/play")
    public ApiResponse<Map<String, Object>> play(@PathVariable Long id) {
        return ApiResponse.ok(trackService.play(id));
    }

    @PostMapping("/{id}/like")
    public ApiResponse<Map<String, Object>> like(@PathVariable Long id,
                                                 @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ApiResponse.ok(trackService.toggleLike(id, userId));
    }
}
