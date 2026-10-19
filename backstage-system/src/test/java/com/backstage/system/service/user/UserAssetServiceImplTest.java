package com.backstage.system.service.user;

import com.backstage.common.constant.OshUserConstants;
import com.backstage.common.core.redis.RedisCache;
import com.backstage.common.exception.ServiceException;
import com.backstage.system.domain.user.OshUserAsset;
import com.backstage.system.domain.user.OshUserAssetRecord;
import com.backstage.system.mapper.user.OshUserAssetMapper;
import com.backstage.system.mapper.user.OshUserAssetRecordMapper;
import com.backstage.system.service.impl.user.UserAssetServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用户资产（积分）服务资金安全测试
 *
 * <p>覆盖 {@code /internal/auth-user/asset/deduct|refund} 背后的核心资金逻辑：原子扣减/退还、
 * 账本 before/after 精确一致、余额不足与资产缺失的失败终态不落账、参数校验拦截、
 * 以及登录态积分缓存"提交后刷新"的有事务/无事务双路径。</p>
 *
 * @author osh-backend
 * @date 2026-09-30
 */
public class UserAssetServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final Integer CHANGE_SOURCE_BUY = 3;

    private OshUserAssetMapper assetMapper;
    private OshUserAssetRecordMapper recordMapper;
    private RedisCache redisCache;
    private UserAssetServiceImpl service;

    /**
     * 构建被测服务并注入 mock 依赖（与项目现有 Service 层测试范式一致）
     *
     * @return 已注入依赖的被测服务
     */
    private UserAssetServiceImpl newService() {
        UserAssetServiceImpl target = new UserAssetServiceImpl();
        assetMapper = mock(OshUserAssetMapper.class);
        recordMapper = mock(OshUserAssetRecordMapper.class);
        redisCache = mock(RedisCache.class);
        ReflectionTestUtils.setField(target, "oshUserAssetMapper", assetMapper);
        ReflectionTestUtils.setField(target, "oshUserAssetRecordMapper", recordMapper);
        ReflectionTestUtils.setField(target, "redisCache", redisCache);
        return target;
    }

    /**
     * 构造一个持有指定积分的用户资产行
     *
     * @param points 积分余额
     * @return 用户资产
     */
    private OshUserAsset assetWith(long points) {
        OshUserAsset asset = new OshUserAsset();
        asset.setUserId(USER_ID);
        asset.setPoints(points);
        return asset;
    }

    @AfterEach
    public void tearDown() {
        // 【测试隔离】清理可能残留的事务同步器，避免污染后续用例
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    public void shouldDeductPointsAndRecordAccurateBeforeAfterBalance() {
        service = newService();
        when(assetMapper.updatePointsAtomic(anyLong(), anyLong(), anyBoolean())).thenReturn(1);
        when(assetMapper.selectByUserIdForUpdate(USER_ID)).thenReturn(assetWith(150L));

        OshUserAssetRecord record = service.deductPoints(USER_ID, 50L, CHANGE_SOURCE_BUY, "购买工具");

        // 【原子增量】扣减应以负增量、requireEnough=true 触发余额充足闸门
        ArgumentCaptor<Long> deltaCaptor = ArgumentCaptor.forClass(Long.class);
        verify(assetMapper).updatePointsAtomic(eq(USER_ID), deltaCaptor.capture(), eq(true));
        assertEquals(Long.valueOf(-50L), deltaCaptor.getValue());
        // 【账本一致】支出类型；before = after - delta = 150 -(-50)= 200
        assertEquals(Integer.valueOf(1), record.getChangeType());
        assertEquals(Long.valueOf(50L), record.getChangeAmount());
        assertEquals(Long.valueOf(200L), record.getBeforeBalance());
        assertEquals(Long.valueOf(150L), record.getAfterBalance());
        assertEquals(USER_ID, record.getCreateBy());
        verify(recordMapper, times(1)).insert(any(OshUserAssetRecord.class));
    }

    @Test
    public void shouldThrowAndSkipLedgerWhenDeductBalanceInsufficient() {
        service = newService();
        // 【失败终态】原子更新受影响行数为 0，代表余额不足（WHERE points+delta>=0 未命中）
        when(assetMapper.updatePointsAtomic(anyLong(), anyLong(), anyBoolean())).thenReturn(0);

        assertThrows(ServiceException.class,
                () -> service.deductPoints(USER_ID, 50L, CHANGE_SOURCE_BUY, "购买工具"));

        // 【副作用验证】余额不足时绝不落账本记录
        verify(recordMapper, never()).insert(any(OshUserAssetRecord.class));
    }

    @Test
    public void shouldRefundPointsAsIncomeWithoutBalanceGuard() {
        service = newService();
        when(assetMapper.updatePointsAtomic(anyLong(), anyLong(), anyBoolean())).thenReturn(1);
        when(assetMapper.selectByUserIdForUpdate(USER_ID)).thenReturn(assetWith(250L));

        OshUserAssetRecord record = service.refundPoints(USER_ID, 50L, CHANGE_SOURCE_BUY, "订单退款");

        // 【原子增量】退还应以正增量、requireEnough=false（不校验余额充足）
        ArgumentCaptor<Long> deltaCaptor = ArgumentCaptor.forClass(Long.class);
        verify(assetMapper).updatePointsAtomic(eq(USER_ID), deltaCaptor.capture(), eq(false));
        assertEquals(Long.valueOf(50L), deltaCaptor.getValue());
        // 【账本一致】收入类型；before = after - delta = 250 - 50 = 200
        assertEquals(Integer.valueOf(0), record.getChangeType());
        assertEquals(Long.valueOf(200L), record.getBeforeBalance());
        assertEquals(Long.valueOf(250L), record.getAfterBalance());
        verify(recordMapper, times(1)).insert(any(OshUserAssetRecord.class));
    }

    @Test
    public void shouldThrowAndSkipLedgerWhenRefundTargetAssetMissing() {
        service = newService();
        when(assetMapper.updatePointsAtomic(anyLong(), anyLong(), anyBoolean())).thenReturn(0);

        assertThrows(ServiceException.class,
                () -> service.refundPoints(USER_ID, 50L, CHANGE_SOURCE_BUY, "订单退款"));

        verify(recordMapper, never()).insert(any(OshUserAssetRecord.class));
    }

    @Test
    public void shouldRejectNonPositiveChangeAmountWithoutTouchingDb() {
        service = newService();

        assertThrows(IllegalArgumentException.class,
                () -> service.deductPoints(USER_ID, 0L, CHANGE_SOURCE_BUY, "非法"));
        assertThrows(IllegalArgumentException.class,
                () -> service.refundPoints(USER_ID, -10L, CHANGE_SOURCE_BUY, "非法"));

        // 【参数校验前置】非法金额不得触达任何持久化操作
        verify(assetMapper, never()).updatePointsAtomic(anyLong(), anyLong(), anyBoolean());
        verify(recordMapper, never()).insert(any(OshUserAssetRecord.class));
    }

    @Test
    public void shouldRejectNullUserIdOrChangeSource() {
        service = newService();

        assertThrows(IllegalArgumentException.class,
                () -> service.deductPoints(null, 50L, CHANGE_SOURCE_BUY, "缺用户"));
        assertThrows(IllegalArgumentException.class,
                () -> service.deductPoints(USER_ID, 50L, null, "缺来源"));

        verify(assetMapper, never()).updatePointsAtomic(anyLong(), anyLong(), anyBoolean());
    }

    @Test
    public void shouldReturnZeroBalanceWhenAssetRowMissing() {
        service = newService();
        when(assetMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertEquals(0L, service.getPointsBalance(USER_ID));
    }

    @Test
    public void shouldReturnZeroBalanceWithoutQueryWhenUserIdNull() {
        service = newService();

        assertEquals(0L, service.getPointsBalance(null));

        verify(assetMapper, never()).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    public void shouldRefreshPointsCacheImmediatelyWhenNoTransactionActive() {
        service = newService();
        assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
        when(assetMapper.updatePointsAtomic(anyLong(), anyLong(), anyBoolean())).thenReturn(1);
        when(assetMapper.selectByUserIdForUpdate(USER_ID)).thenReturn(assetWith(150L));
        Map<String, String> assetMap = loginCacheWithPoints("999");

        service.deductPoints(USER_ID, 50L, CHANGE_SOURCE_BUY, "购买工具");

        // 【无事务分支】立即刷新登录态积分快照为最新余额
        assertEquals("150", assetMap.get(OshUserConstants.POINTS));
        verify(redisCache).setCacheObject(eq(OshUserConstants.LOGIN_USER + USER_ID), any());
    }

    @Test
    public void shouldDeferPointsCacheRefreshUntilAfterCommitWhenTransactionActive() {
        service = newService();
        when(assetMapper.updatePointsAtomic(anyLong(), anyLong(), anyBoolean())).thenReturn(1);
        when(assetMapper.selectByUserIdForUpdate(USER_ID)).thenReturn(assetWith(150L));
        Map<String, String> assetMap = loginCacheWithPoints("999");

        // 【有事务分支】模拟事务进行中：缓存刷新应被延迟，不得领先于库提交
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.deductPoints(USER_ID, 50L, CHANGE_SOURCE_BUY, "购买工具");

            verify(redisCache, never()).setCacheObject(anyString(), any());
            assertEquals("999", assetMap.get(OshUserConstants.POINTS));

            List<TransactionSynchronization> synchronizations =
                    TransactionSynchronizationManager.getSynchronizations();
            assertEquals(1, synchronizations.size());
            for (TransactionSynchronization synchronization : synchronizations) {
                synchronization.afterCommit();
            }

            // 【提交后】回调触发，缓存刷新为最新余额
            assertEquals("150", assetMap.get(OshUserConstants.POINTS));
            verify(redisCache).setCacheObject(eq(OshUserConstants.LOGIN_USER + USER_ID), any());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    /**
     * 构造含积分快照的登录态缓存，并桩定 redisCache 读取该缓存
     *
     * @param points 缓存中的旧积分值
     * @return 缓存内的 asset 子 Map（供断言刷新结果）
     */
    private Map<String, String> loginCacheWithPoints(String points) {
        Map<String, String> assetMap = new HashMap<>();
        assetMap.put(OshUserConstants.POINTS, points);
        Map<String, Object> userMap = new HashMap<>();
        userMap.put(OshUserConstants.ASSET, assetMap);
        doReturn(userMap).when(redisCache).getCacheObject(OshUserConstants.LOGIN_USER + USER_ID);
        return assetMap;
    }
}
