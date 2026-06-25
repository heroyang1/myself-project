package com.backstage.system.service.impl.audit;

import com.backstage.common.enums.ResourceStatusEnum;
import com.backstage.common.enums.ResourceTypeEnum;
import com.backstage.common.utils.StringUtils;
import com.backstage.system.domain.audit.ResourceAuditItemVO;
import com.backstage.system.domain.user.OshUser;
import com.backstage.system.domain.websocket.WsNotifyMessage;
import com.backstage.system.mapper.audit.ResourceAuditMapper;
import com.backstage.system.mapper.user.OshUserMapper;
import com.backstage.system.service.audit.AuditAnnouncementPublisher;
import com.backstage.system.service.audit.ResourceAuditCallbackContext;
import com.backstage.system.service.audit.ResourceAuditCallbackHandler;
import com.backstage.system.service.website.IWebsiteAnnouncementService;
import com.backstage.system.service.websocket.WebSocketNotifyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

/**
 * 实用网站资源审核回调处理器
 *
 * 审核通过：写入公告栏 + WebSocket 通知提交人
 * 审核拒绝：WebSocket 通知提交人
 */
@Component
public class WebsiteResourceAuditCallbackHandler implements ResourceAuditCallbackHandler {

    private static final Logger log = LoggerFactory.getLogger(WebsiteResourceAuditCallbackHandler.class);

    @Resource
    private ResourceAuditMapper resourceAuditMapper;

    @Resource
    private OshUserMapper oshUserMapper;

    @Resource
    private WebSocketNotifyService webSocketNotifyService;

    @Resource
    private IWebsiteAnnouncementService websiteAnnouncementService;

    @Resource
    private AuditAnnouncementPublisher auditAnnouncementPublisher;

    @Override
    public List<ResourceTypeEnum> resourceTypes() {
        return Collections.singletonList(ResourceTypeEnum.WEBSITE);
    }

    @Override
    public void handle(ResourceAuditCallbackContext context) {
        ResourceAuditItemVO resource = resourceAuditMapper.selectAuditNotifyItem(
                ResourceTypeEnum.WEBSITE.getMysqlTableName(),
                context.getResourceId()
        );
        if (resource == null) {
            return;
        }

        String websiteName = StringUtils.isEmpty(resource.getTitle())
                ? String.valueOf(context.getResourceId())
                : resource.getTitle();

        // 通知提交人
        notifyCreator(resource, context.getResourceId(), context.getResourceStatus(), websiteName);

        auditAnnouncementPublisher.publishResult(ResourceTypeEnum.WEBSITE, context.getResourceId(), websiteName, context.getResourceStatus(), "/website");

        // 审核通过才写网站模块公告
        if (ResourceStatusEnum.isPublished(context.getResourceStatus())) {
            websiteAnnouncementService.insertWebsiteNotice(context.getResourceId(), websiteName);
        }
    }

    private void notifyCreator(ResourceAuditItemVO resource, Long resourceId,
                                Integer resourceStatus, String websiteName) {
        if (StringUtils.isEmpty(resource.getCreateBy())) {
            return;
        }
        Long targetUserId = parseTargetUserId(resource.getCreateBy());
        if (targetUserId == null) {
            log.warn("网站审核通知跳过：无法解析创建人ID, resourceId={}, createBy={}",
                    resourceId, resource.getCreateBy());
            return;
        }
        boolean approved = ResourceStatusEnum.isPublished(resourceStatus);
        WsNotifyMessage message = new WsNotifyMessage();
        message.setType("WEBSITE_AUDIT_RESULT");
        message.setTitle(approved ? "网站审核通过" : "网站审核未通过");
        message.setContent(webSocketNotifyService.truncate(
                "实用网站「" + websiteName + "」审核" + (approved ? "已通过" : "未通过")));
        message.setJumpUrl("/website");
        message.setBizId(String.valueOf(resourceId));
        webSocketNotifyService.send(targetUserId, message);
    }

    private Long parseTargetUserId(String createBy) {
        try {
            return Long.valueOf(createBy);
        } catch (Exception ignored) {
            OshUser user = oshUserMapper.getUserByUsername(createBy);
            return user == null ? null : user.getId();
        }
    }
}
