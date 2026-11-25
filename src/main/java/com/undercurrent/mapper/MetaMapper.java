package com.undercurrent.mapper;

import com.undercurrent.domain.Dict;
import com.undercurrent.vo.GenreOptionVO;
import com.undercurrent.vo.MetaVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 字典与筛选项 */
@Mapper
public interface MetaMapper {

    List<String> selectGenres();

    /** 流派 + 分类，按 sort_order（分类序*100+类内序）返回，Service 顺序分组即可 */
    List<GenreOptionVO> selectGenreOptions();

    List<Dict> selectChannels();

    List<String> selectCategories();

    List<MetaVO.TagStatVO> selectTags();

    Integer findGenreIdByName(@Param("name") String name);

    Integer findChannelIdByName(@Param("name") String name);

    Integer findChannelIdBySlug(@Param("slug") String slug);

    Integer findCategoryIdByName(@Param("name") String name);
}
