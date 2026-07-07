package com.backstage.system.mapper.internalmember;

import com.backstage.system.domain.internalmember.dto.InternalMemberCourseDetailQueryDTO;
import com.backstage.system.domain.internalmember.dto.InternalMemberEmploymentProfileSaveDTO;
import com.backstage.system.domain.internalmember.dto.InternalMemberOutcomeQueryDTO;
import com.backstage.system.domain.internalmember.dto.InternalMemberPageQueryDTO;
import com.backstage.system.domain.internalmember.vo.InternalMemberAdminUserOptionVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberCourseProgressVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberEmploymentDocumentVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberExamItemVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberEmploymentProfileVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberFollowRecordVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberInterviewSummaryVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberOutcomeVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberResumeSummaryVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberStudyOverviewVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberStudySummaryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InternalMemberMapper {
    List<InternalMemberAdminUserOptionVO> selectAdminUserOptions(@Param("keyword") String keyword);

    List<InternalMemberStudyOverviewVO> selectPaidMemberStudyOverview(@Param("query") InternalMemberPageQueryDTO query);

    InternalMemberStudySummaryVO selectMemberStudySummary(@Param("userId") Long userId);

    List<InternalMemberCourseProgressVO> selectMemberCourseProgressList(@Param("userId") Long userId,
                                                                        @Param("query") InternalMemberCourseDetailQueryDTO query);

    InternalMemberEmploymentProfileVO selectEmploymentProfileByUserId(@Param("userId") Long userId);

    InternalMemberEmploymentDocumentVO selectEmploymentDocumentByUserId(@Param("userId") Long userId);

    InternalMemberResumeSummaryVO selectResumeSummaryByUserId(@Param("userId") Long userId);

    InternalMemberInterviewSummaryVO selectInterviewSummaryByUserId(@Param("userId") Long userId);

    int insertEmploymentProfile(@Param("userId") Long userId,
                                @Param("dto") InternalMemberEmploymentProfileSaveDTO dto,
                                @Param("operatorId") Long operatorId);

    int updateEmploymentProfile(@Param("userId") Long userId,
                                @Param("dto") InternalMemberEmploymentProfileSaveDTO dto,
                                @Param("operatorId") Long operatorId);

    int insertEmploymentProfileShellIfAbsent(@Param("userId") Long userId,
                                             @Param("operatorId") Long operatorId);

    int updateEmploymentDocument(@Param("userId") Long userId,
                                 @Param("resumeFileKey") String resumeFileKey,
                                 @Param("resumeFileName") String resumeFileName,
                                 @Param("offerFileKey") String offerFileKey,
                                 @Param("offerFileName") String offerFileName,
                                 @Param("operatorId") Long operatorId);

    int clearEmploymentDocument(@Param("userId") Long userId,
                                @Param("clearResume") boolean clearResume,
                                @Param("clearOffer") boolean clearOffer,
                                @Param("operatorId") Long operatorId);

    List<InternalMemberFollowRecordVO> selectFollowRecordListByUserId(@Param("userId") Long userId);

    int insertFollowRecord(@Param("userId") Long userId,
                           @Param("followType") String followType,
                           @Param("content") String content,
                           @Param("nextFollowTime") String nextFollowTime,
                           @Param("operatorId") Long operatorId,
                           @Param("responsibleUserId") Long responsibleUserId);

    int countInternalMemberAdminUserByUserId(@Param("userId") Long userId);

    List<InternalMemberOutcomeVO> selectOutcomeList(@Param("query") InternalMemberOutcomeQueryDTO query);

    List<InternalMemberExamItemVO> selectExamListByUserId(@Param("userId") Long userId);

    int countPaidMemberByUserId(@Param("userId") Long userId);
}
