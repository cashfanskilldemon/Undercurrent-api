package com.undercurrent.mapper;

import com.undercurrent.domain.Track;
import com.undercurrent.vo.TrackVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TrackMapper {

    /** sort: latest(默认) / plays / likes */
    List<TrackVO> selectList(@Param("genreId") Integer genreId,
                             @Param("keyword") String keyword,
                             @Param("sort") String sort,
                             @Param("viewerId") Long viewerId,
                             @Param("offset") int offset,
                             @Param("size") int size);

    long countList(@Param("genreId") Integer genreId, @Param("keyword") String keyword);

    TrackVO selectDetail(@Param("id") Long id, @Param("viewerId") Long viewerId);

    int addPlays(@Param("id") Long id);

    int addLikeCount(@Param("id") Long id, @Param("delta") int delta);

    int insertLike(@Param("userId") Long userId, @Param("trackId") Long trackId);

    int deleteLike(@Param("userId") Long userId, @Param("trackId") Long trackId);

    int existsLike(@Param("userId") Long userId, @Param("trackId") Long trackId);

    Integer selectLikesCount(@Param("id") Long id);

    /** 上传作品 */
    int insert(Track track);

    int insertTags(@Param("trackId") Long trackId, @Param("tags") List<String> tags);
}
