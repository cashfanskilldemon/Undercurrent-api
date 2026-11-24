package com.undercurrent.domain;

import lombok.Data;

import java.time.LocalDateTime;

/** 站内通知（演示数据：like / reply / follow / system） */
@Data
public class Notification {
    private Long id;
    private Long recipientId;
    private Long actorId;
    private String type;
    private String body;
    private String href;
    private Boolean read;
    private LocalDateTime createdAt;
}
