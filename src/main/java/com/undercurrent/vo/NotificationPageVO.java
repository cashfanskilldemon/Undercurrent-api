package com.undercurrent.vo;

import lombok.Data;

import java.util.List;

/** 通知列表出参：除了分页内容，直接带未读数供左导航角标使用 */
@Data
public class NotificationPageVO {
    private List<NotificationVO> list;
    private long total;
    private int unreadCount;
}
