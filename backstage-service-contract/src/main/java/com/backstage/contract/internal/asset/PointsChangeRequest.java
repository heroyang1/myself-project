package com.backstage.contract.internal.asset;

import java.io.Serializable;

/**
 * 内部积分变动请求（扣减/退还共用）
 *
 * <p>映射到单体 {@code osh_user_asset} + {@code osh_user_asset_record} 账本写入。
 * 扣减端点要求余额充足（不足则失败），退还端点为收入方向、不校验余额。
 * 变动方向（收入/支出）由所调用的端点决定，请求体不携带，避免调用方误传。</p>
 *
 * @author osh-backend
 * @date 2026-09-30
 */
public class PointsChangeRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户 ID（非空）
     */
    private Long userId;

    /**
     * 变动积分数量（正数，非空且大于 0）
     */
    private Long changeAmount;

    /**
     * 变动来源：0-签到 1-观看视频 2-分享 3-购买商品 4-提现 5-管理员调整
     */
    private Integer changeSource;

    /**
     * 备注说明（写入账本 remark，建议包含业务单号便于对账）
     */
    private String remark;

    /**
     * 操作人标识（写入审计字段 createBy/updateBy）
     */
    private String operator;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getChangeAmount() {
        return changeAmount;
    }

    public void setChangeAmount(Long changeAmount) {
        this.changeAmount = changeAmount;
    }

    public Integer getChangeSource() {
        return changeSource;
    }

    public void setChangeSource(Integer changeSource) {
        this.changeSource = changeSource;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }
}
