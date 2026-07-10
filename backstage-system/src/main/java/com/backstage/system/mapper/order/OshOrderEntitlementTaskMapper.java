package com.backstage.system.mapper.order;

import com.backstage.system.domain.order.OshOrderEntitlementTask;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OshOrderEntitlementTaskMapper {

    int insertOrKeep(@Param("orderNo") String orderNo, @Param("productType") Integer productType);

    OshOrderEntitlementTask selectByOrderNo(@Param("orderNo") String orderNo);

    List<OshOrderEntitlementTask> selectDueTasks(@Param("now") LocalDateTime now,
                                                 @Param("staleBefore") LocalDateTime staleBefore,
                                                 @Param("limit") int limit);

    int claim(@Param("orderNo") String orderNo,
              @Param("now") LocalDateTime now,
              @Param("staleBefore") LocalDateTime staleBefore);

    int markSuccess(@Param("orderNo") String orderNo, @Param("completedTime") LocalDateTime completedTime);

    int markRetry(@Param("orderNo") String orderNo,
                  @Param("nextRetryTime") LocalDateTime nextRetryTime,
                  @Param("lastError") String lastError);
}
