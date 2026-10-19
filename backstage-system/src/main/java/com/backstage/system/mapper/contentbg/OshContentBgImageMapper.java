package com.backstage.system.mapper.contentbg;

import com.backstage.system.domain.contentbg.OshContentBgImage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OshContentBgImageMapper extends BaseMapper<OshContentBgImage> {

    int clearGlobalDefault(@Param("operator") String operator);
}
