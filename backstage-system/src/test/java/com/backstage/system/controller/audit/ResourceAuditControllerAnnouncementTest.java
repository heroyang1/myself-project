package com.backstage.system.controller.audit;

import com.backstage.common.enums.AnnouncementChannelEnum;
import com.backstage.common.enums.AnnouncementModuleEnum;
import com.backstage.system.domain.announcement.vo.AnnouncementMarqueeVO;
import com.backstage.system.service.announcement.IAnnouncementMarqueeQueryService;
import com.backstage.system.service.audit.IResourceAuditService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ResourceAuditControllerAnnouncementTest {

    private ResourceAuditController controller;
    private IAnnouncementMarqueeQueryService announcementMarqueeQueryService;

    @Before
    public void setUp() {
        controller = new ResourceAuditController();
        ReflectionTestUtils.setField(controller, "resourceAuditService", mock(IResourceAuditService.class));
        announcementMarqueeQueryService = mock(IAnnouncementMarqueeQueryService.class);
        ReflectionTestUtils.setField(controller, "announcementMarqueeQueryService", announcementMarqueeQueryService);
    }

    @Test
    public void shouldReturnAuditNoticeAnnouncements() {
        AnnouncementMarqueeVO item = new AnnouncementMarqueeVO();
        item.setId(1L);
        item.setTitle("审核系统通知");
        when(announcementMarqueeQueryService.list(
                AnnouncementModuleEnum.AUDIT,
                AnnouncementChannelEnum.SYSTEM_NOTICE,
                10
        )).thenReturn(Collections.singletonList(item));

        assertEquals("审核系统通知", controller.listNoticeAnnouncements(10).getData().get(0).getTitle());
        verify(announcementMarqueeQueryService).list(
                AnnouncementModuleEnum.AUDIT,
                AnnouncementChannelEnum.SYSTEM_NOTICE,
                10
        );
    }

    @Test
    public void shouldReturnAuditDynamicAnnouncements() {
        AnnouncementMarqueeVO item = new AnnouncementMarqueeVO();
        item.setId(2L);
        item.setTitle("审核业务动态");
        when(announcementMarqueeQueryService.list(
                AnnouncementModuleEnum.AUDIT,
                AnnouncementChannelEnum.USER_NOTICE,
                10
        )).thenReturn(Collections.singletonList(item));

        assertEquals("审核业务动态", controller.listDynamicAnnouncements(10).getData().get(0).getTitle());
        verify(announcementMarqueeQueryService).list(
                AnnouncementModuleEnum.AUDIT,
                AnnouncementChannelEnum.USER_NOTICE,
                10
        );
    }
}
