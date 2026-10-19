package com.backstage.system.service.internalmember.support;

import com.backstage.system.enums.internalmember.InternalMemberProgressStageEnum;
import com.backstage.system.enums.internalmember.InterviewResultEnum;
import com.backstage.system.enums.internalmember.JobStatusEnum;

public final class InternalMemberProgressStageResolver {

    private InternalMemberProgressStageResolver() {
    }

    public static InternalMemberProgressStageEnum resolve(Integer jobStatus,
                                                          Integer resumeDeliveryCount,
                                                          Integer interviewCompanyCount,
                                                          Integer pendingInterviewCount,
                                                          Integer completedInterviewCount,
                                                          Integer interviewRoundCount,
                                                          String interviewStatus,
                                                          String interviewResult,
                                                          String entryTime) {
        if (isOnboarded(jobStatus, interviewResult, entryTime)) {
            return InternalMemberProgressStageEnum.ONBOARDED;
        }
        if (InterviewResultEnum.OFFER_SENT.getCode().equals(interviewResult)) {
            return InternalMemberProgressStageEnum.OFFER_PENDING;
        }
        if (hasInterviewSignal(interviewCompanyCount,
                pendingInterviewCount,
                completedInterviewCount,
                interviewRoundCount,
                interviewStatus,
                interviewResult)) {
            return InternalMemberProgressStageEnum.INTERVIEWING;
        }
        if (gtZero(resumeDeliveryCount)) {
            return InternalMemberProgressStageEnum.DELIVERING;
        }
        if (equalsNumber(jobStatus, JobStatusEnum.LOOKING.getCode())) {
            return InternalMemberProgressStageEnum.PREPARING;
        }
        return InternalMemberProgressStageEnum.SEPARATED;
    }

    private static boolean isOnboarded(Integer jobStatus, String interviewResult, String entryTime) {
        return equalsNumber(jobStatus, JobStatusEnum.EMPLOYED.getCode())
                || InterviewResultEnum.ONBOARDED.getCode().equals(interviewResult)
                || hasText(entryTime);
    }

    private static boolean hasInterviewSignal(Integer interviewCompanyCount,
                                              Integer pendingInterviewCount,
                                              Integer completedInterviewCount,
                                              Integer interviewRoundCount,
                                              String interviewStatus,
                                              String interviewResult) {
        return gtZero(interviewCompanyCount)
                || gtZero(pendingInterviewCount)
                || gtZero(completedInterviewCount)
                || gtZero(interviewRoundCount)
                || hasText(interviewStatus)
                || InterviewResultEnum.PASSED.getCode().equals(interviewResult)
                || InterviewResultEnum.FAILED.getCode().equals(interviewResult)
                || InterviewResultEnum.PENDING.getCode().equals(interviewResult);
    }

    private static boolean gtZero(Number value) {
        return value != null && value.intValue() > 0;
    }

    private static boolean equalsNumber(Number value, Number target) {
        return value != null && target != null && value.intValue() == target.intValue();
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
