package com.backstage.web.controller.tool;

import com.backstage.system.controller.tool.ToolAnnouncementController;
import com.backstage.system.domain.vo.tool.ToolAnnouncementVO;
import com.backstage.system.service.tool.IToolAnnouncementService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ToolAnnouncementControllerTest {

    private MockMvc mockMvc;
    private IToolAnnouncementService toolAnnouncementService;

    @Before
    public void setUp() {
        ToolAnnouncementController controller = new ToolAnnouncementController();
        toolAnnouncementService = mock(IToolAnnouncementService.class);
        ReflectionTestUtils.setField(controller, "toolAnnouncementService", toolAnnouncementService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void shouldReturnLatestSystemNotices() throws Exception {
        ToolAnnouncementVO item = new ToolAnnouncementVO();
        item.setId(1L);
        item.setTitle("系统通知");
        when(toolAnnouncementService.listLatestSystemNotices()).thenReturn(Collections.singletonList(item));

        mockMvc.perform(get("/pc/tool/announcement/systemNotice/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].title").value("系统通知"));

        verify(toolAnnouncementService).listLatestSystemNotices();
    }

    @Test
    public void shouldReturnNoticeAnnouncements() throws Exception {
        ToolAnnouncementVO item = new ToolAnnouncementVO();
        item.setId(1L);
        item.setTitle("系统公告");
        when(toolAnnouncementService.listNoticeAnnouncements()).thenReturn(Collections.singletonList(item));

        mockMvc.perform(get("/pc/tool/announcement/notice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].title").value("系统公告"));

        verify(toolAnnouncementService).listNoticeAnnouncements();
    }

    @Test
    public void shouldReturnLatestUserNotices() throws Exception {
        ToolAnnouncementVO item = new ToolAnnouncementVO();
        item.setId(2L);
        item.setTitle("业务公告");
        when(toolAnnouncementService.listLatestUserNotices()).thenReturn(Collections.singletonList(item));

        mockMvc.perform(get("/pc/tool/announcement/userNotice/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].title").value("业务公告"));

        verify(toolAnnouncementService).listLatestUserNotices();
    }

    @Test
    public void shouldReturnDynamicAnnouncements() throws Exception {
        ToolAnnouncementVO item = new ToolAnnouncementVO();
        item.setId(2L);
        item.setTitle("业务动态");
        when(toolAnnouncementService.listDynamicAnnouncements()).thenReturn(Collections.singletonList(item));

        mockMvc.perform(get("/pc/tool/announcement/dynamic"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].title").value("业务动态"));

        verify(toolAnnouncementService).listDynamicAnnouncements();
    }
}
