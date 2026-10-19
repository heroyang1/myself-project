package com.backstage.system.controller.internal;

import com.backstage.contract.common.InternalApiConstants;
import com.backstage.contract.common.InternalResult;
import com.backstage.contract.internal.asset.PointsBalanceResponse;
import com.backstage.contract.internal.asset.PointsChangeRequest;
import com.backstage.contract.internal.asset.PointsChangeResponse;
import com.backstage.system.domain.user.OshUserAssetRecord;
import com.backstage.system.service.user.UserAssetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内部资产（积分）接口
 *
 * <p>auth-user 域对其它服务暴露的积分能力，仅供服务间内网调用（由 InternalApiAuthFilter 校验共享密钥，
 * 且 /internal/** 不经网关对外暴露）。用户身份由调用方在请求体/参数中透传，不依赖登录态。</p>
 *
 * @author osh-backend
 * @date 2026-09-30
 */
@RestController
@RequestMapping(InternalApiConstants.ASSET_BASE)
public class InternalAssetController {

    @Autowired
    private UserAssetService userAssetService;

    /**
     * 扣减积分（支出，要求余额充足）
     *
     * @param request 积分变动请求
     * @return 变动结果（含前后余额与账本记录 ID）
     */
    @PostMapping("/deduct")
    public InternalResult<PointsChangeResponse> deduct(@RequestBody PointsChangeRequest request) {
        OshUserAssetRecord record = userAssetService.deductPoints(
                request.getUserId(),
                request.getChangeAmount() == null ? 0L : request.getChangeAmount(),
                request.getChangeSource(),
                request.getRemark());
        return InternalResult.ok(toResponse(record));
    }

    /**
     * 退还积分（收入，用于关单/SAGA 补偿）
     *
     * @param request 积分变动请求
     * @return 变动结果
     */
    @PostMapping("/refund")
    public InternalResult<PointsChangeResponse> refund(@RequestBody PointsChangeRequest request) {
        OshUserAssetRecord record = userAssetService.refundPoints(
                request.getUserId(),
                request.getChangeAmount() == null ? 0L : request.getChangeAmount(),
                request.getChangeSource(),
                request.getRemark());
        return InternalResult.ok(toResponse(record));
    }

    /**
     * 查询积分余额
     *
     * @param userId 用户 ID
     * @return 余额
     */
    @GetMapping("/balance")
    public InternalResult<PointsBalanceResponse> balance(@RequestParam("userId") Long userId) {
        long points = userAssetService.getPointsBalance(userId);
        return InternalResult.ok(new PointsBalanceResponse(userId, points));
    }

    /**
     * 账本记录映射为契约响应
     *
     * @param record 账本记录
     * @return 契约响应
     */
    private PointsChangeResponse toResponse(OshUserAssetRecord record) {
        PointsChangeResponse response = new PointsChangeResponse();
        response.setBeforeBalance(record.getBeforeBalance());
        response.setAfterBalance(record.getAfterBalance());
        response.setChangeAmount(record.getChangeAmount());
        response.setRecordId(record.getId());
        return response;
    }
}
