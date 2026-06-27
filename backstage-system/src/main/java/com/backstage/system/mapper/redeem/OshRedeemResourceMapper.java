package com.backstage.system.mapper.redeem;

import com.backstage.system.domain.redeem.vo.RedeemResourceVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OshRedeemResourceMapper {
    List<RedeemResourceVO> searchResources(@Param("resourceType") String resourceType,
                                           @Param("keyword") String keyword);
}
