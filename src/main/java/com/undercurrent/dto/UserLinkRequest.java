package com.undercurrent.dto;

import lombok.Data;

/** 添加主页链接的入参 */
@Data
public class UserLinkRequest {
    private String url;
    private String title;
}
