package com.backstage.system.service.internalmember.impl;

import com.backstage.common.exception.ServiceException;
import com.backstage.common.utils.StringUtils;
import com.backstage.system.domain.internalmember.dto.InternalMemberCourseDetailQueryDTO;
import com.backstage.system.domain.internalmember.dto.InternalMemberEmploymentProfileSaveDTO;
import com.backstage.system.domain.internalmember.dto.InternalMemberFollowRecordSaveDTO;
import com.backstage.system.domain.internalmember.dto.InternalMemberOutcomeQueryDTO;
import com.backstage.system.domain.internalmember.dto.InternalMemberPageQueryDTO;
import com.backstage.system.domain.internalmember.vo.InternalMemberCourseProgressVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberEmploymentProfileVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberFollowRecordVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberOutcomeVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberStudyOverviewVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberStudySummaryVO;
import com.backstage.system.mapper.internalmember.InternalMemberMapper;
import com.backstage.system.service.internalmember.IInternalMemberService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
public class InternalMemberServiceImpl implements IInternalMemberService {
    @Resource
    private InternalMemberMapper internalMemberMapper;

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
        return internalMemberMapper.selectEmploymentProfileByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveEmploymentProfile(Long userId, InternalMemberEmploymentProfileSaveDTO dto, Long operatorId) {
        ensurePaidMember(userId);
        if (dto == null) {
            throw new ServiceException("Employment profile cannot be empty");
        }
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
        return internalMemberMapper.selectOutcomeList(query);
    }

    private void ensurePaidMember(Long userId) {
        if (userId == null) {
            throw new ServiceException("User id cannot be null");
        }
        if (internalMemberMapper.countPaidMemberByUserId(userId) <= 0) {
            throw new ServiceException("The user is not a paid member or has no valid paid record");
        }
    }
}
