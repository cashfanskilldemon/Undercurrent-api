package com.undercurrent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 发帖请求 */
@Data
public class CreateThreadRequest {

    @NotBlank(message = "频道不能为空")
    private String channel;

    @NotBlank(message = "正文不能为空")
    @Size(max = 5000, message = "正文最多 5000 字")
    private String body;

    @Size(max = 5, message = "最多 5 个标签")
    private List<String> tags;

    /** track / plugin，可为空 */
    private String attachmentKind;

    private Long attachmentTrackId;

    /** 配图文件名，对应前端 /img/xxx，可为空 */
    private String image;
}
