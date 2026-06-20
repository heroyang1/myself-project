package com.backstage.system.service.internalmember;

import com.backstage.system.domain.internalmember.dto.InternalMemberCourseDetailQueryDTO;
import com.backstage.system.domain.internalmember.dto.InternalMemberEmploymentProfileSaveDTO;
import com.backstage.system.domain.internalmember.dto.InternalMemberFollowRecordSaveDTO;
import com.backstage.system.domain.internalmember.dto.InternalMemberOutcomeQueryDTO;
import com.backstage.system.domain.internalmember.dto.InternalMemberPageQueryDTO;
import com.backstage.system.domain.internalmember.vo.InternalMemberCourseProgressVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberExamSummaryVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberEmploymentProfileVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberFollowRecordVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberInterviewSummaryVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberOutcomeVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberResumeSummaryVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberSalarySummaryVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberStudyOverviewVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberStudySummaryVO;

import java.util.List;

public interface IInternalMemberService {
    List<InternalMemberStudyOverviewVO> selectPaidMemberStudyOverview(InternalMemberPageQueryDTO query);

    InternalMemberStudySummaryVO selectMemberStudySummary(Long userId);

    List<InternalMemberCourseProgressVO> selectMemberCourseProgressList(Long userId, InternalMemberCourseDetailQueryDTO query);

    InternalMemberEmploymentProfileVO selectEmploymentProfile(Long userId);

    InternalMemberResumeSummaryVO selectResumeSummary(Long userId);

    InternalMemberInterviewSummaryVO selectInterviewSummary(Long userId);

    InternalMemberSalarySummaryVO selectSalarySummary(Long userId);

    void saveEmploymentProfile(Long userId, InternalMemberEmploymentProfileSaveDTO dto, Long operatorId);

    List<InternalMemberFollowRecordVO> selectFollowRecordList(Long userId);

    void addFollowRecord(Long userId, InternalMemberFollowRecordSaveDTO dto, Long operatorId);

    List<InternalMemberOutcomeVO> selectOutcomeList(InternalMemberOutcomeQueryDTO query);

    InternalMemberExamSummaryVO selectExamSummary(Long userId);
}
