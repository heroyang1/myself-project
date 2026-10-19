package com.backstage.system.service.user;

import com.backstage.system.domain.user.OshUserAssetRecord;

/**
 * 用户资产（积分）服务
 *
 * <p>统一封装积分扣减、退还与余额查询，作为 auth-user 域对外提供的资产能力入口。
 * 供单体内部业务与未来 tool-service 通过 /internal 端点复用，避免积分账本逻辑分散重复。</p>
 *
 * <p>并发安全：扣减/退还均基于 {@code OshUserAssetMapper.updatePointsAtomic} 的
 * SQL 原子条件更新（{@code WHERE points + delta >= 0}）防止负余额，不依赖读改写。</p>
 *
 * @author osh-backend
 * @date 2026-09-30
 */
public interface UserAssetService {

    /**
     * 功能描述：扣减用户积分（支出方向，要求余额充足）
     * 业务流程：原子条件更新余额 → 回读锁定后余额 → 计算变动前余额 → 写入账本 → 提交后刷新登录缓存
     * 语法逻辑：先执行 UPDATE（持行锁）再 SELECT，保证账本 before/after 与本次原子变动精确一致
     * 实现效果：余额不足时抛出 ServiceException 且不产生任何账本记录
     *
     * @param userId       用户 ID（非空）
     * @param changeAmount 扣减积分数量（正数）
     * @param changeSource 变动来源：0-签到 1-观看视频 2-分享 3-购买商品 4-提现 5-管理员调整
     * @param remark       账本备注（建议含业务单号）
     * @return 已写入的积分账本记录
     */
    OshUserAssetRecord deductPoints(Long userId, long changeAmount, Integer changeSource, String remark);

    /**
     * 功能描述：退还用户积分（收入方向，用于关单/SAGA 补偿）
     * 业务流程：原子更新余额 → 回读锁定后余额 → 计算变动前余额 → 写入账本 → 提交后刷新登录缓存
     * 实现效果：用户资产不存在时抛出 ServiceException
     *
     * @param userId       用户 ID（非空）
     * @param changeAmount 退还积分数量（正数）
     * @param changeSource 变动来源，取值同 deductPoints
     * @param remark       账本备注（建议含业务单号）
     * @return 已写入的积分账本记录
     */
    OshUserAssetRecord refundPoints(Long userId, long changeAmount, Integer changeSource, String remark);

    /**
     * 功能描述：查询用户当前积分余额
     * 实现效果：无资产记录时返回 0
     *
     * @param userId 用户 ID
     * @return 积分余额
     */
    long getPointsBalance(Long userId);
}
