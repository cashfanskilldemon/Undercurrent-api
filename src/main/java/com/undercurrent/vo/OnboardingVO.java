package com.undercurrent.vo;

import lombok.Data;

import java.util.List;

/**
 * 注册引导页一次拿全：可选项 + 当前已选内容 + 是否还需要引导。
 * 前端刷新页面也能靠它恢复进度（已选中的风格/艺人会回显）。
 */
@Data
public class OnboardingVO {
    /** true 表示还没走过引导（新注册用户） */
    private Boolean needsOnboarding;
    /** 可选的流派（扁平列表，保留给窄屏/旧客户端） */
    private List<String> genres;
    /** 可选的流派按分类分组（宽屏泡泡云用） */
    private List<GenreGroupVO> genreGroups;
    /** 可选的艺人（按粉丝数排序的推荐列表） */
    private List<UserProfileVO> artists;
    private List<String> roles;
    private List<String> ageGroups;
    private List<String> cities;

    /** 已选的流派 / 艺人 id */
    private List<String> selectedGenres;
    private List<Long> selectedArtistIds;
    /** 当前资料里的身份 / 年龄段 / 城市 */
    private String currentRole;
    private String currentAgeGroup;
    private String currentCity;

    /** 分类 + 该分类下的流派名 */
    @Data
    public static class GenreGroupVO {
        private String category;
        private List<String> genres;
    }
}
