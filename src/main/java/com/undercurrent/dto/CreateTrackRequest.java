package com.undercurrent.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 上传作品请求（试听模拟：只存元数据，不接收音频文件） */
@Data
public class CreateTrackRequest {

    @NotBlank(message = "作品名不能为空")
    @Size(max = 120, message = "作品名最多 120 字")
    private String title;

    /** 流派名称，如 Boom Bap；不存在则按未分类处理 */
    private String genre;

    @Min(value = 40, message = "BPM 至少 40")
    @Max(value = 220, message = "BPM 最多 220")
    private Integer bpm = 90;

    private String musicalKey;

    @Size(max = 500, message = "简介最多 500 字")
    private String blurb;

    /** 封面文件名，对应前端 /img/xxx */
    private String cover;

    private List<String> tags;
}
