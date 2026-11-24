package com.undercurrent.service;

import com.undercurrent.common.BusinessException;
import com.undercurrent.common.PageResult;
import com.undercurrent.mapper.MetaMapper;
import com.undercurrent.mapper.PluginMapper;
import com.undercurrent.vo.PluginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PluginService {

    private final PluginMapper pluginMapper;
    private final MetaMapper metaMapper;

    public PageResult<PluginVO> list(String category, String keyword, int page, int size) {
        Integer categoryId = null;
        if (category != null && !category.isBlank() && !"全部".equals(category)) {
            categoryId = metaMapper.findCategoryIdByName(category);
            if (categoryId == null) {
                throw new BusinessException(400, "分类不存在: " + category);
            }
        }
        int offset = Math.max(0, (page - 1) * size);
        List<PluginVO> rows = pluginMapper.selectList(categoryId, keyword, offset, size);
        long total = pluginMapper.countList(categoryId, keyword);
        return PageResult.of(rows, total, page, size);
    }

    public PluginVO detail(Long id) {
        PluginVO vo = pluginMapper.selectDetail(id);
        if (vo == null) {
            throw BusinessException.notFound("插件");
        }
        return vo;
    }
}
