package com.backstage.system.service.internalmember;

import com.backstage.common.exception.ServiceException;
import com.backstage.system.domain.internalmember.dto.InternalMemberFollowRecordSaveDTO;
import com.backstage.system.domain.internalmember.dto.InternalMemberFollowTaskCompleteDTO;
import com.backstage.system.domain.internalmember.dto.InternalMemberFollowTaskSaveDTO;
import com.backstage.system.domain.internalmember.vo.InternalMemberAdminUserOptionVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberEmploymentProfileVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberFollowTaskReminderVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberFollowTaskVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberOutcomeVO;
import com.backstage.system.enums.internalmember.InternalMemberFollowTaskStatusEnum;
import com.backstage.common.utils.email.EmailUtil;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class InternalMemberServiceImplTest {

    @InjectMocks
    private InternalMemberServiceImpl internalMemberService;

    @Mock
    private InternalMemberMapper internalMemberMapper;
    @Mock
    private EmailUtil emailUtil;

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

    @Test
    public void shouldCreateFollowTask() {
        InternalMemberFollowTaskSaveDTO dto = new InternalMemberFollowTaskSaveDTO();
        dto.setContent("  reminder content  ");
        dto.setResponsibleUserId(101L);
        dto.setPlannedFollowTime("2099-07-10 15:30:00");
        dto.setRemindTime("2099-07-09 15:30:00");

        when(internalMemberMapper.countPaidMemberByUserId(2001L)).thenReturn(1);
        when(internalMemberMapper.countInternalMemberAdminUserByUserId(101L)).thenReturn(1);
        when(internalMemberMapper.insertFollowTask(
                eq(2001L),
                eq(dto),
                eq("pending"),
                eq("email"),
                eq(9001L))).thenReturn(1);

        internalMemberService.addFollowTask(2001L, dto, 9001L);

        assertEquals("reminder content", dto.getContent());
        verify(internalMemberMapper).insertFollowTask(2001L, dto, "pending", "email", 9001L);
    }

    @Test
    public void shouldCompleteFollowTaskAndAppendFollowRecord() {
        InternalMemberFollowTaskVO task = new InternalMemberFollowTaskVO();
        task.setId(3001L);
        task.setUserId(2001L);
        task.setContent("Need follow up");
        task.setResponsibleUserId(101L);
        task.setStatus(InternalMemberFollowTaskStatusEnum.NOTIFIED.getCode());

        InternalMemberFollowTaskCompleteDTO dto = new InternalMemberFollowTaskCompleteDTO();
        dto.setResultNote("Reached user and closed");

        when(internalMemberMapper.selectFollowTaskById(3001L)).thenReturn(task);
        when(internalMemberMapper.countPaidMemberByUserId(2001L)).thenReturn(1);
        when(internalMemberMapper.updateFollowTaskStatus(
                eq(3001L),
                eq("completed"),
                eq("Reached user and closed"),
                any(String.class),
                eq(9001L),
                eq(9001L))).thenReturn(1);
        when(internalMemberMapper.insertFollowRecord(
                eq(2001L),
                eq("follow_task"),
                eq("完成跟进任务: Need follow up；完成说明: Reached user and closed"),
                eq(null),
                eq(9001L),
                eq(101L))).thenReturn(1);

        internalMemberService.completeFollowTask(3001L, dto, 9001L);

        verify(internalMemberMapper).insertFollowRecord(
                2001L,
                "follow_task",
                "完成跟进任务: Need follow up；完成说明: Reached user and closed",
                null,
                9001L,
                101L);
    }

    @Test
    public void shouldSendReminderEmailAndMarkTaskNotified() throws Exception {
        InternalMemberFollowTaskReminderVO task = new InternalMemberFollowTaskReminderVO();
        task.setId(3002L);
        task.setUserId(2001L);
        task.setContent("Reminder");
        task.setResponsibleUserId(101L);
        task.setResponsibleUserName("alice");
        task.setResponsibleUserEmail("alice@example.test");
        task.setPlannedFollowTime("2099-07-10 10:00:00");
        task.setRemindTime("2099-07-09 10:00:00");
        task.setStatus("pending");

        when(internalMemberMapper.selectDueFollowTaskReminderList()).thenReturn(Collections.singletonList(task));
        when(internalMemberMapper.markFollowTaskReminderSent(3002L, 0L)).thenReturn(1);

        internalMemberService.processDueFollowTaskReminders();

        verify(emailUtil).sendEmailCommonTo(eq("alice@example.test"), eq("【内推会员跟进提醒】请及时跟进用户"), any(String.class));
        verify(internalMemberMapper).markFollowTaskReminderSent(3002L, 0L);
    }

    @Test
    public void shouldSkipReminderWhenResponsibleAdminHasNoEmail() throws Exception {
        InternalMemberFollowTaskReminderVO task = new InternalMemberFollowTaskReminderVO();
        task.setId(3003L);
        task.setUserId(2001L);
        task.setContent("Reminder");
        task.setResponsibleUserId(101L);
        task.setResponsibleUserEmail(null);
        task.setPlannedFollowTime("2099-07-10 10:00:00");
        task.setRemindTime("2099-07-09 10:00:00");
        task.setStatus("pending");

        when(internalMemberMapper.selectDueFollowTaskReminderList()).thenReturn(Collections.singletonList(task));

        internalMemberService.processDueFollowTaskReminders();

        verify(emailUtil, never()).sendEmailCommonTo(any(String.class), any(String.class), any(String.class));
        verify(internalMemberMapper, never()).markFollowTaskReminderSent(any(Long.class), any(Long.class));
    }

}
