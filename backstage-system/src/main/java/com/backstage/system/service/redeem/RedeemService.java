package com.backstage.system.service.redeem;

import com.backstage.system.domain.redeem.OshRedeemChannel;
import com.backstage.system.domain.redeem.OshRedeemCode;
import com.backstage.system.domain.redeem.dto.RedeemChannelCreateRequest;
import com.backstage.system.domain.redeem.dto.RedeemCodeBatchCreateRequest;
import com.backstage.system.domain.redeem.dto.RedeemCodeCreateRequest;
import com.backstage.system.domain.redeem.dto.RedeemCodePageQuery;
import com.backstage.system.domain.redeem.dto.RedeemResourceSearchQuery;
import com.backstage.system.domain.redeem.vo.RedeemCodeBatchResultVO;
import com.backstage.system.domain.redeem.vo.RedeemCodeVO;
import com.backstage.system.domain.redeem.vo.RedeemResourceVO;
import com.github.pagehelper.PageInfo;

import java.util.List;

public interface RedeemService {
    List<OshRedeemChannel> listChannels();

    OshRedeemChannel createChannel(RedeemChannelCreateRequest request);

    RedeemResourceVO resolveResource(String resourceNo);

    PageInfo<RedeemResourceVO> searchResources(RedeemResourceSearchQuery query);

    OshRedeemCode createCode(RedeemCodeCreateRequest request);

    RedeemCodeBatchResultVO batchCreateCodes(RedeemCodeBatchCreateRequest request);

    PageInfo<RedeemCodeVO> pageAdminCodes(RedeemCodePageQuery query);

    PageInfo<RedeemCodeVO> pageUserCodes(Long userId, RedeemCodePageQuery query);

    OshRedeemCode redeem(Long userId, Long id);

    OshRedeemCode redeemByCode(Long userId, String code);

    void cancelCode(Long id);
}
