package com.backstage.system.service.impl.user;

import com.backstage.common.constant.OshUserConstants;
import com.backstage.common.core.redis.RedisCache;
import com.backstage.common.exception.ServiceException;
import com.backstage.system.domain.user.OshUserAsset;
import com.backstage.system.domain.user.OshUserAssetRecord;
import com.backstage.system.mapper.user.OshUserAssetMapper;
import com.backstage.system.mapper.user.OshUserAssetRecordMapper;
import com.backstage.system.service.user.UserAssetService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;

/**
 * 用户资产（积分）服务实现类
 *
 * <p>积分变动统一走 {@code updatePointsAtomic} 原子条件更新，杜绝读改写在并发下的超扣；
 * 账本 before/after 通过"先原子更新持行锁、再回读"的方式保证与本次变动精确一致。</p>
 *
 * @author osh-backend
 * @date 2026-09-30
 */
@Service
public class UserAssetServiceImpl implements UserAssetService {

    /**
     * 变动类型：0-收入
     */
    private static final int CHANGE_TYPE_INCOME = 0;

    /**
     * 变动类型：1-支出
     */
    private static final int CHANGE_TYPE_EXPENSE = 1;

    @Autowired
    private OshUserAssetMapper oshUserAssetMapper;

    @Autowired
    private OshUserAssetRecordMapper oshUserAssetRecordMapper;

    @Autowired
    private RedisCache redisCache;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OshUserAssetRecord deductPoints(Long userId, long changeAmount, Integer changeSource, String remark) {
        return changePoints(userId, changeAmount, changeSource, remark, CHANGE_TYPE_EXPENSE, true, "积分不足");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OshUserAssetRecord refundPoints(Long userId, long changeAmount, Integer changeSource, String remark) {
        return changePoints(userId, changeAmount, changeSource, remark, CHANGE_TYPE_INCOME, false, "用户资产不存在，无法退回积分");
    }

    @Override
    public long getPointsBalance(Long userId) {
        if (userId == null) {
            return 0L;
        }
        OshUserAsset userAsset = oshUserAssetMapper.selectOne(
                new LambdaQueryWrapper<OshUserAsset>().eq(OshUserAsset::getUserId, userId)
        );
        return userAsset == null || userAsset.getPoints() == null ? 0L : userAsset.getPoints();
    }

    /**
     * 功能描述：积分变动核心流程（扣减/退还共用）
     * 业务流程：参数校验 → 原子条件更新余额 → 回读锁定后余额 → 计算变动前余额 → 写账本 → 提交后刷新缓存
     * 语法逻辑：先 UPDATE 再 SELECT，UPDATE 已对目标行加锁，本事务内回读即为准确的变动后余额，
     *          从而 before = after - delta 与本次原子变动严格对应，避免并发下账本失真
     * 实现效果：余额不足（扣减）或资产不存在（退还）时抛异常并回滚，不产生账本记录
     *
     * @param userId        用户 ID
     * @param changeAmount  变动数量（正数）
     * @param changeSource  变动来源
     * @param remark        账本备注
     * @param changeType    变动类型：0-收入 1-支出
     * @param requireEnough 是否要求余额充足（扣减为 true，退还为 false）
     * @param failMessage   原子更新失败时的异常提示
     * @return 已写入的账本记录
     */
    private OshUserAssetRecord changePoints(Long userId, long changeAmount, Integer changeSource, String remark,
                                            int changeType, boolean requireEnough, String failMessage) {
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (changeAmount <= 0) {
            throw new IllegalArgumentException("积分变动数量必须大于0");
        }
        if (changeSource == null) {
            throw new IllegalArgumentException("积分变动来源不能为空");
        }

        // 【计算有符号增量】支出为负、收入为正，与 updatePointsAtomic 的 points = points + delta 语义对齐
        long delta = changeType == CHANGE_TYPE_EXPENSE ? -changeAmount : changeAmount;

        // 【原子闸门】SQL WHERE points + delta >= 0 保证并发下不会扣成负余额；受影响行数 != 1 视为失败
        int rows = oshUserAssetMapper.updatePointsAtomic(userId, delta, requireEnough);
        if (rows != 1) {
            throw new ServiceException(failMessage);
        }

        // 【回读锁定后余额】UPDATE 已持行锁，事务内回读即本次变动后的准确余额
        OshUserAsset userAsset = oshUserAssetMapper.selectByUserIdForUpdate(userId);
        long afterBalance = userAsset == null || userAsset.getPoints() == null ? 0L : userAsset.getPoints();
        long beforeBalance = afterBalance - delta;

        // 【写账本】createBy/updateBy 显式置为 userId：内部调用无登录上下文，避免依赖自动填充得到 null
        OshUserAssetRecord record = new OshUserAssetRecord();
        record.setUserId(userId);
        record.setChangeType(changeType);
        record.setChangeSource(changeSource);
        record.setChangeAmount(changeAmount);
        record.setBeforeBalance(beforeBalance);
        record.setAfterBalance(afterBalance);
        record.setRemark(remark);
        record.setCreateBy(userId);
        record.setUpdateBy(userId);
        oshUserAssetRecordMapper.insert(record);

        // 【提交后刷新缓存】事务提交后再刷新登录态积分缓存，避免缓存领先于库
        registerAssetCacheRefreshAfterCommit(userId, afterBalance);
        return record;
    }

    /**
     * 刷新登录用户缓存中的积分快照（与 ToolPurchaseServiceImpl 现有行为保持一致）
     *
     * @param userId 用户 ID
     * @param points 最新积分
     */
    @SuppressWarnings("unchecked")
    private void refreshUserAssetCache(Long userId, Long points) {
        Map<String, Object> userMap = redisCache.getCacheObject(OshUserConstants.LOGIN_USER + userId);
        if (userMap == null) {
            return;
        }
        Object assetObject = userMap.get(OshUserConstants.ASSET);
        if (!(assetObject instanceof Map)) {
            return;
        }
        Map<String, String> asset = (Map<String, String>) assetObject;
        asset.put(OshUserConstants.POINTS, String.valueOf(points));
        userMap.put(OshUserConstants.ASSET, asset);
        redisCache.setCacheObject(OshUserConstants.LOGIN_USER + userId, userMap);
    }

    /**
     * 注册事务提交后回调刷新缓存；无事务时立即刷新
     *
     * @param userId 用户 ID
     * @param points 最新积分
     */
    private void registerAssetCacheRefreshAfterCommit(Long userId, Long points) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            refreshUserAssetCache(userId, points);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                refreshUserAssetCache(userId, points);
            }
        });
    }
}
