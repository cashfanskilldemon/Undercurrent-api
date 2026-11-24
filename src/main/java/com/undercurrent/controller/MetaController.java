package com.undercurrent.controller;

import com.undercurrent.common.ApiResponse;
import com.undercurrent.service.MetaService;
import com.undercurrent.vo.MetaVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/meta")
@RequiredArgsConstructor
public class MetaController {

    private final MetaService metaService;

    /** 前端启动时拉一次：流派 / 频道 / 插件分类 / 热门标签 */
    @GetMapping
    public ApiResponse<MetaVO> meta() {
        return ApiResponse.ok(metaService.load());
    }
}
