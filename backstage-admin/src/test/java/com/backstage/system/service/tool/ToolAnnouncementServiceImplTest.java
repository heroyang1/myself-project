package com.backstage.system.service.tool;

import com.backstage.common.enums.AnnouncementChannelEnum;
import com.backstage.system.domain.vo.tool.ToolAnnouncementVO;
import com.backstage.system.mapper.tool.OshToolAnnouncementMapper;
import com.backstage.system.service.impl.tool.ToolAnnouncementServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ToolAnnouncementServiceImplTest {

    @InjectMocks
    private ToolAnnouncementServiceImpl toolAnnouncementService;

    @Mock
    private OshToolAnnouncementMapper oshToolAnnouncementMapper;

    @Test
    public void shouldQuerySystemNoticesBySystemChannel() {
        when(oshToolAnnouncementMapper.selectLatestToolAnnouncementsByChannel(AnnouncementChannelEnum.SYSTEM_NOTICE.getCode()))
                .thenReturn(Collections.singletonList(new ToolAnnouncementVO()));

        List<ToolAnnouncementVO> result = toolAnnouncementService.listLatestSystemNotices();

        assertTrue(result.size() == 1);
        verify(oshToolAnnouncementMapper).selectLatestToolAnnouncementsByChannel(AnnouncementChannelEnum.SYSTEM_NOTICE.getCode());
    }

    @Test
    public void shouldQueryNoticeAnnouncementsBySystemChannel() {
        when(oshToolAnnouncementMapper.selectLatestToolAnnouncementsByChannel(AnnouncementChannelEnum.SYSTEM_NOTICE.getCode()))
                .thenReturn(Collections.singletonList(new ToolAnnouncementVO()));

        List<ToolAnnouncementVO> result = toolAnnouncementService.listNoticeAnnouncements();

        assertTrue(result.size() == 1);
        verify(oshToolAnnouncementMapper).selectLatestToolAnnouncementsByChannel(AnnouncementChannelEnum.SYSTEM_NOTICE.getCode());
    }

    @Test
    public void shouldQueryUserNoticesByUserChannel() {
        when(oshToolAnnouncementMapper.selectLatestToolAnnouncementsByChannel(AnnouncementChannelEnum.USER_NOTICE.getCode()))
                .thenReturn(Collections.singletonList(new ToolAnnouncementVO()));

        List<ToolAnnouncementVO> result = toolAnnouncementService.listLatestUserNotices();

        assertTrue(result.size() == 1);
        verify(oshToolAnnouncementMapper).selectLatestToolAnnouncementsByChannel(AnnouncementChannelEnum.USER_NOTICE.getCode());
    }

    @Test
    public void shouldQueryDynamicAnnouncementsByUserChannel() {
        when(oshToolAnnouncementMapper.selectLatestToolAnnouncementsByChannel(AnnouncementChannelEnum.USER_NOTICE.getCode()))
                .thenReturn(Collections.singletonList(new ToolAnnouncementVO()));

        List<ToolAnnouncementVO> result = toolAnnouncementService.listDynamicAnnouncements();

        assertTrue(result.size() == 1);
        verify(oshToolAnnouncementMapper).selectLatestToolAnnouncementsByChannel(AnnouncementChannelEnum.USER_NOTICE.getCode());
    }
}
