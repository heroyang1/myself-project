package com.backstage.contract.internal.asset;

import java.io.Serializable;

/**
 * 内部积分余额查询响应
 *
 * @author osh-backend
 * @date 2026-09-30
 */
public class PointsBalanceResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 当前积分余额（无资产记录时返回 0）
     */
    private Long points;

    public PointsBalanceResponse() {
    }

    public PointsBalanceResponse(Long userId, Long points) {
        this.userId = userId;
        this.points = points;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getPoints() {
        return points;
    }

    public void setPoints(Long points) {
        this.points = points;
    }
}
