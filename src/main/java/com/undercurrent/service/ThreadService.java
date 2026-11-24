package com.undercurrent.service;

import com.undercurrent.common.BusinessException;
import com.undercurrent.common.PageResult;
import com.undercurrent.domain.Thread;
import com.undercurrent.dto.CreateThreadRequest;
import com.undercurrent.mapper.MetaMapper;
import com.undercurrent.mapper.ThreadMapper;
import com.undercurrent.vo.ThreadVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ThreadService {

    private final ThreadMapper threadMapper;
    private final MetaMapper metaMapper;

    /** 频道名或 slug 解析成 id；「全部」或空返回 null 表示不过滤 */
    private Integer resolveChannel(String channel) {
        if (channel == null || channel.isBlank() || "全部".equals(channel) || "all".equalsIgnoreCase(channel)) {
            return null;
        }
        Integer id = metaMapper.findChannelIdByName(channel);
        if (id == null) {
            id = metaMapper.findChannelIdBySlug(channel);
        }
        if (id == null) {
            throw new BusinessException(400, "频道不存在: " + channel);
        }
        return id;
    }

    public PageResult<ThreadVO> list(String channel, String keyword, String tag,
                                     Long viewerId, int page, int size) {
        return list(channel, keyword, tag, viewerId, false, page, size);
    }

    /** 关注流：onlyFollowing=true 时只返回自己与已关注作者的帖子 */
    public PageResult<ThreadVO> list(String channel, String keyword, String tag,
                                     Long viewerId, boolean onlyFollowing, int page, int size) {
        Integer channelId = resolveChannel(channel);
        int offset = Math.max(0, (page - 1) * size);
        List<ThreadVO> rows = threadMapper.selectList(channelId, keyword, tag,
                viewerId, onlyFollowing, offset, size);
        long total = threadMapper.countList(channelId, keyword, tag, viewerId, onlyFollowing);
        return PageResult.of(rows, total, page, size);
    }

    public ThreadVO detail(Long id, Long viewerId) {
        ThreadVO vo = threadMapper.selectDetail(id, viewerId);
        if (vo == null) {
            throw BusinessException.notFound("帖子");
        }
        vo.setReplies(threadMapper.selectReplies(id, viewerId));
        return vo;
    }

    @Transactional
    public ThreadVO create(CreateThreadRequest req, Long authorId) {
        if (authorId == null) {
            throw new BusinessException(401, "请先登录");
        }
        Integer channelId = resolveChannel(req.getChannel());
        if (channelId == null) {
            throw new BusinessException(400, "发帖必须指定频道");
        }

        Thread t = new Thread();
        t.setAuthorId(authorId);
        t.setChannelId(channelId);
        t.setBody(req.getBody());
        t.setPinned(false);
        t.setAttachmentKind(req.getAttachmentKind());
        t.setAttachmentTrackId(req.getAttachmentTrackId());
        t.setImage(req.getImage());
        threadMapper.insert(t);

        if (req.getTags() != null && !req.getTags().isEmpty()) {
            threadMapper.insertTags(t.getId(), req.getTags());
        }
        return detail(t.getId(), authorId);
    }

    /** 点赞切换，返回最新状态与计数 */
    @Transactional
    public Map<String, Object> toggleLike(Long threadId, Long userId) {
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        if (threadMapper.selectLikesCount(threadId) == null) {
            throw BusinessException.notFound("帖子");
        }
        boolean liked = threadMapper.existsLike(userId, threadId) > 0;
        if (liked) {
            threadMapper.deleteLike(userId, threadId);
            threadMapper.addLikeCount(threadId, -1);
        } else {
            threadMapper.insertLike(userId, threadId);
            threadMapper.addLikeCount(threadId, 1);
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("liked", !liked);
        r.put("likes", threadMapper.selectLikesCount(threadId));
        return r;
    }

    /** 收藏切换 */
    @Transactional
    public Map<String, Object> toggleSave(Long threadId, Long userId) {
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        boolean saved = threadMapper.existsSave(userId, threadId) > 0;
        if (saved) {
            threadMapper.deleteSave(userId, threadId);
        } else {
            threadMapper.insertSave(userId, threadId);
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("saved", !saved);
        return r;
    }
}
