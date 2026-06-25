package com.backstage.system.service.audit;

import com.backstage.common.enums.ResourceStatusEnum;
import com.backstage.common.enums.ResourceTypeEnum;
import com.backstage.system.domain.audit.ResourceAuditItemVO;
import com.backstage.system.mapper.audit.ResourceAuditMapper;
import com.backstage.system.mapper.user.OshUserMapper;
import com.backstage.system.service.impl.audit.DefaultResourceAuditCallbackHandler;
import com.backstage.system.service.websocket.WebSocketNotifyService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultResourceAuditCallbackHandlerTest {

    private DefaultResourceAuditCallbackHandler handler;
    private ResourceAuditMapper resourceAuditMapper;
    private AuditAnnouncementPublisher auditAnnouncementPublisher;
    private WebSocketNotifyService webSocketNotifyService;

    @Before
    public void setUp() {
        handler = new DefaultResourceAuditCallbackHandler();
        resourceAuditMapper = mock(ResourceAuditMapper.class);
        auditAnnouncementPublisher = mock(AuditAnnouncementPublisher.class);
        webSocketNotifyService = mock(WebSocketNotifyService.class);
        when(webSocketNotifyService.truncate("课程「Vue进阶课」审核已通过")).thenReturn("课程「Vue进阶课」审核已通过");

        ReflectionTestUtils.setField(handler, "resourceAuditMapper", resourceAuditMapper);
        ReflectionTestUtils.setField(handler, "oshUserMapper", mock(OshUserMapper.class));
        ReflectionTestUtils.setField(handler, "webSocketNotifyService", webSocketNotifyService);
        ReflectionTestUtils.setField(handler, "auditAnnouncementPublisher", auditAnnouncementPublisher);
    }

    @Test
    public void shouldPublishAuditAnnouncementWhenDefaultResourceApproved() {
        ResourceAuditItemVO resource = buildResource(11L, "Vue进阶课", "9");
        when(resourceAuditMapper.selectAuditNotifyItem(ResourceTypeEnum.COURSE.getMysqlTableName(), 11L)).thenReturn(resource);

        handler.handle(buildContext(ResourceTypeEnum.COURSE, 11L, ResourceStatusEnum.PUBLISHED.getCode()));

        verify(auditAnnouncementPublisher).publishResult(ResourceTypeEnum.COURSE, 11L, "Vue进阶课", ResourceStatusEnum.PUBLISHED.getCode(), "/audit");
    }

    @Test
    public void shouldPublishAuditAnnouncementWhenDefaultResourceRejected() {
        ResourceAuditItemVO resource = buildResource(11L, "Vue进阶课", "9");
        when(resourceAuditMapper.selectAuditNotifyItem(ResourceTypeEnum.COURSE.getMysqlTableName(), 11L)).thenReturn(resource);

        handler.handle(buildContext(ResourceTypeEnum.COURSE, 11L, ResourceStatusEnum.OFF_SHELF.getCode()));

        verify(auditAnnouncementPublisher).publishResult(ResourceTypeEnum.COURSE, 11L, "Vue进阶课", ResourceStatusEnum.OFF_SHELF.getCode(), "/audit");
    }

    private ResourceAuditCallbackContext buildContext(ResourceTypeEnum resourceType, Long resourceId, Integer resourceStatus) {
        ResourceAuditCallbackContext context = new ResourceAuditCallbackContext();
        context.setResourceType(resourceType);
        context.setResourceId(resourceId);
        context.setResourceStatus(resourceStatus);
        return context;
    }

    private ResourceAuditItemVO buildResource(Long id, String title, String createBy) {
        ResourceAuditItemVO resource = new ResourceAuditItemVO();
        resource.setId(id);
        resource.setTitle(title);
        resource.setCreateBy(createBy);
        return resource;
    }
}
