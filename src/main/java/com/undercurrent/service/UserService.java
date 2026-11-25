package com.undercurrent.service;

import com.undercurrent.common.BusinessException;
import com.undercurrent.domain.User;
import com.undercurrent.mapper.UserMapper;
import com.undercurrent.vo.UserProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    public List<UserProfileVO> suggested(Long viewerId, String keyword, int limit) {
        return userMapper.selectSuggested(viewerId, keyword, Math.min(50, Math.max(1, limit)));
    }

    public UserProfileVO detail(Long id, Long viewerId) {
        UserProfileVO vo = userMapper.selectProfile(id, viewerId);
        if (vo == null) {
            throw BusinessException.notFound("用户");
        }
        return vo;
    }

    public UserProfileVO byHandle(String handle, Long viewerId) {
        UserProfileVO vo = userMapper.selectByHandle(handle, viewerId);
        if (vo == null) {
            throw BusinessException.notFound("用户");
        }
        return vo;
    }

    /** 关注切换，返回最新关注态与粉丝数 */
    @Transactional
    public Map<String, Object> toggleFollow(Long targetId, Long viewerId) {
        if (viewerId == null) {
            throw new BusinessException(401, "请先登录");
        }
        if (targetId == null || userMapper.selectProfile(targetId, viewerId) == null) {
            throw BusinessException.notFound("用户");
        }
        if (targetId.equals(viewerId)) {
            throw new BusinessException(400, "不能关注自己");
        }
        boolean following = userMapper.existsFollow(viewerId, targetId) > 0;
        if (following) {
            userMapper.deleteFollow(viewerId, targetId);
            userMapper.addFollowerCount(targetId, -1);
            userMapper.addFollowingCount(viewerId, -1);
        } else {
            userMapper.insertFollow(viewerId, targetId);
            userMapper.addFollowerCount(targetId, 1);
            userMapper.addFollowingCount(viewerId, 1);
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("followed", !following);
        UserProfileVO fresh = userMapper.selectProfile(targetId, viewerId);
        r.put("followers", fresh != null ? fresh.getFollowers() : 0);
        return r;
    }

    public List<Long> followeeIds(Long viewerId) {
        if (viewerId == null) {
            return List.of();
        }
        return userMapper.selectFolloweeIds(viewerId);
    }

    /** 新注册用户 / 没走过引导的老用户返回 true，前端据此先跳 /onboarding */
    public boolean needsOnboarding(Long userId) {
        if (userId == null) {
            return false;
        }
        User user = userMapper.selectOnboardingState(userId);
        return user == null || user.getOnboardedAt() == null;
    }
}
