package com.undercurrent.dto;

import lombok.Data;

import java.util.List;

/**
 * 注册引导提交：喜欢的风格、喜欢的艺人、身份、年龄段、城市。
 * {@code skipped = true} 表示用户直接跳过，只把引导标记为已走过，不保存任何偏好。
 */
@Data
public class OnboardingRequest {
    /** 流派名，取自 /api/onboarding 的 genres */
    private List<String> genres;
    /** 艺人 id，只记录偏好，不产生关注关系（现阶段用户体量还小） */
    private List<Long> artistIds;
    /** 身份：听众 / 制作人 / 音乐人 / 插件作者 */
    private String role;
    /** 年龄段：18 岁以下 / 18-24 / 25-30 / 31-40 / 40 岁以上 */
    private String ageGroup;
    private String city;
    private Boolean skipped;
}
