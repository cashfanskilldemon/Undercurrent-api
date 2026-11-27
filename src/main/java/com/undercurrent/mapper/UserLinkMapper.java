package com.undercurrent.mapper;

import com.undercurrent.domain.UserLink;
import com.undercurrent.vo.UserLinkVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserLinkMapper {

    List<UserLinkVO> selectByUser(@Param("userId") Long userId);

    int countByUser(@Param("userId") Long userId);

    int insert(UserLink link);

    int deleteById(@Param("id") Long id, @Param("userId") Long userId);
}
