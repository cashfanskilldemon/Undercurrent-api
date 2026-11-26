package com.undercurrent.mapper;

import com.undercurrent.vo.ArtistVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 艺人库（网易云抓取的流派代表艺人） */
@Mapper
public interface ArtistMapper {

    /**
     * 引导页艺人卡：每个流派各取前 perCategory 位（sort_order 升序），
     * 一位艺人属于多个流派时会出现多行，调用方按 id 去重。
     */
    List<ArtistVO> selectForOnboarding(@Param("perCategory") int perCategory);

    /** 校验提交上来的艺人 id 是否都还在库里 */
    int countExisting(@Param("ids") List<Long> ids);

    /** 按 id 批量取（回显已选艺人用） */
    List<ArtistVO> selectByIds(@Param("ids") List<Long> ids);
}
