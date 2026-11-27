package com.undercurrent.service;

import com.undercurrent.common.BusinessException;
import com.undercurrent.common.PageResult;
import com.undercurrent.domain.Post;
import com.undercurrent.dto.CreatePostRequest;
import com.undercurrent.mapper.MetaMapper;
import com.undercurrent.mapper.PostMapper;
import com.undercurrent.vo.PostVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostMapper postMapper;
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

    public PageResult<PostVO> list(String channel, String keyword, String tag,
                                     Long viewerId, int page, int size) {
        return list(channel, keyword, tag, viewerId, false, page, size);
    }

    /** 关注流：onlyFollowing=true 时只返回自己与已关注作者的帖子 */
    public PageResult<PostVO> list(String channel, String keyword, String tag,
                                     Long viewerId, boolean onlyFollowing, int page, int size) {
        Integer channelId = resolveChannel(channel);
        int offset = Math.max(0, (page - 1) * size);
        List<PostVO> rows = postMapper.selectList(channelId, keyword, tag,
                viewerId, onlyFollowing, offset, size);
        long total = postMapper.countList(channelId, keyword, tag, viewerId, onlyFollowing);
        return PageResult.of(rows, total, page, size);
    }

    public PostVO detail(Long id, Long viewerId) {
        PostVO vo = postMapper.selectDetail(id, viewerId);
        if (vo == null) {
            throw BusinessException.notFound("帖子");
        }
        vo.setReplies(postMapper.selectReplies(id, viewerId));
        return vo;
    }

    @Transactional
    public PostVO create(CreatePostRequest req, Long authorId) {
        if (authorId == null) {
            throw new BusinessException(401, "请先登录");
        }
        Integer channelId = resolveChannel(req.getChannel());
        if (channelId == null) {
            throw new BusinessException(400, "发帖必须指定频道");
        }

        Post t = new Post();
        t.setAuthorId(authorId);
        t.setChannelId(channelId);
        t.setBody(req.getBody());
        t.setPinned(false);
        t.setAttachmentKind(req.getAttachmentKind());
        t.setAttachmentTrackId(req.getAttachmentTrackId());
        t.setImage(req.getImage());
        postMapper.insert(t);

        if (req.getTags() != null && !req.getTags().isEmpty()) {
            postMapper.insertTags(t.getId(), req.getTags());
        }
        return detail(t.getId(), authorId);
    }

    /** 点赞切换，返回最新状态与计数 */
    @Transactional
    public Map<String, Object> toggleLike(Long postId, Long userId) {
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        if (postMapper.selectLikesCount(postId) == null) {
            throw BusinessException.notFound("帖子");
        }
        boolean liked = postMapper.existsLike(userId, postId) > 0;
        if (liked) {
            postMapper.deleteLike(userId, postId);
            postMapper.addLikeCount(postId, -1);
        } else {
            postMapper.insertLike(userId, postId);
            postMapper.addLikeCount(postId, 1);
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("liked", !liked);
        r.put("likes", postMapper.selectLikesCount(postId));
        return r;
    }

    /** 收藏切换 */
    @Transactional
    public Map<String, Object> toggleSave(Long postId, Long userId) {
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        boolean saved = postMapper.existsSave(userId, postId) > 0;
        if (saved) {
            postMapper.deleteSave(userId, postId);
        } else {
            postMapper.insertSave(userId, postId);
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("saved", !saved);
        return r;
    }
}
