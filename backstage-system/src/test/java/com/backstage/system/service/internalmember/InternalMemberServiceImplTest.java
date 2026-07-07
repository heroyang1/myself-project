package com.backstage.system.service.internalmember;

import com.backstage.common.exception.ServiceException;
import com.backstage.system.domain.internalmember.dto.InternalMemberFollowRecordSaveDTO;
import com.backstage.system.domain.internalmember.vo.InternalMemberAdminUserOptionVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberEmploymentProfileVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberOutcomeVO;
import com.backstage.system.mapper.internalmember.InternalMemberMapper;
import com.backstage.system.service.internalmember.impl.InternalMemberServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class InternalMemberServiceImplTest {

    @InjectMocks
    private InternalMemberServiceImpl internalMemberService;

    @Mock
    private InternalMemberMapper internalMemberMapper;

    @Test
    public void shouldTrimKeywordWhenSelectingAdminUserOptions() {
        InternalMemberAdminUserOptionVO first = new InternalMemberAdminUserOptionVO();
        first.setUserId(101L);
        first.setUsername("alice");
        first.setRoleCode("manager");

        InternalMemberAdminUserOptionVO second = new InternalMemberAdminUserOptionVO();
        second.setUserId(102L);
        second.setUsername("bob");
        second.setRoleCode("core_developer");

        when(internalMemberMapper.selectAdminUserOptions(eq("alice"))).thenReturn(Arrays.asList(first, second));

        List<InternalMemberAdminUserOptionVO> result = internalMemberService.selectAdminUserOptions("  alice  ");

        assertEquals(2, result.size());
        assertEquals(Long.valueOf(101L), result.get(0).getUserId());
        assertEquals("manager", result.get(0).getRoleCode());
        assertEquals("core_developer", result.get(1).getRoleCode());
    }

    @Test
    public void shouldFillProgressStageForEmploymentProfile() {
        InternalMemberEmploymentProfileVO profile = new InternalMemberEmploymentProfileVO();
        profile.setJobStatus(1);
        profile.setResumeDeliveryCount(12);
        profile.setInterviewCompanyCount(3);
        profile.setInterviewStatus("interviewing");

        when(internalMemberMapper.countPaidMemberByUserId(9L)).thenReturn(1);
        when(internalMemberMapper.selectEmploymentProfileByUserId(9L)).thenReturn(profile);

        InternalMemberEmploymentProfileVO result = internalMemberService.selectEmploymentProfile(9L);

        assertEquals("interviewing", result.getProgressStageCode());
        assertEquals(Integer.valueOf(4), result.getProgressStageOrder());
        assertEquals(Integer.valueOf(6), result.getProgressStageTotal());
    }

    @Test
    public void shouldKeepNullEmploymentProfileWhenRecordDoesNotExist() {
        when(internalMemberMapper.countPaidMemberByUserId(9L)).thenReturn(1);
        when(internalMemberMapper.selectEmploymentProfileByUserId(9L)).thenReturn(null);

        InternalMemberEmploymentProfileVO result = internalMemberService.selectEmploymentProfile(9L);

        assertNull(result);
    }

    @Test
    public void shouldFillProgressStageForOutcomeList() {
        InternalMemberOutcomeVO item = new InternalMemberOutcomeVO();
        item.setJobStatus(1);
        item.setResumeDeliveryCount(5);

        when(internalMemberMapper.selectOutcomeList(any())).thenReturn(Collections.singletonList(item));

        List<InternalMemberOutcomeVO> result = internalMemberService.selectOutcomeList(null);

        assertEquals(1, result.size());
        assertEquals("delivering", result.get(0).getProgressStageCode());
        assertEquals(Integer.valueOf(3), result.get(0).getProgressStageOrder());
        assertEquals(Integer.valueOf(6), result.get(0).getProgressStageTotal());
    }

    @Test
    public void shouldAddFollowRecordWithResponsibleUserId() {
        InternalMemberFollowRecordSaveDTO dto = new InternalMemberFollowRecordSaveDTO();
        dto.setFollowType("  wechat_follow  ");
        dto.setContent("  follow content  ");
        dto.setNextFollowTime(" 2026-07-05 12:00:00 ");
        dto.setResponsibleUserId(101L);

        when(internalMemberMapper.countPaidMemberByUserId(2001L)).thenReturn(1);
        when(internalMemberMapper.countInternalMemberAdminUserByUserId(101L)).thenReturn(1);
        when(internalMemberMapper.insertFollowRecord(
                eq(2001L),
                eq("wechat_follow"),
                eq("follow content"),
                eq("2026-07-05 12:00:00"),
                eq(9001L),
                eq(101L))).thenReturn(1);

        internalMemberService.addFollowRecord(2001L, dto, 9001L);

        verify(internalMemberMapper).insertFollowRecord(
                2001L,
                "wechat_follow",
                "follow content",
                "2026-07-05 12:00:00",
                9001L,
                101L);
    }

    @Test
    public void shouldRejectWhenResponsibleUserIdIsMissing() {
        InternalMemberFollowRecordSaveDTO dto = new InternalMemberFollowRecordSaveDTO();
        dto.setFollowType("wechat_follow");
        dto.setContent("follow content");

        when(internalMemberMapper.countPaidMemberByUserId(2001L)).thenReturn(1);

        try {
            internalMemberService.addFollowRecord(2001L, dto, 9001L);
            fail("Expected ServiceException");
        } catch (ServiceException ex) {
            assertEquals("Responsible admin user id cannot be empty", ex.getMessage());
        }
    }

    @Test
    public void shouldRejectWhenResponsibleUserIsNotInternalMemberAdmin() {
        InternalMemberFollowRecordSaveDTO dto = new InternalMemberFollowRecordSaveDTO();
        dto.setFollowType("wechat_follow");
        dto.setContent("follow content");
        dto.setResponsibleUserId(202L);

        when(internalMemberMapper.countPaidMemberByUserId(2001L)).thenReturn(1);
        when(internalMemberMapper.countInternalMemberAdminUserByUserId(202L)).thenReturn(0);

        try {
            internalMemberService.addFollowRecord(2001L, dto, 9001L);
            fail("Expected ServiceException");
        } catch (ServiceException ex) {
            assertEquals("Responsible admin user is invalid", ex.getMessage());
        }
    }

}
