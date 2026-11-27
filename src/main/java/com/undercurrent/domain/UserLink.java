package com.undercurrent.domain;

import lombok.Data;

import java.time.LocalDateTime;

/** 个人主页外部链接 */
@Data
public class UserLink {
    private Long id;
    private Long userId;
    private String url;
    private String title;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
