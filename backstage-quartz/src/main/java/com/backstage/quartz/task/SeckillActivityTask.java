package com.backstage.quartz.task;

import com.backstage.system.domain.seckill.OshSeckillActivity;
import com.backstage.system.domain.seckill.OshSeckillActivityItem;
import com.backstage.system.mapper.seckill.OshSeckillActivityItemMapper;
import com.backstage.system.mapper.seckill.OshSeckillActivityMapper;
import com.backstage.system.mapper.seckill.OshSeckillGoodsTagMapper;
import com.backstage.system.service.OutboxEventService;
import com.backstage.system.service.seckill.SeckillItemIndexDeleteMessage;
import com.backstage.system.service.seckill.SeckillItemIndexEventType;
import com.backstage.system.service.seckill.SeckillItemIndexUpsertMessage;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 秒杀活动状态自动流转定时任务
 * 未开始(1) → 进行中(2)：更新状态 + 预热 Redis 缓存
 * 进行中(2) → 已结束(3)：更新状态 + 清理 Redis 缓存
 *
 * xxl-job handler 名称：seckill-activity-status
 * Cron：0 0/5 * * * ?（每5分钟执行一次）
 */
@Component
public class SeckillActivityTask {

    private static final Logger logger = LoggerFactory.getLogger(SeckillActivityTask.class);

    private static final String SECKILL_ACTIVITY_KEY = "seckill:activity:";
    private static final String SECKILL_ITEM_KEY     = "seckill:item:";
    private static final String SECKILL_STOCK_KEY    = "seckill:stock:";
    private static final long   CACHE_EXPIRE         = 7200L;

    @Autowired
    private OshSeckillActivityMapper activityMapper;

    @Autowired
    private OshSeckillActivityItemMapper itemMapper;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private OutboxEventService outboxEventService;

    @Autowired
    private OshSeckillGoodsTagMapper seckillGoodsTagMapper;

    /**
     * 每5分钟执行一次，自动流转活动状态
     * xxl-job handler 名称：seckill-activity-status
     */
    @XxlJob("seckill-activity-status")
    public void updateActivityStatus() {
        XxlJobHelper.log("【秒杀活动状态流转】任务开始");

        // ===== 1. 未开始 → 进行中 =====
        List<OshSeckillActivity> toStart = activityMapper.selectActivitiesToStart();
        List<Long> toStartIds = toStart.stream().map(OshSeckillActivity::getId).collect(Collectors.toList());
        // select 与 update 范围对齐：仅更新本次查询命中的活动，
        // 避免新到期活动被误更新却无缓存预热/ES 事件（丢事件后无法自愈）
        int startCount = toStartIds.isEmpty() ? 0 : activityMapper.updateToOngoing(toStartIds);
        if (startCount > 0) {
            logger.info("【秒杀活动状态流转】{}个活动状态更新为进行中，开始预热缓存", startCount);
            XxlJobHelper.log("{}个活动状态更新为进行中，开始预热缓存", startCount);
            toStart.forEach(activity -> {
                OshSeckillActivity latest = activityMapper.selectActivityById(activity.getId());
                if (latest == null) return;

                // 预热活动缓存（status=2）
                redisTemplate.opsForValue().set(
                        SECKILL_ACTIVITY_KEY + latest.getId(),
                        latest, CACHE_EXPIRE, TimeUnit.SECONDS);

                // 预热明细缓存 + 库存 Key
                List<OshSeckillActivityItem> items = itemMapper.selectItemsByActivityId(latest.getId());
                for (OshSeckillActivityItem item : items) {
                    redisTemplate.opsForValue().set(
                            SECKILL_ITEM_KEY + item.getId(),
                            item, CACHE_EXPIRE, TimeUnit.SECONDS);

                    String stockKey = SECKILL_STOCK_KEY + latest.getId() + ":" + item.getId();
                    stringRedisTemplate.opsForValue().setIfAbsent(
                            stockKey,
                            String.valueOf(item.getAvailableStock()),
                            CACHE_EXPIRE, TimeUnit.SECONDS);
                }
                // 发送 ES upsert 事件（原 SeckillActivityStatusTask 职责，合并避免双调度器竞争）
                for (OshSeckillActivityItem item : items) {
                    try {
                        outboxEventService.saveSeckillItemIndexEvent(
                                item.getId(),
                                buildUpsertMessage(item, latest, SeckillItemIndexEventType.SECKILL_ITEM_INDEX_UPDATE),
                                "status-task");
                    } catch (Exception e) {
                        logger.error("【秒杀活动状态流转】发送 ES upsert 事件失败，itemId={}", item.getId(), e);
                    }
                }
                logger.info("【秒杀活动状态流转】活动{}缓存预热完成，明细数量：{}", latest.getId(), items.size());
                XxlJobHelper.log("活动{}缓存预热完成，明细数量：{}", latest.getId(), items.size());
            });
        }

        // ===== 2. 进行中 → 已结束 =====
        List<OshSeckillActivity> toEnd = activityMapper.selectActivitiesToEnd();
        List<Long> toEndIds = toEnd.stream().map(OshSeckillActivity::getId).collect(Collectors.toList());
        // 同上：select 与 update 范围对齐，保证每个被更新的活动都有对应的缓存清理/ES delete 事件
        int endCount = toEndIds.isEmpty() ? 0 : activityMapper.updateToFinished(toEndIds);
        if (endCount > 0) {
            logger.info("【秒杀活动状态流转】{}个活动状态更新为已结束，清理缓存", endCount);
            XxlJobHelper.log("{}个活动状态更新为已结束，清理缓存", endCount);
            toEnd.forEach(activity -> {
                redisTemplate.delete(SECKILL_ACTIVITY_KEY + activity.getId());
                List<OshSeckillActivityItem> items = itemMapper.selectItemsByActivityId(activity.getId());
                for (OshSeckillActivityItem item : items) {
                    redisTemplate.delete(SECKILL_ITEM_KEY + item.getId());
                    stringRedisTemplate.delete(SECKILL_STOCK_KEY + activity.getId() + ":" + item.getId());
                    // 发送 ES delete 事件（原 SeckillActivityStatusTask 职责，合并避免双调度器竞争）
                    try {
                        outboxEventService.saveSeckillItemIndexDeleteEvent(
                                item.getId(),
                                new SeckillItemIndexDeleteMessage(item.getId()),
                                "status-task");
                    } catch (Exception e) {
                        logger.error("【秒杀活动状态流转】发送 ES delete 事件失败，itemId={}", item.getId(), e);
                    }
                }
                logger.info("【秒杀活动状态流转】活动{}缓存已清理", activity.getId());
                XxlJobHelper.log("活动{}缓存已清理", activity.getId());
            });
        }

        XxlJobHelper.log("【秒杀活动状态流转】任务完成，开始数量={}，结束数量={}", startCount, endCount);
    }

