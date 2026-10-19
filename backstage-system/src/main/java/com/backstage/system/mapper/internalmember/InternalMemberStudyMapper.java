package com.backstage.system.mapper.internalmember;

import com.backstage.system.domain.internalmember.dto.InternalMemberCourseDetailQueryDTO;
import com.backstage.system.domain.internalmember.dto.InternalMemberPageQueryDTO;
import com.backstage.system.domain.internalmember.vo.InternalMemberCourseProgressVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberStudyOverviewVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberStudySummaryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InternalMemberStudyMapper {
    List<InternalMemberStudyOverviewVO> selectPaidMemberStudyOverview(@Param("query") InternalMemberPageQueryDTO query);

    InternalMemberStudySummaryVO selectMemberStudySummary(@Param("userId") Long userId);

    List<InternalMemberCourseProgressVO> selectMemberCourseProgressList(@Param("userId") Long userId,
                                                                        @Param("query") InternalMemberCourseDetailQueryDTO query);

    int countPaidMemberByUserId(@Param("userId") Long userId);
}
