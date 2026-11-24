package com.undercurrent.service;

import com.undercurrent.common.BusinessException;
import com.undercurrent.mapper.NotificationMapper;
import com.undercurrent.vo.NotificationPageVO;
import com.undercurrent.vo.NotificationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;

    public NotificationPageVO list(Long recipientId, int page, int size) {
        if (recipientId == null) {
            throw new BusinessException(401, "请先登录");
        }
        int offset = Math.max(0, (page - 1) * size);
        List<NotificationVO> rows = notificationMapper.selectList(recipientId, offset, size);
        NotificationPageVO vo = new NotificationPageVO();
        vo.setList(rows);
        vo.setTotal(notificationMapper.countList(recipientId));
        vo.setUnreadCount(notificationMapper.countUnread(recipientId));
        return vo;
    }

    @Transactional
    public void markRead(Long recipientId, Long id) {
        if (recipientId == null) {
            throw new BusinessException(401, "请先登录");
        }
        if (notificationMapper.existsOwned(recipientId, id) == 0) {
            throw BusinessException.notFound("通知");
        }
        notificationMapper.markRead(recipientId, id);
    }

    @Transactional
    public int markAllRead(Long recipientId) {
        if (recipientId == null) {
            throw new BusinessException(401, "请先登录");
        }
        return notificationMapper.markAllRead(recipientId);
    }
}
