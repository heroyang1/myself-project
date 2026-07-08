package com.backstage.system.service.internalmember.impl;

import com.backstage.common.enums.UploadPathEnum;
import com.backstage.common.exception.ServiceException;
import com.backstage.common.utils.StringUtils;
import com.backstage.system.domain.internalmember.dto.InternalMemberCourseDetailQueryDTO;
import com.backstage.system.domain.internalmember.dto.InternalMemberEmploymentProfileSaveDTO;
import com.backstage.system.domain.internalmember.dto.InternalMemberFollowRecordSaveDTO;
import com.backstage.system.domain.internalmember.dto.InternalMemberFollowTaskCompleteDTO;
import com.backstage.system.domain.internalmember.dto.InternalMemberFollowTaskSaveDTO;
import com.backstage.system.domain.internalmember.dto.InternalMemberFollowTaskStatusUpdateDTO;
import com.backstage.system.domain.internalmember.dto.InternalMemberOutcomeQueryDTO;
import com.backstage.system.domain.internalmember.dto.InternalMemberPageQueryDTO;
import com.backstage.system.domain.internalmember.vo.InternalMemberAdminUserOptionVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberCourseProgressVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberDocumentDownloadVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberDocumentPreviewVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberDocumentUploadVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberEmploymentDocumentVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberEmploymentProfileVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberExamItemVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberExamSummaryVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberFollowRecordVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberFollowTaskReminderVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberFollowTaskVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberInterviewSummaryVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberOutcomeVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberResumeSummaryVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberSalarySummaryVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberStudyOverviewVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberStudySummaryVO;
import com.backstage.system.enums.internalmember.InternalMemberDocumentTypeEnum;
import com.backstage.system.enums.internalmember.InternalMemberFollowTaskStatusEnum;
import com.backstage.system.enums.internalmember.InternalMemberProgressStageEnum;
import com.backstage.system.enums.internalmember.InterviewResultEnum;
import com.backstage.system.enums.internalmember.InterviewStatusEnum;
import com.backstage.system.enums.internalmember.JobStatusEnum;
import com.backstage.system.mapper.internalmember.InternalMemberMapper;
import com.backstage.system.service.internalmember.IInternalMemberService;
import com.backstage.system.service.internalmember.support.InternalMemberDocumentProcessor;
import com.backstage.system.service.internalmember.support.InternalMemberProgressStageResolver;
import com.backstage.system.utils.OssUtil;
import com.backstage.common.utils.email.EmailUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class InternalMemberServiceImpl implements IInternalMemberService {
    private static final Logger log = LoggerFactory.getLogger(InternalMemberServiceImpl.class);
    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int DEFAULT_PREVIEW_MINUTE = 30;
    private static final String FOLLOW_TASK_NOTIFY_CHANNEL_EMAIL = "email";
    private static final String FOLLOW_TASK_COMPLETE_RECORD_TYPE = "follow_task";
    private static final Long SYSTEM_OPERATOR_ID = 0L;

    @Resource
    private InternalMemberMapper internalMemberMapper;
    @Resource
    private InternalMemberDocumentProcessor internalMemberDocumentProcessor;
    @Resource
    private OssUtil ossUtil;
    @Resource
    private EmailUtil emailUtil;

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
    @Transactional(rollbackFor = Exception.class)
    public InternalMemberDocumentUploadVO uploadEmploymentDocument(Long userId,
                                                                   String docType,
                                                                   MultipartFile file,
                                                                   Long operatorId) {
        ensurePaidMember(userId);
        if (file == null || file.isEmpty()) {
            throw new ServiceException("Upload file cannot be empty");
        }

        InternalMemberDocumentTypeEnum documentType = InternalMemberDocumentTypeEnum.fromCode(docType);
        InternalMemberDocumentProcessor.ProcessedDocument processed =
                internalMemberDocumentProcessor.process(file, documentType);
        String bucketName = ossUtil.getOssProperties().getBucketName();
        String objectKey = buildDocumentObjectKey(userId, documentType, processed.getFileName());
        String previousFileKey = null;
        try {
            previousFileKey = selectStoredFileKey(userId, documentType);
            ossUtil.uploadObject(bucketName, objectKey, processed.getPdfFile(), "application/pdf");
            internalMemberMapper.insertEmploymentProfileShellIfAbsent(userId, operatorId);
            int affected = updateEmploymentDocument(userId, documentType, objectKey, processed.getFileName(), operatorId);
            if (affected <= 0) {
                throw new ServiceException("Failed to save employment document");
            }
            deleteObjectQuietly(bucketName, previousFileKey, objectKey);

            InternalMemberDocumentUploadVO vo = new InternalMemberDocumentUploadVO();
            vo.setDocType(documentType.getCode());
            vo.setFileKey(objectKey);
            vo.setFileName(processed.getFileName());
            return vo;
        } catch (ServiceException ex) {
            deleteUploadedObjectQuietly(bucketName, objectKey);
            throw ex;
        } catch (Exception ex) {
            deleteUploadedObjectQuietly(bucketName, objectKey);
            throw new ServiceException("Failed to upload employment document: " + ex.getMessage());
        } finally {
            internalMemberDocumentProcessor.cleanup(processed.getWorkDir());
        }
    }

    @Override
    public InternalMemberDocumentPreviewVO previewEmploymentDocument(Long userId, String docType, Integer minute) {
        ensurePaidMember(userId);
        InternalMemberDocumentTypeEnum documentType = InternalMemberDocumentTypeEnum.fromCode(docType);
        StoredEmploymentDocument storedDocument = getStoredEmploymentDocument(userId, documentType);

        InternalMemberDocumentPreviewVO vo = new InternalMemberDocumentPreviewVO();
        vo.setDocType(documentType.getCode());
        vo.setFileKey(storedDocument.getFileKey());
        vo.setFileName(storedDocument.getFileName());
        vo.setPreviewUrl(ossUtil.getInlinePreviewSignedUrl(
                storedDocument.getFileKey(),
                normalizePreviewMinute(minute),
                "application/pdf"
        ));
        return vo;
    }

    @Override
    public InternalMemberDocumentDownloadVO downloadEmploymentDocument(Long userId, String docType, Integer minute) {
        ensurePaidMember(userId);
        InternalMemberDocumentTypeEnum documentType = InternalMemberDocumentTypeEnum.fromCode(docType);
        StoredEmploymentDocument storedDocument = getStoredEmploymentDocument(userId, documentType);

        InternalMemberDocumentDownloadVO vo = new InternalMemberDocumentDownloadVO();
        vo.setDocType(documentType.getCode());
        vo.setFileKey(storedDocument.getFileKey());
        vo.setFileName(storedDocument.getFileName());
        vo.setDownloadUrl(ossUtil.getAttachmentDownloadSignedUrl(
                storedDocument.getFileKey(),
                normalizePreviewMinute(minute),
                "application/pdf",
                storedDocument.getFileName()
        ));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEmploymentDocument(Long userId, String docType, Long operatorId) {
        ensurePaidMember(userId);
        InternalMemberDocumentTypeEnum documentType = InternalMemberDocumentTypeEnum.fromCode(docType);
        StoredEmploymentDocument storedDocument = getStoredEmploymentDocument(userId, documentType);

        int affected = internalMemberMapper.clearEmploymentDocument(
                userId,
                InternalMemberDocumentTypeEnum.RESUME == documentType,
                InternalMemberDocumentTypeEnum.OFFER == documentType,
                operatorId
        );
        if (affected <= 0) {
            throw new ServiceException("Failed to delete employment document");
        }
        try {
            ossUtil.deleteObject(ossUtil.getOssProperties().getBucketName(), storedDocument.getFileKey());
        } catch (Exception ex) {
            throw new ServiceException("Failed to delete employment document file: " + ex.getMessage());
        }
    }

    private StoredEmploymentDocument getStoredEmploymentDocument(Long userId, InternalMemberDocumentTypeEnum documentType) {
        InternalMemberEmploymentDocumentVO document = internalMemberMapper.selectEmploymentDocumentByUserId(userId);
        String fileKey = extractFileKey(document, documentType);
        if (StringUtils.isEmpty(fileKey)) {
            throw new ServiceException("Employment document not found");
        }
        return new StoredEmploymentDocument(fileKey, extractFileName(document, documentType));
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
        if (dto.getResponsibleUserId() == null) {
            throw new ServiceException("Responsible admin user id cannot be empty");
        }
        ensureInternalMemberAdminUser(dto.getResponsibleUserId());
        dto.setFollowType(StringUtils.trim(dto.getFollowType()));
        dto.setContent(StringUtils.trim(dto.getContent()));
        dto.setNextFollowTime(trimToNull(dto.getNextFollowTime()));
        int affected = internalMemberMapper.insertFollowRecord(
                userId,
                dto.getFollowType(),
                dto.getContent(),
                dto.getNextFollowTime(),
                operatorId,
                dto.getResponsibleUserId()
        );
        if (affected <= 0) {
            throw new ServiceException("Failed to add follow record");
        }
    }

    @Override
    public List<InternalMemberFollowTaskVO> selectFollowTaskList(Long userId) {
        ensurePaidMember(userId);
        List<InternalMemberFollowTaskVO> list = internalMemberMapper.selectFollowTaskListByUserId(userId);
        fillFollowTaskStatus(list);
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addFollowTask(Long userId, InternalMemberFollowTaskSaveDTO dto, Long operatorId) {
        ensurePaidMember(userId);
        normalizeAndValidateFollowTask(dto);
        ensureInternalMemberAdminUser(dto.getResponsibleUserId());
        int affected = internalMemberMapper.insertFollowTask(
                userId,
                dto,
                InternalMemberFollowTaskStatusEnum.PENDING.getCode(),
                FOLLOW_TASK_NOTIFY_CHANNEL_EMAIL,
                operatorId
        );
        if (affected <= 0) {
            throw new ServiceException("Failed to create follow task");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFollowTask(Long taskId, InternalMemberFollowTaskSaveDTO dto, Long operatorId) {
        InternalMemberFollowTaskVO task = getRequiredFollowTask(taskId);
        ensureEditableFollowTask(task);
        normalizeAndValidateFollowTask(dto);
        ensureInternalMemberAdminUser(dto.getResponsibleUserId());
        int affected = internalMemberMapper.updateFollowTask(
                taskId,
                dto,
                InternalMemberFollowTaskStatusEnum.PENDING.getCode(),
                operatorId
        );
        if (affected <= 0) {
            throw new ServiceException("Failed to update follow task");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFollowTaskStatus(Long taskId, InternalMemberFollowTaskStatusUpdateDTO dto, Long operatorId) {
        InternalMemberFollowTaskVO task = getRequiredFollowTask(taskId);
        if (dto == null || StringUtils.isEmpty(dto.getStatus())) {
            throw new ServiceException("Follow task status cannot be empty");
        }
        String targetStatus = InternalMemberFollowTaskStatusEnum.normalize(dto.getStatus());
        if (!InternalMemberFollowTaskStatusEnum.CANCELED.getCode().equals(targetStatus)) {
            throw new ServiceException("Only canceled status can be updated manually");
        }
        ensureCancelableFollowTask(task);
        int affected = internalMemberMapper.updateFollowTaskStatus(
                taskId,
                targetStatus,
                null,
                null,
                null,
                operatorId
        );
        if (affected <= 0) {
            throw new ServiceException("Failed to update follow task status");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeFollowTask(Long taskId, InternalMemberFollowTaskCompleteDTO dto, Long operatorId) {
        InternalMemberFollowTaskVO task = getRequiredFollowTask(taskId);
        ensureCompletableFollowTask(task);
        String resultNote = dto == null ? null : trimToNull(dto.getResultNote());
        String completedTime = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        int affected = internalMemberMapper.updateFollowTaskStatus(
                taskId,
                InternalMemberFollowTaskStatusEnum.COMPLETED.getCode(),
                resultNote,
                completedTime,
                operatorId,
                operatorId
        );
        if (affected <= 0) {
            throw new ServiceException("Failed to complete follow task");
        }
        String followRecordContent = buildFollowTaskCompletionRecordContent(task, resultNote);
        internalMemberMapper.insertFollowRecord(
                task.getUserId(),
                FOLLOW_TASK_COMPLETE_RECORD_TYPE,
                followRecordContent,
                null,
                operatorId,
                task.getResponsibleUserId()
        );
    }

    @Override
    public void processDueFollowTaskReminders() {
        List<InternalMemberFollowTaskReminderVO> tasks = internalMemberMapper.selectDueFollowTaskReminderList();
        for (InternalMemberFollowTaskReminderVO task : tasks) {
            if (StringUtils.isEmpty(task.getResponsibleUserEmail())) {
                log.info("Skip follow task reminder because responsible admin has no email, taskId={}, responsibleUserId={}",
                        task.getId(), task.getResponsibleUserId());
                continue;
            }
            try {
                emailUtil.sendEmailCommonTo(
                        task.getResponsibleUserEmail(),
                        "【内推会员跟进提醒】请及时跟进用户",
                        buildFollowTaskReminderContent(task)
                );
                internalMemberMapper.markFollowTaskReminderSent(task.getId(), SYSTEM_OPERATOR_ID);
            } catch (Exception ex) {
                log.warn("Failed to send internal-member follow task reminder, taskId={}, email={}",
                        task.getId(), task.getResponsibleUserEmail(), ex);
            }
        }
    }

    @Override
    public void markOverdueFollowTasks() {
        int affected = internalMemberMapper.markOverdueFollowTasks(SYSTEM_OPERATOR_ID);
        if (affected > 0) {
            log.info("Marked internal-member follow tasks overdue, count={}", affected);
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

    private void ensureInternalMemberAdminUser(Long userId) {
        if (internalMemberMapper.countInternalMemberAdminUserByUserId(userId) <= 0) {
            throw new ServiceException("Responsible admin user is invalid");
        }
    }

    private InternalMemberFollowTaskVO getRequiredFollowTask(Long taskId) {
        if (taskId == null) {
            throw new ServiceException("Follow task id cannot be empty");
        }
        InternalMemberFollowTaskVO task = internalMemberMapper.selectFollowTaskById(taskId);
        if (task == null) {
            throw new ServiceException("Follow task not found");
        }
        ensurePaidMember(task.getUserId());
        fillFollowTaskStatus(task);
        return task;
    }

    private void ensureEditableFollowTask(InternalMemberFollowTaskVO task) {
        if (task == null) {
            throw new ServiceException("Follow task not found");
        }
        if (InternalMemberFollowTaskStatusEnum.COMPLETED.getCode().equals(task.getStatus())
                || InternalMemberFollowTaskStatusEnum.CANCELED.getCode().equals(task.getStatus())) {
            throw new ServiceException("Completed or canceled follow task cannot be edited");
        }
    }

    private void ensureCancelableFollowTask(InternalMemberFollowTaskVO task) {
        if (task == null) {
            throw new ServiceException("Follow task not found");
        }
        String status = task.getStatus();
        if (!InternalMemberFollowTaskStatusEnum.PENDING.getCode().equals(status)
                && !InternalMemberFollowTaskStatusEnum.NOTIFIED.getCode().equals(status)
                && !InternalMemberFollowTaskStatusEnum.OVERDUE.getCode().equals(status)) {
            throw new ServiceException("Current follow task status cannot be canceled");
        }
    }

    private void ensureCompletableFollowTask(InternalMemberFollowTaskVO task) {
        if (task == null) {
            throw new ServiceException("Follow task not found");
        }
        String status = task.getStatus();
        if (!InternalMemberFollowTaskStatusEnum.PENDING.getCode().equals(status)
                && !InternalMemberFollowTaskStatusEnum.NOTIFIED.getCode().equals(status)
                && !InternalMemberFollowTaskStatusEnum.OVERDUE.getCode().equals(status)) {
            throw new ServiceException("Current follow task status cannot be completed");
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
            throw new ServiceException("Job status is invalid");
        }

        String interviewStatus = trimToNull(dto.getInterviewStatus());
        if (interviewStatus != null && !InterviewStatusEnum.contains(interviewStatus)) {
            throw new ServiceException("Interview status is invalid");
        }

        String interviewResult = trimToNull(dto.getInterviewResult());
        if (interviewResult != null && !InterviewResultEnum.contains(interviewResult)) {
            throw new ServiceException("Interview result is invalid");
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

    private void normalizeAndValidateFollowTask(InternalMemberFollowTaskSaveDTO dto) {
        if (dto == null) {
            throw new ServiceException("Follow task cannot be empty");
        }
        dto.setContent(trimToNull(dto.getContent()));
        dto.setPlannedFollowTime(trimToNull(dto.getPlannedFollowTime()));
        dto.setRemindTime(trimToNull(dto.getRemindTime()));
        if (StringUtils.isEmpty(dto.getContent())) {
            throw new ServiceException("Follow task content cannot be empty");
        }
        if (dto.getResponsibleUserId() == null) {
            throw new ServiceException("Responsible admin user id cannot be empty");
        }
        if (StringUtils.isEmpty(dto.getPlannedFollowTime())) {
            throw new ServiceException("Planned follow time cannot be empty");
        }
        if (StringUtils.isEmpty(dto.getRemindTime())) {
            throw new ServiceException("Reminder time cannot be empty");
        }
        LocalDateTime plannedFollowTime = parseDateTime(dto.getPlannedFollowTime(), "Planned follow time format is invalid");
        LocalDateTime remindTime = parseDateTime(dto.getRemindTime(), "Reminder time format is invalid");
        if (plannedFollowTime.isBefore(LocalDateTime.now())) {
            throw new ServiceException("Planned follow time cannot be earlier than now");
        }
        if (remindTime.isAfter(plannedFollowTime)) {
            throw new ServiceException("Reminder time cannot be later than planned follow time");
        }
    }

    private LocalDateTime parseDateTime(String value, String message) {
        try {
            return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
        } catch (Exception ex) {
            throw new ServiceException(message);
        }
    }

    private String trimToNull(String value) {
        String trimmed = StringUtils.trim(value);
        return StringUtils.isEmpty(trimmed) ? null : trimmed;
    }

    private void fillFollowTaskStatus(List<InternalMemberFollowTaskVO> tasks) {
        if (tasks == null) {
            return;
        }
        for (InternalMemberFollowTaskVO task : tasks) {
            fillFollowTaskStatus(task);
        }
    }

    private void fillFollowTaskStatus(InternalMemberFollowTaskVO task) {
        if (task == null) {
            return;
        }
        task.setStatusName(InternalMemberFollowTaskStatusEnum.getLabelByCode(task.getStatus()));
    }

    private String buildFollowTaskCompletionRecordContent(InternalMemberFollowTaskVO task, String resultNote) {
        if (StringUtils.isEmpty(resultNote)) {
            return "完成跟进任务: " + task.getContent();
        }
        return "完成跟进任务: " + task.getContent() + "；完成说明: " + resultNote;
    }

    private String buildFollowTaskReminderContent(InternalMemberFollowTaskReminderVO task) {
        StringBuilder builder = new StringBuilder();
        builder.append("您有一条内推会员跟进任务即将到期，请及时处理。<br/><br/>");
        builder.append("会员用户ID：").append(task.getUserId()).append("<br/>");
        builder.append("任务内容：").append(task.getContent()).append("<br/>");
        builder.append("计划跟进时间：").append(task.getPlannedFollowTime()).append("<br/>");
        builder.append("提醒时间：").append(task.getRemindTime()).append("<br/>");
        builder.append("当前状态：").append(InternalMemberFollowTaskStatusEnum.getLabelByCode(task.getStatus()));
        return builder.toString();
    }

    private int updateEmploymentDocument(Long userId,
                                         InternalMemberDocumentTypeEnum documentType,
                                         String fileKey,
                                         String fileName,
                                         Long operatorId) {
        if (InternalMemberDocumentTypeEnum.RESUME == documentType) {
            return internalMemberMapper.updateEmploymentDocument(userId, fileKey, fileName, null, null, operatorId);
        }
        return internalMemberMapper.updateEmploymentDocument(userId, null, null, fileKey, fileName, operatorId);
    }

    private String selectStoredFileKey(Long userId, InternalMemberDocumentTypeEnum documentType) {
        InternalMemberEmploymentDocumentVO document = internalMemberMapper.selectEmploymentDocumentByUserId(userId);
        return extractFileKey(document, documentType);
    }

    private String extractFileKey(InternalMemberEmploymentDocumentVO document, InternalMemberDocumentTypeEnum documentType) {
        if (document == null) {
            return null;
        }
        return InternalMemberDocumentTypeEnum.RESUME == documentType ? document.getResumeFileKey() : document.getOfferFileKey();
    }

    private String extractFileName(InternalMemberEmploymentDocumentVO document, InternalMemberDocumentTypeEnum documentType) {
        if (document == null) {
            return null;
        }
        return InternalMemberDocumentTypeEnum.RESUME == documentType ? document.getResumeFileName() : document.getOfferFileName();
    }

    private String buildDocumentObjectKey(Long userId, InternalMemberDocumentTypeEnum documentType, String fileName) {
        String yearMonth = LocalDate.now().format(YEAR_MONTH_FORMATTER);
        return UploadPathEnum.DOCUMENT.getPath()
                + "internal-member/"
                + userId
                + "/"
                + documentType.getPathSegment()
                + "/"
                + yearMonth
                + "/"
                + UUID.randomUUID().toString().replace("-", "")
                + "_"
                + sanitizeFileName(fileName);
    }

    private String sanitizeFileName(String fileName) {
        String normalized = StringUtils.trim(fileName);
        if (StringUtils.isEmpty(normalized)) {
            return "document.pdf";
        }
        return normalized.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
    }

    private int normalizePreviewMinute(Integer minute) {
        if (minute == null || minute <= 0) {
            return DEFAULT_PREVIEW_MINUTE;
        }
        return minute;
    }

    private void deleteUploadedObjectQuietly(String bucketName, String objectKey) {
        if (StringUtils.isEmpty(objectKey)) {
            return;
        }
        try {
            ossUtil.deleteObject(bucketName, objectKey);
        } catch (Exception ignore) {
            // ignore rollback cleanup failure
        }
    }

    private void deleteObjectQuietly(String bucketName, String previousFileKey, String currentFileKey) {
        if (StringUtils.isEmpty(previousFileKey) || previousFileKey.equals(currentFileKey)) {
            return;
        }
        try {
            ossUtil.deleteObject(bucketName, previousFileKey);
        } catch (Exception ignore) {
            // ignore replacement cleanup failure
        }
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

    private static class StoredEmploymentDocument {
        private final String fileKey;
        private final String fileName;

        private StoredEmploymentDocument(String fileKey, String fileName) {
            this.fileKey = fileKey;
            this.fileName = fileName;
        }

        private String getFileKey() {
            return fileKey;
        }

        private String getFileName() {
            return fileName;
        }
    }
}
