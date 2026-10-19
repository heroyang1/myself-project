package com.backstage.system.service.internalmember;

import com.backstage.system.domain.internalmember.dto.InternalMemberCourseDetailQueryDTO;
import com.backstage.system.domain.internalmember.dto.InternalMemberPageQueryDTO;
import com.backstage.system.domain.internalmember.vo.InternalMemberCourseProgressVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberStudyAttitudeVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberStudyOverviewVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberStudySummaryVO;

import java.util.List;

public interface IInternalMemberStudyService {
    List<InternalMemberStudyOverviewVO> selectPaidMemberStudyOverview(InternalMemberPageQueryDTO query);

    InternalMemberStudySummaryVO selectMemberStudySummary(Long userId);

    List<InternalMemberCourseProgressVO> selectMemberCourseProgressList(Long userId,
                                                                        InternalMemberCourseDetailQueryDTO query);

    InternalMemberStudyAttitudeVO selectStudyAttitude(Long userId);
}
