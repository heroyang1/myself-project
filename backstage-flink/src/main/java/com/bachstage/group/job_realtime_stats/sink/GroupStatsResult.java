package com.bachstage.group.job_realtime_stats.sink;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 拼团统计聚合结果
 *
 * @author system
 * @date 2026-07-12
 */
public class GroupStatsResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long activityId;
    private final BigDecimal gmv;
    private final long orderCount;
    private final long windowEnd;

    public GroupStatsResult(Long activityId, BigDecimal gmv, long orderCount, long windowEnd) {
        this.activityId = activityId;
        this.gmv = gmv;
        this.orderCount = orderCount;
        this.windowEnd = windowEnd;
    }

    public Long getActivityId() { return activityId; }
    public BigDecimal getGmv() { return gmv; }
    public long getOrderCount() { return orderCount; }
    public long getWindowEnd() { return windowEnd; }

    @Override
    public String toString() {
        return "GroupStatsResult{activityId=" + activityId +
                ", gmv=" + gmv + ", orderCount=" + orderCount +
                ", windowEnd=" + windowEnd + "}";
    }
}
