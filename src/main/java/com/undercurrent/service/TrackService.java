package com.undercurrent.service;

import com.undercurrent.common.BusinessException;
import com.undercurrent.common.PageResult;
import com.undercurrent.domain.Track;
import com.undercurrent.dto.CreateTrackRequest;
import com.undercurrent.mapper.MetaMapper;
import com.undercurrent.mapper.TrackMapper;
import com.undercurrent.vo.TrackVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TrackService {

    private final TrackMapper trackMapper;
    private final MetaMapper metaMapper;

    public PageResult<TrackVO> list(String genre, String keyword, String sort,
                                    Long viewerId, int page, int size) {
        Integer genreId = null;
        if (genre != null && !genre.isBlank() && !"全部".equals(genre)) {
            genreId = metaMapper.findGenreIdByName(genre);
            if (genreId == null) {
                throw new BusinessException(400, "流派不存在: " + genre);
            }
        }
        int offset = Math.max(0, (page - 1) * size);
        List<TrackVO> rows = trackMapper.selectList(genreId, keyword, sort, viewerId, offset, size);
        long total = trackMapper.countList(genreId, keyword);
        return PageResult.of(rows, total, page, size);
    }

    public TrackVO detail(Long id, Long viewerId) {
        TrackVO vo = trackMapper.selectDetail(id, viewerId);
        if (vo == null) {
            throw BusinessException.notFound("作品");
        }
        return vo;
    }

    /** 上传作品（试听模拟：只存元数据，released_at=NOW，无音频文件） */
    @Transactional
    public TrackVO create(CreateTrackRequest req, Long artistId) {
        if (artistId == null) {
            throw new BusinessException(401, "请先登录");
        }
        Integer genreId = null;
        if (req.getGenre() != null && !req.getGenre().isBlank()
                && !"全部".equals(req.getGenre())) {
            genreId = metaMapper.findGenreIdByName(req.getGenre());
        }
        Track t = new Track();
        t.setArtistId(artistId);
        t.setTitle(req.getTitle());
        t.setGenreId(genreId);
        t.setBpm(req.getBpm() == null ? 90 : req.getBpm());
        t.setMusicalKey(req.getMusicalKey());
        t.setDurationSec(180);
        t.setCover(req.getCover());
        t.setBlurb(req.getBlurb());
        t.setReleasedAt(java.time.LocalDateTime.now());
        trackMapper.insert(t);

        if (req.getTags() != null && !req.getTags().isEmpty()) {
            trackMapper.insertTags(t.getId(), req.getTags());
        }
        return detail(t.getId(), artistId);
    }

    /** 播放计数 +1，返回最新播放量 */
    @Transactional
    public Map<String, Object> play(Long id) {
        Integer likes = trackMapper.selectLikesCount(id);
        if (likes == null) {
            throw BusinessException.notFound("作品");
        }
        trackMapper.addPlays(id);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("ok", true);
        return r;
    }

    @Transactional
    public Map<String, Object> toggleLike(Long trackId, Long userId) {
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        if (trackMapper.selectLikesCount(trackId) == null) {
            throw BusinessException.notFound("作品");
        }
        boolean liked = trackMapper.existsLike(userId, trackId) > 0;
        if (liked) {
            trackMapper.deleteLike(userId, trackId);
            trackMapper.addLikeCount(trackId, -1);
        } else {
            trackMapper.insertLike(userId, trackId);
            trackMapper.addLikeCount(trackId, 1);
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("liked", !liked);
        r.put("likes", trackMapper.selectLikesCount(trackId));
        return r;
    }
}
