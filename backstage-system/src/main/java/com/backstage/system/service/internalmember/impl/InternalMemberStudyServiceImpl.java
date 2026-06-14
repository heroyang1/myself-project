package com.backstage.system.service.internalmember.impl;

import com.backstage.common.exception.ServiceException;
import com.backstage.system.domain.internalmember.dto.InternalMemberCourseDetailQueryDTO;
import com.backstage.system.domain.internalmember.dto.InternalMemberPageQueryDTO;
import com.backstage.system.domain.internalmember.vo.InternalMemberCourseProgressVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberStudyOverviewVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberStudySummaryVO;
import com.backstage.system.mapper.internalmember.InternalMemberStudyMapper;
import com.backstage.system.service.internalmember.IInternalMemberStudyService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class InternalMemberStudyServiceImpl implements IInternalMemberStudyService {
    @Resource
    private InternalMemberStudyMapper internalMemberStudyMapper;

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
        return summary;
    }

    @Override
    public List<InternalMemberCourseProgressVO> selectMemberCourseProgressList(Long userId,
                                                                               InternalMemberCourseDetailQueryDTO query) {
        ensurePaidMember(userId);
        return internalMemberStudyMapper.selectMemberCourseProgressList(userId, query);
    }

    private void ensurePaidMember(Long userId) {
        if (userId == null) {
            throw new ServiceException("User id cannot be null");
        }
        if (internalMemberStudyMapper.countPaidMemberByUserId(userId) <= 0) {
            throw new ServiceException("The user is not a paid member or has no valid paid record");
        }
    }
}
