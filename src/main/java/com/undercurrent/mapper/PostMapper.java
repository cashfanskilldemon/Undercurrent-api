package com.undercurrent.mapper;

import com.undercurrent.domain.Post;
import com.undercurrent.vo.ReplyVO;
import com.undercurrent.vo.PostVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PostMapper {

    /** 列表：支持按频道、关键词、标签过滤；viewerId 用于标记 liked/saved 与关注流 */
    List<PostVO> selectList(@Param("channelId") Integer channelId,
                              @Param("keyword") String keyword,
                              @Param("tag") String tag,
                              @Param("viewerId") Long viewerId,
                              @Param("following") Boolean following,
                              @Param("offset") int offset,
                              @Param("size") int size);

    long countList(@Param("channelId") Integer channelId,
                   @Param("keyword") String keyword,
                   @Param("tag") String tag,
                   @Param("viewerId") Long viewerId,
                   @Param("following") Boolean following);

    PostVO selectDetail(@Param("id") Long id, @Param("viewerId") Long viewerId);

    List<ReplyVO> selectReplies(@Param("threadId") Long threadId, @Param("viewerId") Long viewerId);

    int insert(Post thread);

    int insertTags(@Param("threadId") Long threadId, @Param("tags") List<String> tags);

    /** 点赞数增减，delta 为 +1 / -1 */
    int addLikeCount(@Param("id") Long id, @Param("delta") int delta);

    int addReplyCount(@Param("id") Long id, @Param("delta") int delta);

    int insertLike(@Param("userId") Long userId, @Param("threadId") Long threadId);

    int deleteLike(@Param("userId") Long userId, @Param("threadId") Long threadId);

    int existsLike(@Param("userId") Long userId, @Param("threadId") Long threadId);

    int insertSave(@Param("userId") Long userId, @Param("threadId") Long threadId);

    int deleteSave(@Param("userId") Long userId, @Param("threadId") Long threadId);

    int existsSave(@Param("userId") Long userId, @Param("threadId") Long threadId);

    Integer selectLikesCount(@Param("id") Long id);
}
