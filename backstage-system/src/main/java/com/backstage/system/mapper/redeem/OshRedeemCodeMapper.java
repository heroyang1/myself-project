package com.backstage.system.mapper.redeem;

import com.backstage.system.domain.redeem.OshRedeemCode;
import com.backstage.system.domain.redeem.dto.RedeemCodePageQuery;
import com.backstage.system.domain.redeem.vo.RedeemCodeVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OshRedeemCodeMapper extends BaseMapper<OshRedeemCode> {
    List<RedeemCodeVO> selectAdminPage(@Param("query") RedeemCodePageQuery query);

    List<RedeemCodeVO> selectUserPage(@Param("userId") Long userId, @Param("query") RedeemCodePageQuery query);

    OshRedeemCode selectByIdForUpdate(@Param("id") Long id);

    OshRedeemCode selectByCodeForUpdate(@Param("code") String code);

    int markRedeemed(@Param("id") Long id,
                     @Param("orderNo") String orderNo,
                     @Param("paymentNo") String paymentNo,
                     @Param("redeemTime") LocalDateTime redeemTime,
                     @Param("operatorId") Long operatorId);

    int cancelPending(@Param("id") Long id,
                      @Param("cancelTime") LocalDateTime cancelTime,
                      @Param("operatorId") Long operatorId);
}
