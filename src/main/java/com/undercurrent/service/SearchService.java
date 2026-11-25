package com.undercurrent.service;

import com.undercurrent.vo.SearchResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 发现页聚合搜索：复用各模块 list，按关键词返回四类结果 */
@Service
@RequiredArgsConstructor
public class SearchService {

    private final UserService userService;
    private final TrackService trackService;
    private final PostService postService;
    private final PluginService pluginService;

    public SearchResultVO search(String q, Long viewerId, int limit) {
        int n = Math.min(20, Math.max(1, limit));
        String keyword = (q == null || q.isBlank()) ? null : q.trim();

        SearchResultVO r = new SearchResultVO();
        r.setQ(keyword == null ? "" : keyword);
        r.setUsers(userService.suggested(viewerId, keyword, n));
        r.setTracks(trackService.list(null, keyword, "plays", viewerId, 1, n).getList());
        r.setThreads(postService.list(null, keyword, null, viewerId, 1, n).getList());
        r.setPlugins(pluginService.list(null, keyword, 1, n).getList());
        return r;
    }
}