    /**
     * 构建 ES upsert 消息（从 SeckillActivityStatusTask 合并而来）
     */
    private SeckillItemIndexUpsertMessage buildUpsertMessage(
            OshSeckillActivityItem item, OshSeckillActivity activity, String eventType) {
        SeckillItemIndexUpsertMessage msg = new SeckillItemIndexUpsertMessage();
        msg.setEventType(eventType);
        msg.setId(item.getId());
        msg.setActivityId(activity.getId());
        msg.setActivityStatus(activity.getStatus());
        msg.setGoodsId(item.getGoodsId());
        msg.setGoodsType(item.getGoodsType());
        msg.setTitle(item.getTitle());
        msg.setCover(item.getCover());
        msg.setOriginPrice(item.getOriginPrice());
        msg.setSeckillPrice(item.getSeckillPrice());
        msg.setTotalStock(item.getTotalStock());
        msg.setAvailableStock(item.getAvailableStock());
        msg.setSoldCount(item.getSoldCount() != null ? item.getSoldCount() : 0);
        msg.setLimitPerUser(item.getLimitPerUser());
        msg.setSort(item.getSort());
        msg.setActivityTitle(activity.getTitle());
        msg.setPayTimeoutMin(activity.getPayTimeoutMin());
        msg.setStartTime(activity.getStartTime());
        msg.setEndTime(activity.getEndTime());
        msg.setDeleteFlag(item.getDeleteFlag() != null ? item.getDeleteFlag() : 0);
        msg.setCreateTime(item.getCreateTime());
        msg.setUpdateTime(item.getUpdateTime());
        // 写入标签
        if (item.getSeckillGoodsId() != null) {
            List<String> tagNames = seckillGoodsTagMapper.selectTagNamesBySeckillGoodsId(item.getSeckillGoodsId());
            msg.setTagNames(tagNames);
            msg.setTagNamesText(tagNames == null || tagNames.isEmpty() ? "" : String.join(" ", tagNames));
        }
        return msg;
    }
}
