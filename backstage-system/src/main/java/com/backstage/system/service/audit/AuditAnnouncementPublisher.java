package com.backstage.system.service.audit;

import com.backstage.common.enums.ResourceTypeEnum;

public interface AuditAnnouncementPublisher {

    void publishResult(ResourceTypeEnum resourceType, Long resourceId, String resourceName, Integer resourceStatus, String jumpUrl);

    default void publishApproved(ResourceTypeEnum resourceType, Long resourceId, String resourceName, String jumpUrl) {
        publishResult(resourceType, resourceId, resourceName, com.backstage.common.enums.ResourceStatusEnum.PUBLISHED.getCode(), jumpUrl);
    }
}
