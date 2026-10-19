package com.backstage.system.service.audit;

import com.backstage.common.enums.ResourceStatusEnum;
import com.backstage.common.enums.ResourceTypeEnum;
import com.backstage.system.domain.audit.ResourceAuditItemVO;
import com.backstage.system.mapper.audit.ResourceAuditMapper;
import com.backstage.system.mapper.user.OshUserMapper;
import com.backstage.system.service.impl.audit.WebsiteResourceAuditCallbackHandler;
import com.backstage.system.service.website.IWebsiteAnnouncementService;
import com.backstage.system.service.websocket.WebSocketNotifyService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WebsiteResourceAuditCallbackHandlerTest {

    private WebsiteResourceAuditCallbackHandler handler;
    private ResourceAuditMapper resourceAuditMapper;
    private IWebsiteAnnouncementService websiteAnnouncementService;
    private AuditAnnouncementPublisher auditAnnouncementPublisher;
    private WebSocketNotifyService webSocketNotifyService;

    @Before
    public void setUp() {
        handler = new WebsiteResourceAuditCallbackHandler();
        resourceAuditMapper = mock(ResourceAuditMapper.class);
        websiteAnnouncementService = mock(IWebsiteAnnouncementService.class);
        auditAnnouncementPublisher = mock(AuditAnnouncementPublisher.class);
        webSocketNotifyService = mock(WebSocketNotifyService.class);
        when(webSocketNotifyService.truncate("实用网站「Vue官网」审核已通过")).thenReturn("实用网站「Vue官网」审核已通过");

        ReflectionTestUtils.setField(handler, "resourceAuditMapper", resourceAuditMapper);
        ReflectionTestUtils.setField(handler, "oshUserMapper", mock(OshUserMapper.class));
        ReflectionTestUtils.setField(handler, "webSocketNotifyService", webSocketNotifyService);
        ReflectionTestUtils.setField(handler, "websiteAnnouncementService", websiteAnnouncementService);
        ReflectionTestUtils.setField(handler, "auditAnnouncementPublisher", auditAnnouncementPublisher);
    }

    @Test
    public void shouldPublishWebsiteAnnouncementAndAuditAnnouncementWhenWebsiteApproved() {
        ResourceAuditItemVO website = buildWebsite(2001L, "Vue官网", "9");
        when(resourceAuditMapper.selectAuditNotifyItem(ResourceTypeEnum.WEBSITE.getMysqlTableName(), 2001L)).thenReturn(website);

        handler.handle(buildContext(2001L, ResourceStatusEnum.PUBLISHED.getCode()));

        verify(auditAnnouncementPublisher).publishResult(ResourceTypeEnum.WEBSITE, 2001L, "Vue官网", ResourceStatusEnum.PUBLISHED.getCode(), "/website");
        verify(websiteAnnouncementService).insertWebsiteNotice(2001L, "Vue官网");
    }

    private ResourceAuditCallbackContext buildContext(Long resourceId, Integer resourceStatus) {
        ResourceAuditCallbackContext context = new ResourceAuditCallbackContext();
        context.setResourceType(ResourceTypeEnum.WEBSITE);
        context.setResourceId(resourceId);
        context.setResourceStatus(resourceStatus);
        return context;
    }

    private ResourceAuditItemVO buildWebsite(Long id, String title, String createBy) {
        ResourceAuditItemVO resource = new ResourceAuditItemVO();
        resource.setId(id);
        resource.setTitle(title);
        resource.setCreateBy(createBy);
        return resource;
    }
}
