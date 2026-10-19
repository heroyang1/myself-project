package com.backstage.contract.internal.asset;

import java.io.Serializable;

/**
 * 内部积分变动响应（扣减/退还共用）
 *
 * <p>返回变动前后余额与账本记录 ID，供调用方对账与幂等追溯。</p>
 *
 * @author osh-backend
 * @date 2026-09-30
 */
public class PointsChangeResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 变动后余额
     */
    private Long afterBalance;

    /**
     * 变动前余额
     */
    private Long beforeBalance;

    /**
     * 本次变动数量（正数）
     */
    private Long changeAmount;

    /**
     * 账本记录 ID
     */
    private Long recordId;

    public Long getAfterBalance() {
        return afterBalance;
    }

    public void setAfterBalance(Long afterBalance) {
        this.afterBalance = afterBalance;
    }

    public Long getBeforeBalance() {
        return beforeBalance;
    }

    public void setBeforeBalance(Long beforeBalance) {
        this.beforeBalance = beforeBalance;
    }

    public Long getChangeAmount() {
        return changeAmount;
    }

    public void setChangeAmount(Long changeAmount) {
        this.changeAmount = changeAmount;
    }

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }
}
