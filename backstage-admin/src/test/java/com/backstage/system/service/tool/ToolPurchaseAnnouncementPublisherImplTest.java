package com.backstage.system.service.tool;

import com.backstage.common.async.AsyncTaskSupport;
import com.backstage.common.enums.AnnouncementChannelEnum;
import com.backstage.system.domain.tool.OshToolPurchaseRecord;
import com.backstage.system.domain.user.OshUser;
import com.backstage.system.domain.websocket.WsNotifyMessage;
import com.backstage.system.mapper.tool.OshToolAnnouncementMapper;
import com.backstage.system.mapper.user.OshUserMapper;
import com.backstage.system.service.impl.tool.ToolPurchaseAnnouncementPublisherImpl;
import com.backstage.system.service.websocket.WebSocketNotifyService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ToolPurchaseAnnouncementPublisherImplTest {

    @InjectMocks
    private ToolPurchaseAnnouncementPublisherImpl publisher;

    @Mock
    private OshToolAnnouncementMapper oshToolAnnouncementMapper;

    @Mock
    private OshUserMapper oshUserMapper;

    @Mock
    private WebSocketNotifyService webSocketNotifyService;

    @Mock
    private AsyncTaskSupport asyncTaskSupport;

    private Executor directExecutor;

    @Before
    public void setUp() {
        directExecutor = new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        };
        ReflectionTestUtils.setField(publisher, "notificationTaskExecutor", directExecutor);
        when(asyncTaskSupport.runAsync(any(Runnable.class), eq(directExecutor))).thenAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return CompletableFuture.completedFuture(null);
        });
    }

    @Test
    public void shouldWriteDynamicAnnouncementAndBroadcastRefreshAfterPurchaseSuccess() {
        OshToolPurchaseRecord record = new OshToolPurchaseRecord();
        record.setOrderNo("O20260624001");
        record.setUserId(9L);
        record.setPackageId(2001L);
        record.setPackageNameSnapshot("体验包");
        OshUser user = new OshUser();
        user.setUsername("alice");
        when(oshUserMapper.selectUserById(9L)).thenReturn(user);

        publisher.publishPurchaseSuccess(record);

        verify(oshToolAnnouncementMapper).insertToolAnnouncement(
                eq("alice购买了「工具点数 体验包」"),
                eq("/tool/quota"),
                eq(AnnouncementChannelEnum.USER_NOTICE.getCode()),
                eq("payment"),
                eq("system")
        );

        ArgumentCaptor<WsNotifyMessage> broadcastCaptor = ArgumentCaptor.forClass(WsNotifyMessage.class);
        verify(webSocketNotifyService).broadcast(broadcastCaptor.capture());
        WsNotifyMessage broadcast = broadcastCaptor.getValue();
        assertEquals("ANNOUNCEMENT_REFRESH", broadcast.getType());
        assertEquals("tool", broadcast.getBizId());
        assertTrue(broadcast.getContent().contains("\"module\":\"tool\""));
        assertTrue(broadcast.getContent().contains("\"action\":\"refresh\""));
        assertTrue(broadcast.getContent().contains("\"refresh\":true"));
        assertTrue(broadcast.getContent().contains("\"noticeApi\":\"/pc/tool/announcement/notice\""));
        assertTrue(broadcast.getContent().contains("\"dynamicApi\":\"/pc/tool/announcement/dynamic\""));
    }
}
