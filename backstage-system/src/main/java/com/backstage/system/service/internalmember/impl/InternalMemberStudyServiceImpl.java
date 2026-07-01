package com.backstage.system.service.internalmember.impl;

import com.backstage.common.exception.ServiceException;
import com.backstage.system.domain.internalmember.dto.InternalMemberCourseDetailQueryDTO;
import com.backstage.system.domain.internalmember.dto.InternalMemberPageQueryDTO;
import com.backstage.system.domain.internalmember.vo.InternalMemberCourseProgressVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberEmploymentProfileVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberExamItemVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberStudyAttitudeVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberStudyOverviewVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberStudySummaryVO;
import com.backstage.system.enums.internalmember.InternalMemberStageEnum;
import com.backstage.system.mapper.internalmember.InternalMemberMapper;
import com.backstage.system.mapper.internalmember.InternalMemberStudyMapper;
import com.backstage.system.service.internalmember.IInternalMemberStudyService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class InternalMemberStudyServiceImpl implements IInternalMemberStudyService {
    @Resource
    private InternalMemberStudyMapper internalMemberStudyMapper;
    @Resource
    private InternalMemberMapper internalMemberMapper;

    @Override
    public List<InternalMemberStudyOverviewVO> selectPaidMemberStudyOverview(InternalMemberPageQueryDTO query) {
        return internalMemberStudyMapper.selectPaidMemberStudyOverview(query);
    }

    @Override
    public InternalMemberStudySummaryVO selectMemberStudySummary(Long userId) {
        ensurePaidMember(userId);
        InternalMemberStudySummaryVO summary = internalMemberStudyMapper.selectMemberStudySummary(userId);
        if (summary == null) {
            throw new ServiceException("Paid member record not found");
        }
        fillCurrentStage(summary, userId);
        return summary;
    }

    @Override
    public List<InternalMemberCourseProgressVO> selectMemberCourseProgressList(Long userId,
                                                                               InternalMemberCourseDetailQueryDTO query) {
        ensurePaidMember(userId);
        return internalMemberStudyMapper.selectMemberCourseProgressList(userId, query);
    }

    @Override
    public InternalMemberStudyAttitudeVO selectStudyAttitude(Long userId) {
        ensurePaidMember(userId);
        InternalMemberStudySummaryVO summary = internalMemberStudyMapper.selectMemberStudySummary(userId);
        if (summary == null) {
            throw new ServiceException("Paid member record not found");
        }
        InternalMemberCourseDetailQueryDTO query = new InternalMemberCourseDetailQueryDTO();
        query.setIncludeUnstarted(Boolean.TRUE);
        List<InternalMemberCourseProgressVO> courses = internalMemberStudyMapper.selectMemberCourseProgressList(userId, query);

        InternalMemberStudyAttitudeVO attitude = new InternalMemberStudyAttitudeVO();
        attitude.setOverallProgress(summary.getOverallProgress());
        attitude.setStartedCourseCount(summary.getStartedCourseCount());
        attitude.setCompletedCourseCount(summary.getCompletedCourseCount());
        attitude.setUnstartedCourseCount(summary.getUnstartedCourseCount());
        attitude.setLastLearnTime(summary.getLastLearnTime());

        int score = 50;
        List<String> signals = new ArrayList<>();
        Long inactiveDays = daysSince(summary.getLastLearnTime());
        if (inactiveDays == null) {
            score -= 25;
            signals.add("no_learning_record");
        } else if (inactiveDays <= 7) {
            score += 20;
            signals.add("recently_active");
        } else if (inactiveDays <= 14) {
            score += 5;
        } else if (inactiveDays <= 30) {
            score -= 10;
            signals.add("learning_slows_down");
        } else {
            score -= 25;
            signals.add("long_time_no_learning");
        }

        BigDecimal startRate = safe(summary.getStartRate());
        BigDecimal completionRate = safe(summary.getCompletionRate());
        if (startRate.compareTo(BigDecimal.valueOf(70)) >= 0) {
            score += 10;
            signals.add("high_started_ratio");
        } else if (startRate.compareTo(BigDecimal.valueOf(30)) < 0) {
            score -= 10;
            signals.add("high_unstarted_ratio");
        }
        if (completionRate.compareTo(BigDecimal.valueOf(40)) >= 0) {
            score += 10;
            signals.add("good_completion_rate");
        } else if (completionRate.compareTo(BigDecimal.TEN) < 0 && summary.getStartedCourseCount() != null
                && summary.getStartedCourseCount() > 0) {
            score -= 10;
            signals.add("low_completion_rate");
        }

        int stalledCourseCount = 0;
        int pausedCourseCount = 0;
        for (InternalMemberCourseProgressVO course : courses) {
            if (Integer.valueOf(2).equals(course.getProgressStatus())) {
                pausedCourseCount++;
            }
            if (Integer.valueOf(1).equals(course.getStartedFlag())
                    && !Integer.valueOf(1).equals(course.getCompletedFlag())
                    && safe(course.getProgressPercent()).compareTo(BigDecimal.valueOf(30)) < 0) {
                Long lastLearnDays = daysSince(course.getLastLearnTime());
                if (lastLearnDays != null && lastLearnDays > 14) {
                    stalledCourseCount++;
                }
            }
        }
        if (pausedCourseCount > 0) {
            score -= 5;
            signals.add("has_paused_courses");
        }
        if (stalledCourseCount > 0) {
            score -= 10;
            signals.add("has_stalled_courses");
        }

        score = Math.max(0, Math.min(100, score));
        attitude.setScore(score);
        if (score >= 75) {
            attitude.setAttitude("positive");
        } else if (score >= 45) {
            attitude.setAttitude("normal");
        } else {
            attitude.setAttitude("negative");
        }
        attitude.setSignals(signals);
        return attitude;
    }

    private void ensurePaidMember(Long userId) {
        if (userId == null) {
            throw new ServiceException("User id cannot be null");
        }
        if (internalMemberStudyMapper.countPaidMemberByUserId(userId) <= 0) {
            throw new ServiceException("The user is not a paid member or has no valid paid record");
        }
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Long daysSince(String time) {
        if (time == null || time.trim().isEmpty()) {
            return null;
        }
        LocalDateTime dateTime = LocalDateTime.parse(time, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return Duration.between(dateTime, LocalDateTime.now()).toDays();
    }

    private void fillCurrentStage(InternalMemberStudySummaryVO summary, Long userId) {
        InternalMemberEmploymentProfileVO employmentProfile = internalMemberMapper.selectEmploymentProfileByUserId(userId);
        List<InternalMemberExamItemVO> exams = internalMemberMapper.selectExamListByUserId(userId);
        InternalMemberStageEnum stage = resolveCurrentStage(summary, employmentProfile, exams);
        summary.setCurrentStage(stage.getCode());
        summary.setCurrentStageName(stage.getName());
        summary.setCurrentStageIndex(stage.getIndex());
    }

    private InternalMemberStageEnum resolveCurrentStage(InternalMemberStudySummaryVO summary,
                                                        InternalMemberEmploymentProfileVO employmentProfile,
                                                        List<InternalMemberExamItemVO> exams) {
        if (employmentProfile != null) {
            if (equalsNumber(employmentProfile.getJobStatus(), 2) || hasText(employmentProfile.getEntryTime())) {
                return InternalMemberStageEnum.EMPLOYED;
            }

            if (equalsNumber(employmentProfile.getJobStatus(), 1)
                    || gtZero(employmentProfile.getPendingInterviewCount())
                    || gtZero(employmentProfile.getCompletedInterviewCount())
                    || hasText(employmentProfile.getInterviewStatus())) {
                return InternalMemberStageEnum.INTERVIEW;
            }
        }

        if (hasExamStage(exams)) {
            return InternalMemberStageEnum.EXAM;
        }

        if (summary != null
                && (gtZero(summary.getStartedCourseCount())
                || gtZero(summary.getOverallProgress())
                || hasText(summary.getLastLearnTime()))) {
            return InternalMemberStageEnum.LEARNING;
        }

        return InternalMemberStageEnum.NOT_STARTED;
    }

    private boolean hasExamStage(List<InternalMemberExamItemVO> exams) {
        if (exams == null || exams.isEmpty()) {
            return false;
        }
        int reportedCount = exams.size();
        int passedCount = 0;
        int failedCount = 0;
        int inProgressCount = 0;
        for (InternalMemberExamItemVO exam : exams) {
            if ("passed".equals(exam.getResult())) {
                passedCount++;
            } else if ("failed".equals(exam.getResult())) {
                failedCount++;
            } else if ("in_progress".equals(exam.getResult())) {
                inProgressCount++;
            }
        }
        return gtZero(reportedCount) || gtZero(inProgressCount) || gtZero(passedCount) || gtZero(failedCount);
    }

    private boolean gtZero(Number value) {
        return value != null && value.doubleValue() > 0;
    }

    private boolean equalsNumber(Number value, int target) {
        return value != null && value.intValue() == target;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
