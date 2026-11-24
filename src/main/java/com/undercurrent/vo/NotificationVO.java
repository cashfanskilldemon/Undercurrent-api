package com.undercurrent.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 通知出参：actor 内联触发者用户信息，前端据此渲染头像和类型图标 */
@Data
public class NotificationVO {
    private Long id;
    private String type;
    private UserVO actor;
    private String body;
    private String href;
    private Boolean read;
    private LocalDateTime createdAt;
}
