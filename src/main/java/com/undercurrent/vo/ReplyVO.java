package com.undercurrent.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReplyVO {
    private Long id;
    private Long parentId;
    private UserVO author;
    private String body;
    private Integer likes;
    private Boolean liked;
    private LocalDateTime createdAt;
}
