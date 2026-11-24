package com.undercurrent.mapper;

import com.undercurrent.vo.PluginVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PluginMapper {

    List<PluginVO> selectList(@Param("categoryId") Integer categoryId,
                              @Param("keyword") String keyword,
                              @Param("offset") int offset,
                              @Param("size") int size);

    long countList(@Param("categoryId") Integer categoryId, @Param("keyword") String keyword);

    PluginVO selectDetail(@Param("id") Long id);
}
