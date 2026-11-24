package com.undercurrent.controller;

import com.undercurrent.common.ApiResponse;
import com.undercurrent.common.PageResult;
import com.undercurrent.service.PluginService;
import com.undercurrent.vo.PluginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/plugins")
@RequiredArgsConstructor
public class PluginController {

    private final PluginService pluginService;

    /** GET /api/plugins?category=效果器&keyword=&page=1&size=20 */
    @GetMapping
    public ApiResponse<PageResult<PluginVO>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(pluginService.list(category, keyword,
                Math.max(1, page), Math.min(100, Math.max(1, size))));
    }

    @GetMapping("/{id}")
    public ApiResponse<PluginVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(pluginService.detail(id));
    }
}
