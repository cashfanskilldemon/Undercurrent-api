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

    /** 登录 / 找回密码用：账号（handle，大小写不敏感）或手机号都能查到 */
    User selectAuthByAccount(@Param("account") String account);

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

    /** 引导里选中的艺人是否都还存在（防止提交脏 id） */
    int countExistingUsers(@Param("ids") List<Long> ids);

    /** 引导状态：role / city / age_group / gender / onboarded_at */
    User selectOnboardingState(@Param("id") Long id);

    /** 某个用户某类偏好（kind = genre / artist） */
    List<String> selectInterestRefs(@Param("userId") Long userId, @Param("kind") String kind);

    int deleteInterests(@Param("userId") Long userId);

    int insertInterest(@Param("userId") Long userId,
                       @Param("kind") String kind,
                       @Param("ref") String ref);

    /** 写回身份/城市/年龄/性别，并标记引导已走过 */
    int updateOnboardingProfile(@Param("id") Long id,
                                @Param("role") String role,
                                @Param("city") String city,
                                @Param("ageGroup") String ageGroup,
                                @Param("gender") String gender);

    /** 个人主页编辑资料：昵称、简介、城市 */
    int updateProfile(@Param("id") Long id,
                      @Param("nickname") String nickname,
                      @Param("bio") String bio,
                      @Param("city") String city);

    /** 设置 / 清除头像路径（null 表示移除） */
    int updateAvatar(@Param("id") Long id, @Param("avatar") String avatar);

    /** 指定时间点之后的改名次数（14 天内最多 2 次） */
    int countNameChangesSince(@Param("id") Long id, @Param("since") java.time.LocalDateTime since);

    /** 记录一次改名 */
    int insertNameChange(@Param("id") Long id);
}
