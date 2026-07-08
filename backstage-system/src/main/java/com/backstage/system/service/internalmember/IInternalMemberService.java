package com.backstage.system.service.internalmember;

import com.backstage.system.domain.internalmember.dto.InternalMemberCourseDetailQueryDTO;
import com.backstage.system.domain.internalmember.dto.InternalMemberEmploymentProfileSaveDTO;
import com.backstage.system.domain.internalmember.dto.InternalMemberFollowRecordSaveDTO;
import com.backstage.system.domain.internalmember.dto.InternalMemberFollowTaskCompleteDTO;
import com.backstage.system.domain.internalmember.dto.InternalMemberFollowTaskSaveDTO;
import com.backstage.system.domain.internalmember.dto.InternalMemberFollowTaskStatusUpdateDTO;
import com.backstage.system.domain.internalmember.dto.InternalMemberOutcomeQueryDTO;
import com.backstage.system.domain.internalmember.dto.InternalMemberPageQueryDTO;
import com.backstage.system.domain.internalmember.vo.InternalMemberCourseProgressVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberAdminUserOptionVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberDocumentDownloadVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberExamSummaryVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberDocumentPreviewVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberDocumentUploadVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberEmploymentProfileVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberFollowRecordVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberFollowTaskVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberInterviewSummaryVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberOutcomeVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberResumeSummaryVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberSalarySummaryVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberStudyOverviewVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberStudySummaryVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IInternalMemberService {
    List<InternalMemberAdminUserOptionVO> selectAdminUserOptions(String keyword);

    List<InternalMemberStudyOverviewVO> selectPaidMemberStudyOverview(InternalMemberPageQueryDTO query);

    InternalMemberStudySummaryVO selectMemberStudySummary(Long userId);

    List<InternalMemberCourseProgressVO> selectMemberCourseProgressList(Long userId, InternalMemberCourseDetailQueryDTO query);

    InternalMemberEmploymentProfileVO selectEmploymentProfile(Long userId);

    InternalMemberResumeSummaryVO selectResumeSummary(Long userId);

    InternalMemberInterviewSummaryVO selectInterviewSummary(Long userId);

    InternalMemberSalarySummaryVO selectSalarySummary(Long userId);

    void saveEmploymentProfile(Long userId, InternalMemberEmploymentProfileSaveDTO dto, Long operatorId);

    InternalMemberDocumentUploadVO uploadEmploymentDocument(Long userId, String docType, MultipartFile file, Long operatorId);

    InternalMemberDocumentPreviewVO previewEmploymentDocument(Long userId, String docType, Integer minute);

    InternalMemberDocumentDownloadVO downloadEmploymentDocument(Long userId, String docType, Integer minute);

    void deleteEmploymentDocument(Long userId, String docType, Long operatorId);

    List<InternalMemberFollowRecordVO> selectFollowRecordList(Long userId);

    void addFollowRecord(Long userId, InternalMemberFollowRecordSaveDTO dto, Long operatorId);

    List<InternalMemberFollowTaskVO> selectFollowTaskList(Long userId);

    void addFollowTask(Long userId, InternalMemberFollowTaskSaveDTO dto, Long operatorId);

    void updateFollowTask(Long taskId, InternalMemberFollowTaskSaveDTO dto, Long operatorId);

    void updateFollowTaskStatus(Long taskId, InternalMemberFollowTaskStatusUpdateDTO dto, Long operatorId);

    void completeFollowTask(Long taskId, InternalMemberFollowTaskCompleteDTO dto, Long operatorId);

    void processDueFollowTaskReminders();

    void markOverdueFollowTasks();

    List<InternalMemberOutcomeVO> selectOutcomeList(InternalMemberOutcomeQueryDTO query);

    InternalMemberExamSummaryVO selectExamSummary(Long userId);
}
