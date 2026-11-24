package com.undercurrent.vo;

import com.undercurrent.domain.Dict;
import lombok.Data;

import java.util.List;

/** 前端筛选项一次性下发，避免每页多次请求 */
@Data
public class MetaVO {
    private List<String> genres;
    /** 首项固定为「全部」，前端直接渲染 */
    private List<Dict> channels;
    private List<String> categories;
    private List<TagStatVO> tags;

    @Data
    public static class TagStatVO {
        private String tag;
        private Integer posts;
    }
}
