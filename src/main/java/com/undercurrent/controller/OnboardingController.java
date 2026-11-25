package com.undercurrent.controller;

import com.undercurrent.common.ApiResponse;
import com.undercurrent.dto.OnboardingRequest;
import com.undercurrent.service.OnboardingService;
import com.undercurrent.vo.OnboardingVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 注册后引导：喜欢风格 / 喜欢的艺人 / 身份 / 年龄段 / 城市 */
@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    /** 引导页数据：可选项 + 已选内容 + needsOnboarding */
    @GetMapping
    public ApiResponse<OnboardingVO> load(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ApiResponse.ok(onboardingService.load(userId));
    }

    /** 提交（或跳过）引导，返回最新的引导状态 */
    @PostMapping
    public ApiResponse<OnboardingVO> submit(
            @RequestBody(required = false) OnboardingRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ApiResponse.ok(onboardingService.submit(userId, request));
    }
}
