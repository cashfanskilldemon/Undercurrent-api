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
}
