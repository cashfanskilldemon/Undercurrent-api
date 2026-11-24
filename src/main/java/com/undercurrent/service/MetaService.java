package com.undercurrent.service;

import com.undercurrent.domain.Dict;
import com.undercurrent.mapper.MetaMapper;
import com.undercurrent.vo.MetaVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MetaService {

    private final MetaMapper metaMapper;

    /** 一次返回页面所有筛选项，前端只需启动时拉一次 */
    public MetaVO load() {
        MetaVO vo = new MetaVO();
        vo.setGenres(metaMapper.selectGenres());
        vo.setCategories(metaMapper.selectCategories());
        vo.setTags(metaMapper.selectTags());

        // 「全部」是前端虚拟频道，这里补在首位
        List<Dict> channels = new ArrayList<>();
        Dict all = new Dict();
        all.setId(0);
        all.setName("全部");
        all.setSlug("all");
        all.setSortOrder(0);
        channels.add(all);
        channels.addAll(metaMapper.selectChannels());
        vo.setChannels(channels);
        return vo;
    }
}
