package com.undercurrent.domain;

import lombok.Data;

import java.time.LocalDateTime;

/** 讨论帖 */
@Data
public class Thread {
    private Long id;
    private Long authorId;
    private Integer channelId;
    private String body;
    private Integer likesCount;
    private Integer repostsCount;
    private Integer repliesCount;
    private Boolean pinned;
    private String attachmentKind;
    private Long attachmentTrackId;
    private String image;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
