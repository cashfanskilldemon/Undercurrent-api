package com.undercurrent.vo;

import lombok.Data;

import java.util.List;

/** 发现页聚合搜索出参：一次请求返回四类结果 */
@Data
public class SearchResultVO {
    private String q;
    private List<UserProfileVO> users;
    private List<TrackVO> tracks;
    private List<PostVO> threads;
    private List<PluginVO> plugins;

    /** 结果总数（Jackson 会序列化为 total 字段） */
    public int getTotal() {
        return size(users) + size(tracks) + size(threads) + size(plugins);
    }

    private static int size(List<?> list) {
        return list == null ? 0 : list.size();
    }
}
