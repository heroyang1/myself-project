package com.backstage.system.service.internalmember.impl;

import com.backstage.common.exception.ServiceException;
import com.backstage.common.utils.StringUtils;
import com.backstage.system.domain.internalmember.dto.InternalMemberCourseDetailQueryDTO;
import com.backstage.system.domain.internalmember.dto.InternalMemberEmploymentProfileSaveDTO;
import com.backstage.system.domain.internalmember.dto.InternalMemberFollowRecordSaveDTO;
import com.backstage.system.domain.internalmember.dto.InternalMemberOutcomeQueryDTO;
import com.backstage.system.domain.internalmember.dto.InternalMemberPageQueryDTO;
import com.backstage.system.domain.internalmember.vo.InternalMemberAdminUserOptionVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberCourseProgressVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberExamItemVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberExamSummaryVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberEmploymentProfileVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberFollowRecordVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberInterviewSummaryVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberOutcomeVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberResumeSummaryVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberSalarySummaryVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberStudyOverviewVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberStudySummaryVO;
import com.backstage.system.enums.internalmember.InternalMemberProgressStageEnum;
import com.backstage.system.enums.internalmember.InterviewResultEnum;
import com.backstage.system.enums.internalmember.InterviewStatusEnum;
import com.backstage.system.enums.internalmember.JobStatusEnum;
import com.backstage.system.mapper.internalmember.InternalMemberMapper;
import com.backstage.system.service.internalmember.IInternalMemberService;
import com.backstage.system.service.internalmember.support.InternalMemberProgressStageResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class InternalMemberServiceImpl implements IInternalMemberService {
    @Resource
    private InternalMemberMapper internalMemberMapper;

    @Override
    public List<InternalMemberAdminUserOptionVO> selectAdminUserOptions(String keyword) {
        return internalMemberMapper.selectAdminUserOptions(StringUtils.trim(keyword));
    }

    @Override
    public List<InternalMemberStudyOverviewVO> selectPaidMemberStudyOverview(InternalMemberPageQueryDTO query) {
        return internalMemberMapper.selectPaidMemberStudyOverview(query);
    }

    @Override
    public InternalMemberStudySummaryVO selectMemberStudySummary(Long userId) {
        ensurePaidMember(userId);
        InternalMemberStudySummaryVO summary = internalMemberMapper.selectMemberStudySummary(userId);
        if (summary == null) {
            throw new ServiceException("Paid member record not found");
        }
        return summary;
    }

    @Override
    public List<InternalMemberCourseProgressVO> selectMemberCourseProgressList(Long userId,
                                                                               InternalMemberCourseDetailQueryDTO query) {
        ensurePaidMember(userId);
        return internalMemberMapper.selectMemberCourseProgressList(userId, query);
    }

    @Override
    public InternalMemberEmploymentProfileVO selectEmploymentProfile(Long userId) {
        ensurePaidMember(userId);
        InternalMemberEmploymentProfileVO profile = internalMemberMapper.selectEmploymentProfileByUserId(userId);
        fillProgressStage(profile);
        return profile;
    }

    @Override
    public InternalMemberResumeSummaryVO selectResumeSummary(Long userId) {
        ensurePaidMember(userId);
        InternalMemberResumeSummaryVO summary = internalMemberMapper.selectResumeSummaryByUserId(userId);
        if (summary == null) {
            summary = new InternalMemberResumeSummaryVO();
            summary.setUserId(userId);
        }
        return summary;
    }

    @Override
    public InternalMemberInterviewSummaryVO selectInterviewSummary(Long userId) {
        ensurePaidMember(userId);
        InternalMemberInterviewSummaryVO summary = internalMemberMapper.selectInterviewSummaryByUserId(userId);
        if (summary == null) {
            summary = new InternalMemberInterviewSummaryVO();
            summary.setUserId(userId);
        }
        return summary;
    }

    @Override
    public InternalMemberSalarySummaryVO selectSalarySummary(Long userId) {
        ensurePaidMember(userId);
        InternalMemberEmploymentProfileVO profile = internalMemberMapper.selectEmploymentProfileByUserId(userId);
        InternalMemberSalarySummaryVO summary = new InternalMemberSalarySummaryVO();
        summary.setUserId(userId);
        if (profile == null) {
            return summary;
        }
        summary.setJobStatus(profile.getJobStatus());
        summary.setJobStatusName(profile.getJobStatusName());
        summary.setCompanyName(profile.getCompanyName());
        summary.setPositionName(profile.getPositionName());
        summary.setSalaryBefore(profile.getSalaryBefore());
        summary.setSalaryAfter(profile.getSalaryAfter());
        summary.setTargetSalary(profile.getTargetSalary());
        summary.setSalaryIncrease(profile.getSalaryIncrease());
        summary.setEntryTime(profile.getEntryTime());
        summary.setCaseEligible(profile.getCaseEligible());
        summary.setUpdateTime(profile.getUpdateTime());
        summary.setSalaryIncreaseRate(calculateIncreaseRate(profile.getSalaryBefore(), profile.getSalaryAfter()));
        summary.setGapToTargetSalary(calculateGapToTarget(profile.getTargetSalary(), profile.getSalaryAfter()));
        return summary;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveEmploymentProfile(Long userId, InternalMemberEmploymentProfileSaveDTO dto, Long operatorId) {
        ensurePaidMember(userId);
        if (dto == null) {
            throw new ServiceException("Employment profile cannot be empty");
        }
        normalizeAndValidateEmploymentProfile(dto);
        InternalMemberEmploymentProfileVO existed = internalMemberMapper.selectEmploymentProfileByUserId(userId);
        int affected = existed == null
                ? internalMemberMapper.insertEmploymentProfile(userId, dto, operatorId)
                : internalMemberMapper.updateEmploymentProfile(userId, dto, operatorId);
        if (affected <= 0) {
            throw new ServiceException("Failed to save employment profile");
        }
    }

    @Override
    public List<InternalMemberFollowRecordVO> selectFollowRecordList(Long userId) {
        ensurePaidMember(userId);
        return internalMemberMapper.selectFollowRecordListByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addFollowRecord(Long userId, InternalMemberFollowRecordSaveDTO dto, Long operatorId) {
        ensurePaidMember(userId);
        if (dto == null || StringUtils.isEmpty(dto.getContent())) {
            throw new ServiceException("Follow content cannot be empty");
        }
        dto.setFollowType(StringUtils.trim(dto.getFollowType()));
        dto.setContent(StringUtils.trim(dto.getContent()));
        dto.setNextFollowTime(trimToNull(dto.getNextFollowTime()));
        int affected = internalMemberMapper.insertFollowRecord(
                userId,
                dto.getFollowType(),
                dto.getContent(),
                dto.getNextFollowTime(),
                operatorId
        );
        if (affected <= 0) {
            throw new ServiceException("Failed to add follow record");
        }
    }

    @Override
    public List<InternalMemberOutcomeVO> selectOutcomeList(InternalMemberOutcomeQueryDTO query) {
        List<InternalMemberOutcomeVO> list = internalMemberMapper.selectOutcomeList(query);
        if (list != null) {
            for (InternalMemberOutcomeVO item : list) {
                fillProgressStage(item);
            }
        }
        return list;
    }

    @Override
    public InternalMemberExamSummaryVO selectExamSummary(Long userId) {
        ensurePaidMember(userId);
        List<InternalMemberExamItemVO> exams = internalMemberMapper.selectExamListByUserId(userId);
        InternalMemberExamSummaryVO summary = new InternalMemberExamSummaryVO();
        summary.setExams(exams);
        summary.setReportedCount(exams.size());
        int passedCount = 0;
        int failedCount = 0;
        int pendingReadCount = 0;
        int inProgressCount = 0;
        String lastExamTime = null;
        for (InternalMemberExamItemVO exam : exams) {
            if (lastExamTime == null) {
                lastExamTime = exam.getReportTime();
            }
            if ("passed".equals(exam.getResult())) {
                passedCount++;
            } else if ("failed".equals(exam.getResult())) {
                failedCount++;
            } else if ("pending_read".equals(exam.getResult())) {
                pendingReadCount++;
            } else if ("in_progress".equals(exam.getResult())) {
                inProgressCount++;
            }
        }
        summary.setPassedCount(passedCount);
        summary.setFailedCount(failedCount);
        summary.setPendingReadCount(pendingReadCount);
        summary.setInProgressCount(inProgressCount);
        summary.setLastExamTime(lastExamTime);
        return summary;
    }

    private void ensurePaidMember(Long userId) {
        if (userId == null) {
            throw new ServiceException("User id cannot be null");
        }
        if (internalMemberMapper.countPaidMemberByUserId(userId) <= 0) {
            throw new ServiceException("The user is not a paid member or has no valid paid record");
        }
    }

    private void fillProgressStage(InternalMemberEmploymentProfileVO profile) {
        if (profile == null) {
            return;
        }
        InternalMemberProgressStageEnum stage = InternalMemberProgressStageResolver.resolve(
                profile.getJobStatus(),
                profile.getResumeDeliveryCount(),
                profile.getInterviewCompanyCount(),
                profile.getPendingInterviewCount(),
                profile.getCompletedInterviewCount(),
                profile.getInterviewRoundCount(),
                profile.getInterviewStatus(),
                profile.getInterviewResult(),
                profile.getEntryTime()
        );
        profile.setProgressStageCode(stage.getCode());
        profile.setProgressStageName(stage.getName());
        profile.setProgressStageOrder(stage.getOrder());
        profile.setProgressStageTotal(stage.getTotal());
    }

    private void fillProgressStage(InternalMemberOutcomeVO outcome) {
        if (outcome == null) {
            return;
        }
        InternalMemberProgressStageEnum stage = InternalMemberProgressStageResolver.resolve(
                outcome.getJobStatus(),
                outcome.getResumeDeliveryCount(),
                outcome.getInterviewCompanyCount(),
                outcome.getPendingInterviewCount(),
                outcome.getCompletedInterviewCount(),
                outcome.getInterviewRoundCount(),
                outcome.getInterviewStatus(),
                outcome.getInterviewResult(),
                outcome.getEntryTime()
        );
        outcome.setProgressStageCode(stage.getCode());
        outcome.setProgressStageName(stage.getName());
        outcome.setProgressStageOrder(stage.getOrder());
        outcome.setProgressStageTotal(stage.getTotal());
    }

    private void normalizeAndValidateEmploymentProfile(InternalMemberEmploymentProfileSaveDTO dto) {
        if (!JobStatusEnum.contains(dto.getJobStatus())) {
            throw new ServiceException("求职状态不合法");
        }

        String interviewStatus = trimToNull(dto.getInterviewStatus());
        if (interviewStatus != null && !InterviewStatusEnum.contains(interviewStatus)) {
            throw new ServiceException("面试状态不合法");
        }

        String interviewResult = trimToNull(dto.getInterviewResult());
        if (interviewResult != null && !InterviewResultEnum.contains(interviewResult)) {
            throw new ServiceException("面试结果不合法");
        }

        dto.setJobStatusName(JobStatusEnum.getLabelByCode(dto.getJobStatus()));
        dto.setCompanyName(trimToNull(dto.getCompanyName()));
        dto.setPositionName(trimToNull(dto.getPositionName()));
        dto.setCity(trimToNull(dto.getCity()));
        dto.setRemark(trimToNull(dto.getRemark()));
        dto.setEntryTime(trimToNull(dto.getEntryTime()));
        dto.setInterviewStatus(interviewStatus);
        dto.setInterviewResult(interviewResult);
        dto.setLastInterviewTime(trimToNull(dto.getLastInterviewTime()));
    }

    private String trimToNull(String value) {
        String trimmed = StringUtils.trim(value);
        return StringUtils.isEmpty(trimmed) ? null : trimmed;
    }

    private BigDecimal calculateIncreaseRate(BigDecimal salaryBefore, BigDecimal salaryAfter) {
        if (salaryBefore == null || salaryAfter == null || BigDecimal.ZERO.compareTo(salaryBefore) == 0) {
            return null;
        }
        return salaryAfter.subtract(salaryBefore)
                .multiply(BigDecimal.valueOf(100))
                .divide(salaryBefore, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateGapToTarget(BigDecimal targetSalary, BigDecimal salaryAfter) {
        if (targetSalary == null || salaryAfter == null) {
            return null;
        }
        return targetSalary.subtract(salaryAfter).setScale(2, RoundingMode.HALF_UP);
    }
}
