package com.backstage.system.controller.internalmember;

import com.backstage.common.constant.OshUserConstants;
import com.backstage.common.threadlocal.ThreadLocalUtil;
import com.backstage.system.domain.internalmember.dto.InternalMemberFollowRecordSaveDTO;
import com.backstage.system.domain.internalmember.dto.InternalMemberFollowTaskSaveDTO;
import com.backstage.system.domain.internalmember.vo.InternalMemberAdminUserOptionVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberEmploymentProfileVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberFollowRecordVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberFollowTaskVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberOutcomeVO;
import com.backstage.system.service.internalmember.IInternalMemberService;
import com.backstage.system.service.internalmember.IInternalMemberStudyService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class InternalMemberAdminControllerTest {

    private MockMvc mockMvc;
    private IInternalMemberService internalMemberService;

    @Before
    public void setUp() {
        InternalMemberAdminController controller = new InternalMemberAdminController();
        internalMemberService = mock(IInternalMemberService.class);
        ReflectionTestUtils.setField(controller, "internalMemberService", internalMemberService);
        ReflectionTestUtils.setField(controller, "internalMemberStudyService", mock(IInternalMemberStudyService.class));
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @After
    public void tearDown() {
        ThreadLocalUtil.remove();
    }

    @Test
    public void shouldReturnEmploymentProfileWithProgressStageFields() throws Exception {
        InternalMemberEmploymentProfileVO profile = new InternalMemberEmploymentProfileVO();
        profile.setProgressStageCode("offer_pending");
        profile.setProgressStageName("Offer Pending");
        profile.setProgressStageOrder(5);
        profile.setProgressStageTotal(6);

        when(internalMemberService.selectEmploymentProfile(eq(9L))).thenReturn(profile);

        mockMvc.perform(get("/pc/admin/internal-member/9/employment-profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.progressStageCode").value("offer_pending"))
                .andExpect(jsonPath("$.data.progressStageName").value("Offer Pending"))
                .andExpect(jsonPath("$.data.progressStageOrder").value(5))
                .andExpect(jsonPath("$.data.progressStageTotal").value(6));
    }

    @Test
    public void shouldReturnAdminUserOptions() throws Exception {
        InternalMemberAdminUserOptionVO first = new InternalMemberAdminUserOptionVO();
        first.setUserId(101L);
        first.setUsername("alice");
        first.setRoleCode("manager");
        first.setRoleName("Manager");
        first.setRoleLevel(4);

        InternalMemberAdminUserOptionVO second = new InternalMemberAdminUserOptionVO();
        second.setUserId(102L);
        second.setUsername("bob");
        second.setRoleCode("founder");
        second.setRoleName("Founder");
        second.setRoleLevel(6);

        when(internalMemberService.selectAdminUserOptions(eq("ali"))).thenReturn(Arrays.asList(first, second));

        mockMvc.perform(get("/pc/admin/internal-member/admin-users")
                        .param("keyword", "ali"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].userId").value(101))
                .andExpect(jsonPath("$.data[0].username").value("alice"))
                .andExpect(jsonPath("$.data[0].roleCode").value("manager"))
                .andExpect(jsonPath("$.data[1].roleCode").value("founder"));
    }

    @Test
    public void shouldReturnFollowRecordsWithResponsibleFields() throws Exception {
        InternalMemberFollowRecordVO record = new InternalMemberFollowRecordVO();
        record.setId(1L);
        record.setUserId(2001L);
        record.setFollowType("wechat_follow");
        record.setContent("follow content");
        record.setOperatorId(9001L);
        record.setOperatorName("current-admin");
        record.setResponsibleUserId(101L);
        record.setResponsibleUserName("alice");
        record.setNextFollowTime("2026-07-05 12:00:00");
        record.setCreateTime("2026-07-05 10:00:00");

        when(internalMemberService.selectFollowRecordList(eq(2001L))).thenReturn(Collections.singletonList(record));

        mockMvc.perform(get("/pc/admin/internal-member/2001/follow-records"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].operatorId").value(9001))
                .andExpect(jsonPath("$.data[0].operatorName").value("current-admin"))
                .andExpect(jsonPath("$.data[0].responsibleUserId").value(101))
                .andExpect(jsonPath("$.data[0].responsibleUserName").value("alice"));
    }

    @Test
    public void shouldPassResponsibleUserIdWhenAddingFollowRecord() throws Exception {
        ThreadLocalUtil.set(OshUserConstants.USER_ID, 9001L);

        mockMvc.perform(post("/pc/admin/internal-member/2001/follow-records")
                        .contentType(APPLICATION_JSON)
                        .content("{\"followType\":\"wechat_follow\",\"content\":\"follow content\",\"nextFollowTime\":\"2026-07-05 12:00:00\",\"responsibleUserId\":101}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<InternalMemberFollowRecordSaveDTO> dtoCaptor =
                ArgumentCaptor.forClass(InternalMemberFollowRecordSaveDTO.class);
        verify(internalMemberService).addFollowRecord(eq(2001L), dtoCaptor.capture(), eq(9001L));
        Assert.assertEquals(Long.valueOf(101L), dtoCaptor.getValue().getResponsibleUserId());
    }

    @Test
    public void shouldReturnFollowTasks() throws Exception {
        InternalMemberFollowTaskVO task = new InternalMemberFollowTaskVO();
        task.setId(11L);
        task.setUserId(2001L);
        task.setContent("need call back");
        task.setResponsibleUserId(101L);
        task.setResponsibleUserName("alice");
        task.setStatus("pending");
        task.setStatusName("待跟进");
        task.setNotifySent(0);

        when(internalMemberService.selectFollowTaskList(eq(2001L))).thenReturn(Collections.singletonList(task));

        mockMvc.perform(get("/pc/admin/internal-member/2001/follow-tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value(11))
                .andExpect(jsonPath("$.data[0].status").value("pending"))
                .andExpect(jsonPath("$.data[0].statusName").value("待跟进"));
    }

    @Test
    public void shouldPassFollowTaskPayloadWhenCreatingTask() throws Exception {
        ThreadLocalUtil.set(OshUserConstants.USER_ID, 9001L);

        mockMvc.perform(post("/pc/admin/internal-member/2001/follow-tasks")
                        .contentType(APPLICATION_JSON)
                        .content("{\"content\":\"need callback\",\"responsibleUserId\":101,\"plannedFollowTime\":\"2099-07-10 12:00:00\",\"remindTime\":\"2099-07-09 12:00:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<InternalMemberFollowTaskSaveDTO> dtoCaptor =
                ArgumentCaptor.forClass(InternalMemberFollowTaskSaveDTO.class);
        verify(internalMemberService).addFollowTask(eq(2001L), dtoCaptor.capture(), eq(9001L));
        Assert.assertEquals(Long.valueOf(101L), dtoCaptor.getValue().getResponsibleUserId());
        Assert.assertEquals("need callback", dtoCaptor.getValue().getContent());
    }

    @Test
    public void shouldPassTaskIdWhenCompletingFollowTask() throws Exception {
        ThreadLocalUtil.set(OshUserConstants.USER_ID, 9001L);

        mockMvc.perform(post("/pc/admin/internal-member/follow-tasks/11/complete")
                        .contentType(APPLICATION_JSON)
                        .content("{\"resultNote\":\"done\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(internalMemberService).completeFollowTask(eq(11L), any(), eq(9001L));
    }

    @Test
    public void shouldReturnOutcomePageWithProgressStageFields() throws Exception {
        InternalMemberOutcomeVO item = new InternalMemberOutcomeVO();
        item.setProgressStageCode("interviewing");
        item.setProgressStageName("Interviewing");
        item.setProgressStageOrder(4);
        item.setProgressStageTotal(6);

        when(internalMemberService.selectOutcomeList(any())).thenReturn(Collections.singletonList(item));

        mockMvc.perform(get("/pc/admin/internal-member/outcome/page")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows[0].progressStageCode").value("interviewing"))
                .andExpect(jsonPath("$.rows[0].progressStageName").value("Interviewing"))
                .andExpect(jsonPath("$.rows[0].progressStageOrder").value(4))
                .andExpect(jsonPath("$.rows[0].progressStageTotal").value(6));
    }
}
