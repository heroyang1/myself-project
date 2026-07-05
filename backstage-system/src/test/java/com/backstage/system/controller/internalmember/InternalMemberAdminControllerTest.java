package com.backstage.system.controller.internalmember;

import com.backstage.system.domain.internalmember.vo.InternalMemberAdminUserOptionVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberEmploymentProfileVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberOutcomeVO;
import com.backstage.system.service.internalmember.IInternalMemberService;
import com.backstage.system.service.internalmember.IInternalMemberStudyService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
