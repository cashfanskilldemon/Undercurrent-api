package com.undercurrent.controller;

import com.undercurrent.common.ApiResponse;
import com.undercurrent.service.SearchService;
import com.undercurrent.vo.SearchResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    /** q 为空时返回各模块的默认推荐内容，供发现页初始展示 */
    @GetMapping
    public ApiResponse<SearchResultVO> search(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "8") int limit,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ApiResponse.ok(searchService.search(q, userId, limit));
    }
}
