package com.backstage.system.service.impl.audit;

import com.alibaba.fastjson2.JSON;
import com.backstage.common.enums.AnnouncementChannelEnum;
import com.backstage.common.enums.ResourceStatusEnum;
import com.backstage.common.enums.ResourceTypeEnum;
import com.backstage.common.utils.StringUtils;
import com.backstage.system.domain.websocket.WsNotifyMessage;
import com.backstage.system.mapper.announcement.OshAnnouncementMapper;
import com.backstage.system.service.audit.AuditAnnouncementPublisher;
import com.backstage.system.service.websocket.WebSocketNotifyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuditAnnouncementPublisherImpl implements AuditAnnouncementPublisher {

    private static final Logger log = LoggerFactory.getLogger(AuditAnnouncementPublisherImpl.class);
    private static final String ANNOUNCEMENT_REFRESH = "ANNOUNCEMENT_REFRESH";
    private static final String AUDIT_MODULE = "audit";
    private static final String SYSTEM_OPERATOR = "system";
    private static final String DEFAULT_JUMP_URL = "/audit";

    @Resource
    private OshAnnouncementMapper announcementMapper;

    @Resource
    private WebSocketNotifyService webSocketNotifyService;

    @Override
    public void publishResult(ResourceTypeEnum resourceType, Long resourceId, String resourceName, Integer resourceStatus, String jumpUrl) {
        if (resourceType == null || resourceId == null) {
            return;
        }
        String resolvedJumpUrl = StringUtils.isEmpty(jumpUrl) ? DEFAULT_JUMP_URL : jumpUrl;
        String title = buildTitle(resourceType, resourceId, resourceName, resourceStatus);
        try {
            announcementMapper.insertAuditAnnouncement(
                    title,
                    resolvedJumpUrl,
                    AUDIT_MODULE,
                    resourceType.getType(),
                    resourceId,
                    AnnouncementChannelEnum.SYSTEM_NOTICE.getCode(),
                    SYSTEM_OPERATOR
            );
            broadcastAuditAnnouncementRefresh(resolvedJumpUrl);
        } catch (Exception ex) {
            log.error("写入审核公告失败, resourceType={}, resourceId={}, error={}",
                    resourceType.getType(), resourceId, ex.getMessage(), ex);
        }
    }

    private String buildTitle(ResourceTypeEnum resourceType, Long resourceId, String resourceName, Integer resourceStatus) {
        String resultText = ResourceStatusEnum.isPublished(resourceStatus) ? "审核通过" : "审核不通过";
        return getResourceTypeLabel(resourceType) + "模块的「" + resolveResourceName(resourceName, resourceId) + "」" + resultText;
    }

    private void broadcastAuditAnnouncementRefresh(String jumpUrl) {
        WsNotifyMessage broadcast = new WsNotifyMessage();
        broadcast.setType(ANNOUNCEMENT_REFRESH);
        broadcast.setTitle("审核模块公告刷新");
        broadcast.setContent(buildAnnouncementRefreshContent());
        broadcast.setJumpUrl(jumpUrl);
        broadcast.setBizId(AUDIT_MODULE);
        webSocketNotifyService.broadcast(broadcast);
    }

    private String buildAnnouncementRefreshContent() {
        Map<String, Object> content = new HashMap<>();
        content.put("module", AUDIT_MODULE);
        content.put("action", "refresh");
        content.put("refresh", true);
        content.put("noticeApi", "/pc/audit/announcement/notice");
        content.put("dynamicApi", "/pc/audit/announcement/dynamic");
        return JSON.toJSONString(content);
    }

    private String resolveResourceName(String resourceName, Long resourceId) {
        return StringUtils.isEmpty(resourceName) ? String.valueOf(resourceId) : resourceName;
    }

    private String getResourceTypeLabel(ResourceTypeEnum resourceType) {
        switch (resourceType) {
            case COURSE:
                return "课程";
            case QA_QUESTION:
                return "答疑";
            case QA_ANSWER:
                return "答疑";
            case BOOK:
                return "电子书";
            case TOOL:
                return "工具";
            case WEBSITE:
                return "实用网站";
            case OPEN_PROJECT:
                return "开源项目";
            case INFO_GAP:
                return "信息差";
            default:
                return resourceType.getType();
        }
    }
}
