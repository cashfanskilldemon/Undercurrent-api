package com.undercurrent.mapper;

import com.undercurrent.vo.UserProfileVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {

    /** 发现页艺人列表：排除自己，可按关键词过滤，按粉丝数降序 */
    List<UserProfileVO> selectSuggested(@Param("viewerId") Long viewerId,
                                        @Param("keyword") String keyword,
                                        @Param("limit") int limit);

    UserProfileVO selectProfile(@Param("id") Long id, @Param("viewerId") Long viewerId);

    UserProfileVO selectByHandle(@Param("handle") String handle, @Param("viewerId") Long viewerId);

    int existsFollow(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);

    int insertFollow(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);

    int deleteFollow(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);

    int addFollowerCount(@Param("id") Long id, @Param("delta") int delta);

    int addFollowingCount(@Param("id") Long id, @Param("delta") int delta);

    /** 被关注者 id 列表，供首页关注流使用 */
    List<Long> selectFolloweeIds(@Param("followerId") Long followerId);
}
