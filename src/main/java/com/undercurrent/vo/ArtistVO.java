package com.undercurrent.vo;

import lombok.Data;

/**
 * 艺人出参（注册引导第二步的艺人卡）。
 *
 * <p>和 UserProfileVO 区分开：艺人不是站内用户，没有 handle / 关注关系，
 * 头像直接用网易云抓下来的图片路径。
 */
@Data
public class ArtistVO {
    private Long id;
    /** 网易云歌手 id */
    private Long neteaseId;
    private String name;
    private String alias;
    /** 头像相对路径，如 /artists/2116.jpg，前端拼上 API 域名 */
    private String avatar;
    private String bio;
    /** 所属流派分类，如 嘻哈 / 说唱 */
    private String category;
    private Integer musicSize;
    private Integer albumSize;
}
