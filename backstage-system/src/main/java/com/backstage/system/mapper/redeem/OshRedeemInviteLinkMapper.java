package com.backstage.system.mapper.redeem;

import com.backstage.system.domain.redeem.OshRedeemInviteLink;
import com.backstage.system.domain.redeem.vo.RedeemInviteLinkVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OshRedeemInviteLinkMapper extends BaseMapper<OshRedeemInviteLink> {
    RedeemInviteLinkVO selectByToken(@Param("token") String token);
}
