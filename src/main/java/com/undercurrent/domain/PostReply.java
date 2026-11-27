package com.undercurrent.domain;

import lombok.Data;

import java.time.LocalDateTime;

/** 帖子回复 */
@Data
public class PostReply {
    private Long id;
    private Long postId;
    private Long authorId;
    private Long parentId;
    private String body;
    private Integer likesCount;
    private LocalDateTime createdAt;
}
