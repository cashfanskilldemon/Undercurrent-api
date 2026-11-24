package com.undercurrent.mapper;

import com.undercurrent.domain.Notification;
import com.undercurrent.vo.NotificationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NotificationMapper {

    List<NotificationVO> selectList(@Param("recipientId") Long recipientId,
                                    @Param("offset") int offset,
                                    @Param("size") int size);

    long countList(@Param("recipientId") Long recipientId);

    int countUnread(@Param("recipientId") Long recipientId);

    int markRead(@Param("recipientId") Long recipientId, @Param("id") Long id);

    int markAllRead(@Param("recipientId") Long recipientId);

    int existsOwned(@Param("recipientId") Long recipientId, @Param("id") Long id);

    /** 站内通知写入（忘记密码申请等） */
    int insert(@Param("recipientId") Long recipientId,
               @Param("actorId") Long actorId,
               @Param("type") String type,
               @Param("body") String body,
               @Param("href") String href);
}
