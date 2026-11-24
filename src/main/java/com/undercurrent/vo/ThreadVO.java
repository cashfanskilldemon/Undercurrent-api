package com.undercurrent.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 讨论列表 / 详情 */
@Data
public class ThreadVO {

    private Long id;
    private UserVO author;
    private String channel;
    private String body;
    private List<String> tags;
    private Integer likes;
    private Integer reposts;
    private Integer repliesCount;
    private Boolean pinned;
    private Attachment attachment;
    /** 配图文件名，对应前端 /img/xxx */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String image;
    private LocalDateTime createdAt;
    /** 当前登录用户是否点过赞 / 收藏，未登录恒为 false */
    private Boolean liked;
    private Boolean saved;
    /** 详情接口才带，列表为空 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ReplyVO> replies;

    @Data
    public static class Attachment {
        private String kind;
        private Long trackId;
        private TrackBrief track;
    }

    @Data
    public static class TrackBrief {
        private Long id;
        private String title;
        private String cover;
        private Integer durationSec;
        private Integer bpm;
    }
}
