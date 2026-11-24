package com.undercurrent.domain;

import lombok.Data;

import java.time.LocalDateTime;

/** 用户：音乐人 / 制作人 / 插件作者 / 听众统一一张表，靠 role 区分 */
@Data
public class User {
    private Long id;
    private String handle;
    private String nickname;
    private Integer avatarHue;
    private String city;
    private String role;
    private String bio;
    private Integer followersCount;
    private Integer followingCount;
    private Boolean verified;
    /** PBKDF2 哈希，格式 pbkdf2$迭代$盐$哈希；不参与任何出参序列化 */
    private String passwordHash;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
