package com.undercurrent.domain;

import lombok.Data;

import java.time.LocalDateTime;

/** 作品 */
@Data
public class Track {
    private Long id;
    private Long artistId;
    private String title;
    private Integer genreId;
    private Integer bpm;
    private String musicalKey;
    private Integer durationSec;
    private Integer plays;
    private Integer likesCount;
    private Integer commentsCount;
    private String cover;
    private String blurb;
    /** MySQL JSON 列，直接按字符串读写 */
    private String specJson;
    private LocalDateTime releasedAt;
    private LocalDateTime createdAt;
}
