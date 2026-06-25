package com.backstage.system.service.audit;

import com.backstage.common.enums.AnnouncementChannelEnum;
import com.backstage.common.enums.ResourceStatusEnum;
import com.backstage.common.enums.ResourceTypeEnum;
import com.backstage.system.domain.websocket.WsNotifyMessage;
import com.backstage.system.mapper.announcement.OshAnnouncementMapper;
import com.backstage.system.service.impl.audit.AuditAnnouncementPublisherImpl;
import com.backstage.system.service.websocket.WebSocketNotifyService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class AuditAnnouncementPublisherImplTest {

    @InjectMocks
    private AuditAnnouncementPublisherImpl publisher;

    @Mock
    private OshAnnouncementMapper announcementMapper;

    @Mock
    private WebSocketNotifyService webSocketNotifyService;

    @Test
    public void shouldWriteApprovedAuditAnnouncementAndBroadcastRefresh() {
        publisher.publishResult(ResourceTypeEnum.TOOL, 1001L, "图片压缩", ResourceStatusEnum.PUBLISHED.getCode(), "/tool?toolId=1001&autoOpen=1");

        verify(announcementMapper).insertAuditAnnouncement(
                "工具模块的「图片压缩」审核通过",
                "/tool?toolId=1001&autoOpen=1",
                "audit",
                ResourceTypeEnum.TOOL.getType(),
                1001L,
                AnnouncementChannelEnum.SYSTEM_NOTICE.getCode(),
                "system"
        );

        ArgumentCaptor<WsNotifyMessage> captor = ArgumentCaptor.forClass(WsNotifyMessage.class);
        verify(webSocketNotifyService).broadcast(captor.capture());
        WsNotifyMessage message = captor.getValue();
        assertEquals("ANNOUNCEMENT_REFRESH", message.getType());
        assertEquals("audit", message.getBizId());
        assertTrue(message.getContent().contains("\"module\":\"audit\""));
        assertTrue(message.getContent().contains("\"action\":\"refresh\""));
        assertTrue(message.getContent().contains("\"refresh\":true"));
        assertTrue(message.getContent().contains("\"noticeApi\":\"/pc/audit/announcement/notice\""));
        assertTrue(message.getContent().contains("\"dynamicApi\":\"/pc/audit/announcement/dynamic\""));
    }

    @Test
    public void shouldWriteRejectedAuditAnnouncementAndBroadcastRefresh() {
        publisher.publishResult(ResourceTypeEnum.TOOL, 1001L, "图片压缩", ResourceStatusEnum.OFF_SHELF.getCode(), "/audit");

        verify(announcementMapper).insertAuditAnnouncement(
                "工具模块的「图片压缩」审核不通过",
                "/audit",
                "audit",
                ResourceTypeEnum.TOOL.getType(),
                1001L,
                AnnouncementChannelEnum.SYSTEM_NOTICE.getCode(),
                "system"
        );

        ArgumentCaptor<WsNotifyMessage> captor = ArgumentCaptor.forClass(WsNotifyMessage.class);
        verify(webSocketNotifyService).broadcast(captor.capture());
        assertEquals("audit", captor.getValue().getBizId());
    }
}
