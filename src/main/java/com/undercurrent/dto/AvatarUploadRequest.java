package com.undercurrent.dto;

import lombok.Data;

/** 头像上传：base64 编码的图片内容（前端 FileReader 读出，去掉 data: 前缀） */
@Data
public class AvatarUploadRequest {
    private String data;
}
