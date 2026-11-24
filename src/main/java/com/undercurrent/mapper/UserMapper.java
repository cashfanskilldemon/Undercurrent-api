package com.undercurrent.mapper;

import com.undercurrent.domain.User;
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

    /** 登录用：按 handle 取完整用户（含 password_hash / status），大小写不敏感 */
    User selectAuthByHandle(@Param("handle") String handle);

    /** 登录成功后把旧格式密码重算成新算法 */
    int updatePasswordHash(@Param("id") Long id, @Param("passwordHash") String passwordHash);

    /** 注册：插入新用户，回填自增 id */
    int insertUser(User user);

    /** 所有管理员 id，忘记密码申请要发给他们 */
    java.util.List<Long> selectAdminIds();

    int existsFollow(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);

    int insertFollow(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);

    int deleteFollow(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);

    int addFollowerCount(@Param("id") Long id, @Param("delta") int delta);

    int addFollowingCount(@Param("id") Long id, @Param("delta") int delta);

    /** 被关注者 id 列表，供首页关注流使用 */
    List<Long> selectFolloweeIds(@Param("followerId") Long followerId);
}
