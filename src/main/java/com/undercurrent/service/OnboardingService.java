package com.undercurrent.service;

import com.undercurrent.common.BusinessException;
import com.undercurrent.domain.User;
import com.undercurrent.dto.OnboardingRequest;
import com.undercurrent.mapper.MetaMapper;
import com.undercurrent.mapper.UserMapper;
import com.undercurrent.vo.GenreOptionVO;
import com.undercurrent.vo.OnboardingVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 注册后引导：喜欢的风格 → 喜欢的艺人 → 身份 → 年龄段 → 城市。
 *
 * <p>偏好写在 user_interest（kind = genre / artist），身份、城市、年龄段写回 app_user，
 * 最后置 onboarded_at 表示走过了（跳过同样置位，避免每次登录都被拦）。
 * 现阶段用户体量小，选中的艺人不自动关注，只作为推荐依据。
 */
@Service
@RequiredArgsConstructor
public class OnboardingService {

    private static final String KIND_GENRE = "genre";
    private static final String KIND_ARTIST = "artist";
    private static final List<String> ROLES = List.of("听众", "制作人", "音乐人", "插件作者");
    private static final List<String> AGE_GROUPS =
            List.of("18 岁以下", "18-24", "25-30", "31-40", "40 岁以上");
    private static final List<String> CITIES = List.of(
            "北京", "上海", "广州", "深圳", "成都", "重庆",
            "杭州", "武汉", "西安", "长沙", "乌鲁木齐", "其他");
    /** 防御性上限：就算前端被改也不至于一次写几百条 */
    private static final int MAX_GENRES = 12;
    private static final int MAX_ARTISTS = 30;

    private final UserMapper userMapper;
    private final MetaMapper metaMapper;

    /** 引导页所需数据：可选项 + 已选内容 + 是否还需要引导 */
    public OnboardingVO load(Long userId) {
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        User state = userMapper.selectOnboardingState(userId);
        if (state == null) {
            throw BusinessException.notFound("用户");
        }

        OnboardingVO vo = new OnboardingVO();
        vo.setNeedsOnboarding(state.getOnboardedAt() == null);
        vo.setGenres(metaMapper.selectGenres());
        vo.setGenreGroups(groupGenres());
        vo.setArtists(userMapper.selectSuggested(userId, null, 12));
        vo.setRoles(ROLES);
        vo.setAgeGroups(AGE_GROUPS);
        vo.setCities(CITIES);

        vo.setSelectedGenres(userMapper.selectInterestRefs(userId, KIND_GENRE));
        List<Long> artistIds = new ArrayList<>();
        for (String ref : userMapper.selectInterestRefs(userId, KIND_ARTIST)) {
            try {
                artistIds.add(Long.valueOf(ref));
            } catch (NumberFormatException ignored) {
                // 历史脏数据直接忽略，不影响页面渲染
            }
        }
        vo.setSelectedArtistIds(artistIds);
        vo.setCurrentRole(state.getRole());
        vo.setCurrentAgeGroup(state.getAgeGroup());
        vo.setCurrentCity(state.getCity());
        return vo;
    }

    /** 提交引导（完成或跳过）；重复提交会覆盖上一次的选择 */
    @Transactional
    public OnboardingVO submit(Long userId, OnboardingRequest request) {
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        boolean skipped = request == null || Boolean.TRUE.equals(request.getSkipped());
        if (skipped) {
            userMapper.updateOnboardingProfile(userId, null, null, null);
            return load(userId);
        }

        List<String> genres = sanitizeGenres(request.getGenres());
        List<Long> artistIds = sanitizeArtists(request.getArtistIds());
        if (!artistIds.isEmpty() && userMapper.countExistingUsers(artistIds) != artistIds.size()) {
            throw new BusinessException(400, "有艺人已下线，请刷新后重试");
        }

        String role = pick(request.getRole(), ROLES);
        String ageGroup = pick(request.getAgeGroup(), AGE_GROUPS);
        String city = pick(request.getCity(), CITIES);

        // 覆盖式保存：先清空再写入，重复提交不会累积
        userMapper.deleteInterests(userId);
        for (String genre : genres) {
            userMapper.insertInterest(userId, KIND_GENRE, genre);
        }
        for (Long artistId : artistIds) {
            userMapper.insertInterest(userId, KIND_ARTIST, String.valueOf(artistId));
        }
        userMapper.updateOnboardingProfile(userId, role, city, ageGroup);
        return load(userId);
    }

    /** 按字典里的 sort_order 顺序切分组，分类名第一次出现即定序 */
    private List<OnboardingVO.GenreGroupVO> groupGenres() {
        Map<String, OnboardingVO.GenreGroupVO> byCategory = new LinkedHashMap<>();
        for (GenreOptionVO option : metaMapper.selectGenreOptions()) {
            String raw = option.getCategory();
            String category = (raw == null || raw.isBlank()) ? "其他" : raw;
            OnboardingVO.GenreGroupVO group = byCategory.get(category);
            if (group == null) {
                group = new OnboardingVO.GenreGroupVO();
                group.setCategory(category);
                group.setGenres(new ArrayList<>());
                byCategory.put(category, group);
            }
            group.getGenres().add(option.getName());
        }
        return new ArrayList<>(byCategory.values());
    }

    /** 只接受字典里真实存在的流派，去重并截断 */
    private List<String> sanitizeGenres(List<String> input) {
        if (input == null || input.isEmpty()) {
            return List.of();
        }
        Set<String> allowed = new LinkedHashSet<>(metaMapper.selectGenres());
        Set<String> picked = new LinkedHashSet<>();
        for (String name : input) {
            if (name == null || name.isBlank()) {
                continue;
            }
            String trimmed = name.trim();
            if (allowed.contains(trimmed)) {
                picked.add(trimmed);
            }
            if (picked.size() >= MAX_GENRES) {
                break;
            }
        }
        return new ArrayList<>(picked);
    }

    private List<Long> sanitizeArtists(List<Long> input) {
        if (input == null || input.isEmpty()) {
            return List.of();
        }
        Set<Long> picked = new LinkedHashSet<>();
        for (Long id : input) {
            if (id != null && id > 0) {
                picked.add(id);
            }
            if (picked.size() >= MAX_ARTISTS) {
                break;
            }
        }
        return new ArrayList<>(picked);
    }

    /** 白名单取值：不在选项里的一律当作没选，避免脏数据进库 */
    private String pick(String value, List<String> allowed) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return allowed.contains(trimmed) ? trimmed : null;
    }
}
