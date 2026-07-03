package com.backstage.system.service.impl.audit;

import com.alibaba.fastjson2.JSON;
import com.backstage.common.enums.AnnouncementChannelEnum;
import com.backstage.common.enums.ResourceStatusEnum;
import com.backstage.common.enums.ResourceTypeEnum;
import com.backstage.common.utils.StringUtils;
import com.backstage.system.domain.audit.ResourceAuditItemVO;
import com.backstage.system.domain.user.OshUser;
import com.backstage.system.domain.websocket.WsNotifyMessage;
import com.backstage.system.mapper.audit.ResourceAuditMapper;
import com.backstage.system.mapper.tool.OshToolAnnouncementMapper;
import com.backstage.system.mapper.user.OshUserMapper;
import com.backstage.system.service.audit.AuditAnnouncementPublisher;
import com.backstage.system.service.audit.ResourceAuditCallbackContext;
import com.backstage.system.service.audit.ResourceAuditCallbackHandler;
import com.backstage.system.service.websocket.WebSocketNotifyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具资源审核回调处理器
 */
@Component
public class ToolResourceAuditCallbackHandler implements ResourceAuditCallbackHandler {

    private static final Logger log = LoggerFactory.getLogger(ToolResourceAuditCallbackHandler.class);
    private static final String TOOL_AUDIT_RESULT = "TOOL_AUDIT_RESULT";
    private static final String ANNOUNCEMENT_REFRESH = "ANNOUNCEMENT_REFRESH";
    private static final String TOOL_MODULE = "tool";
    private static final String SYSTEM_OPERATOR = "system";

    @Resource
    private ResourceAuditMapper resourceAuditMapper;

    @Resource
    private OshUserMapper oshUserMapper;

    @Resource
    private OshToolAnnouncementMapper oshToolAnnouncementMapper;

    @Resource
    private WebSocketNotifyService webSocketNotifyService;

    @Resource
    private AuditAnnouncementPublisher auditAnnouncementPublisher;

    @Override
    public List<ResourceTypeEnum> resourceTypes() {
        return Collections.singletonList(ResourceTypeEnum.TOOL);
    }

    @Override
    public void handle(ResourceAuditCallbackContext context) {
        ResourceAuditItemVO resource = resourceAuditMapper.selectAuditNotifyItem(
                ResourceTypeEnum.TOOL.getMysqlTableName(),
                context.getResourceId()
        );
        if (resource == null) {
            return;
        }
        String toolName = StringUtils.isEmpty(resource.getTitle()) ? String.valueOf(context.getResourceId()) : resource.getTitle();
        String jumpUrl = buildToolJumpUrl(resource, context.getResourceId());
        notifyCreator(resource, context.getResourceId(), context.getResourceStatus(), toolName, jumpUrl);
        auditAnnouncementPublisher.publishResult(ResourceTypeEnum.TOOL, context.getResourceId(), toolName, context.getResourceStatus(), jumpUrl);
        if (ResourceStatusEnum.isPublished(context.getResourceStatus())) {
            publishToolOnline(toolName, context.getResourceId(), jumpUrl);
        }
    }

    private void notifyCreator(ResourceAuditItemVO resource,
                               Long resourceId,
                               Integer resourceStatus,
                               String toolName,
                               String jumpUrl) {
        if (StringUtils.isEmpty(resource.getCreateBy())) {
            return;
        }
        Long targetUserId = parseTargetUserId(resource.getCreateBy());
        if (targetUserId == null) {
            log.warn("工具审核通知跳过：无法解析创建人ID, resourceId={}, createBy={}", resourceId, resource.getCreateBy());
            return;
        }
        WsNotifyMessage message = new WsNotifyMessage();
        message.setType(TOOL_AUDIT_RESULT);
        message.setTitle(ResourceStatusEnum.isPublished(resourceStatus) ? "工具审核通过" : "工具审核未通过");
        message.setContent(webSocketNotifyService.truncate(buildCreatorContent(toolName, resourceStatus)));
        message.setJumpUrl(jumpUrl);
        message.setBizId(String.valueOf(resourceId));
        webSocketNotifyService.send(targetUserId, message);
    }

    private void publishToolOnline(String toolName, Long toolId, String jumpUrl) {
        String title = "工具上新：「" + toolName + "」已上线";
        oshToolAnnouncementMapper.insertToolAnnouncement(
                title,
                jumpUrl,
                AnnouncementChannelEnum.SYSTEM_NOTICE.getCode(),
                "online",
                SYSTEM_OPERATOR
        );

        WsNotifyMessage broadcast = new WsNotifyMessage();
        broadcast.setType(ANNOUNCEMENT_REFRESH);
        broadcast.setTitle("工具模块公告刷新");
        broadcast.setContent(buildAnnouncementRefreshContent());
        broadcast.setJumpUrl(jumpUrl);
        broadcast.setBizId(TOOL_MODULE);
        webSocketNotifyService.broadcast(broadcast);
    }

    private String buildCreatorContent(String toolName, Integer resourceStatus) {
        return "工具「" + toolName + "」审核" + (ResourceStatusEnum.isPublished(resourceStatus) ? "已通过" : "未通过");
    }

    private String buildToolJumpUrl(ResourceAuditItemVO resource, Long toolId) {
        return "/tool?toolId=" + toolId + "&autoOpen=1";
    }

    private Long parseTargetUserId(String createBy) {
        try {
            return Long.valueOf(createBy);
        } catch (Exception ignored) {
            OshUser user = oshUserMapper.getUserByUsername(createBy);
            return user == null ? null : user.getId();
        }
    }

    private String buildAnnouncementRefreshContent() {
        Map<String, Object> content = new HashMap<>();
        content.put("module", TOOL_MODULE);
        content.put("action", "refresh");
        content.put("refresh", true);
        content.put("noticeApi", "/pc/tool/announcement/notice");
        content.put("dynamicApi", "/pc/tool/announcement/dynamic");
        return JSON.toJSONString(content);
    }
}
