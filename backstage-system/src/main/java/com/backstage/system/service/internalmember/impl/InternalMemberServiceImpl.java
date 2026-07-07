package com.backstage.system.service.internalmember.impl;

import com.backstage.common.enums.UploadPathEnum;
import com.backstage.common.exception.ServiceException;
import com.backstage.common.utils.StringUtils;
import com.backstage.system.domain.internalmember.dto.InternalMemberCourseDetailQueryDTO;
import com.backstage.system.domain.internalmember.dto.InternalMemberEmploymentProfileSaveDTO;
import com.backstage.system.domain.internalmember.dto.InternalMemberFollowRecordSaveDTO;
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
import com.backstage.system.domain.internalmember.vo.InternalMemberInterviewSummaryVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberOutcomeVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberResumeSummaryVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberSalarySummaryVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberStudyOverviewVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberStudySummaryVO;
import com.backstage.system.enums.internalmember.InternalMemberDocumentTypeEnum;
import com.backstage.system.enums.internalmember.InternalMemberProgressStageEnum;
import com.backstage.system.enums.internalmember.InterviewResultEnum;
import com.backstage.system.enums.internalmember.InterviewStatusEnum;
import com.backstage.system.enums.internalmember.JobStatusEnum;
import com.backstage.system.mapper.internalmember.InternalMemberMapper;
import com.backstage.system.service.internalmember.IInternalMemberService;
import com.backstage.system.service.internalmember.support.InternalMemberDocumentProcessor;
import com.backstage.system.service.internalmember.support.InternalMemberProgressStageResolver;
import com.backstage.system.utils.OssUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class InternalMemberServiceImpl implements IInternalMemberService {
    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final int DEFAULT_PREVIEW_MINUTE = 30;

    @Resource
    private InternalMemberMapper internalMemberMapper;
    @Resource
    private InternalMemberDocumentProcessor internalMemberDocumentProcessor;
    @Resource
    private OssUtil ossUtil;

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

    private String trimToNull(String value) {
        String trimmed = StringUtils.trim(value);
        return StringUtils.isEmpty(trimmed) ? null : trimmed;
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
