package com.backstage.system.service.audit;

import com.backstage.common.enums.AnnouncementChannelEnum;
import com.backstage.common.enums.ResourceStatusEnum;
import com.backstage.common.enums.ResourceTypeEnum;
import com.backstage.system.domain.audit.ResourceAuditItemVO;
import com.backstage.system.mapper.audit.ResourceAuditMapper;
import com.backstage.system.mapper.tool.OshToolAnnouncementMapper;
import com.backstage.system.mapper.user.OshUserMapper;
import com.backstage.system.service.impl.audit.ToolResourceAuditCallbackHandler;
import com.backstage.system.service.websocket.WebSocketNotifyService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ToolResourceAuditCallbackHandlerTest {

    private ToolResourceAuditCallbackHandler handler;
    private ResourceAuditMapper resourceAuditMapper;
    private OshToolAnnouncementMapper oshToolAnnouncementMapper;
    private AuditAnnouncementPublisher auditAnnouncementPublisher;
    private WebSocketNotifyService webSocketNotifyService;

    @Before
    public void setUp() {
        handler = new ToolResourceAuditCallbackHandler();
        resourceAuditMapper = mock(ResourceAuditMapper.class);
        oshToolAnnouncementMapper = mock(OshToolAnnouncementMapper.class);
        auditAnnouncementPublisher = mock(AuditAnnouncementPublisher.class);
        webSocketNotifyService = mock(WebSocketNotifyService.class);
        when(webSocketNotifyService.truncate("工具「图片压缩」审核已通过")).thenReturn("工具「图片压缩」审核已通过");

        ReflectionTestUtils.setField(handler, "resourceAuditMapper", resourceAuditMapper);
        ReflectionTestUtils.setField(handler, "oshUserMapper", mock(OshUserMapper.class));
        ReflectionTestUtils.setField(handler, "oshToolAnnouncementMapper", oshToolAnnouncementMapper);
        ReflectionTestUtils.setField(handler, "webSocketNotifyService", webSocketNotifyService);
        ReflectionTestUtils.setField(handler, "auditAnnouncementPublisher", auditAnnouncementPublisher);
    }

    @Test
    public void shouldPublishToolAnnouncementAndAuditAnnouncementWhenToolApproved() {
        ResourceAuditItemVO tool = buildTool(1001L, "图片压缩", "9", null);
        when(resourceAuditMapper.selectAuditNotifyItem(ResourceTypeEnum.TOOL.getMysqlTableName(), 1001L)).thenReturn(tool);

        handler.handle(buildContext(1001L, ResourceStatusEnum.PUBLISHED.getCode()));

        verify(oshToolAnnouncementMapper).insertToolAnnouncement(
                "工具上新：「图片压缩」已上线",
                "/tool?toolId=1001&autoOpen=1",
                AnnouncementChannelEnum.SYSTEM_NOTICE.getCode(),
                "online",
                "system"
        );
        verify(auditAnnouncementPublisher).publishResult(ResourceTypeEnum.TOOL, 1001L, "图片压缩", ResourceStatusEnum.PUBLISHED.getCode(), "/tool?toolId=1001&autoOpen=1");
    }

    @Test
    public void shouldPublishAuditAnnouncementOnlyWhenToolRejected() {
        ResourceAuditItemVO tool = buildTool(1001L, "图片压缩", "9", null);
        when(resourceAuditMapper.selectAuditNotifyItem(ResourceTypeEnum.TOOL.getMysqlTableName(), 1001L)).thenReturn(tool);

        handler.handle(buildContext(1001L, ResourceStatusEnum.OFF_SHELF.getCode()));

        verify(auditAnnouncementPublisher).publishResult(ResourceTypeEnum.TOOL, 1001L, "图片压缩", ResourceStatusEnum.OFF_SHELF.getCode(), "/tool?toolId=1001&autoOpen=1");
        verify(oshToolAnnouncementMapper, never()).insertToolAnnouncement(
                "工具上新：「图片压缩」已上线",
                "/tool?toolId=1001&autoOpen=1",
                AnnouncementChannelEnum.SYSTEM_NOTICE.getCode(),
                "online",
                "system"
        );
    }

    private ResourceAuditCallbackContext buildContext(Long resourceId, Integer resourceStatus) {
        ResourceAuditCallbackContext context = new ResourceAuditCallbackContext();
        context.setResourceType(ResourceTypeEnum.TOOL);
        context.setResourceId(resourceId);
        context.setResourceStatus(resourceStatus);
        return context;
    }

    private ResourceAuditItemVO buildTool(Long id, String title, String createBy, String url) {
        ResourceAuditItemVO resource = new ResourceAuditItemVO();
        resource.setId(id);
        resource.setTitle(title);
        resource.setCreateBy(createBy);
        resource.setUrl(url);
        return resource;
    }
}
