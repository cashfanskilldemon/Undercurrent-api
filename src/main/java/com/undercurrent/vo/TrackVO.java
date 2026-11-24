package com.undercurrent.vo;

import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 作品 */
@Data
public class TrackVO {
    private Long id;
    private String title;
    private UserVO artist;
    private String genre;
    private Integer bpm;
    private String musicalKey;
    private Integer duration;
    private Integer plays;
    private Integer likes;
    private Integer comments;
    private String cover;
    private String blurb;
    private List<String> tags;
    /** 鼓机序列 / bass / chord，原样透传 JSON */
    @JsonRawValue
    private String spec;
    private Boolean liked;
    private LocalDateTime releasedAt;
}
