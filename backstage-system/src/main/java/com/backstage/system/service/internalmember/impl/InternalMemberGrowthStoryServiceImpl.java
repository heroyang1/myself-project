package com.backstage.system.service.internalmember.impl;

import com.alibaba.fastjson2.JSON;
import com.backstage.common.exception.ServiceException;
import com.backstage.common.utils.StringUtils;
import com.backstage.system.domain.internalmember.dto.InternalMemberGrowthStoryPageQueryDTO;
import com.backstage.system.domain.internalmember.dto.InternalMemberGrowthStoryVisibilityUpdateDTO;
import com.backstage.system.domain.internalmember.vo.InternalMemberEmploymentProfileVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberExamSummaryVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberGrowthStoryDetailVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberGrowthStoryPageVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberInterviewSummaryVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberResumeSummaryVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberStudyAttitudeVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberStudySummaryVO;
import com.backstage.system.enums.internalmember.JobStatusEnum;
import com.backstage.system.mapper.internalmember.InternalMemberGrowthStoryMapper;
import com.backstage.system.service.internalmember.IInternalMemberGrowthStoryService;
import com.backstage.system.service.internalmember.IInternalMemberService;
import com.backstage.system.service.internalmember.IInternalMemberStudyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class InternalMemberGrowthStoryServiceImpl implements IInternalMemberGrowthStoryService {
    private static final String GENERATE_STATUS_SUCCESS = "success";

    @Resource
    private IInternalMemberService internalMemberService;
    @Resource
    private IInternalMemberStudyService internalMemberStudyService;
    @Resource
    private InternalMemberGrowthStoryMapper internalMemberGrowthStoryMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InternalMemberGrowthStoryDetailVO generateMyStory(Long userId) {
        GenerationContext context = buildGenerationContext(userId);
        StoryRenderResult rendered = renderStory(context);
        InternalMemberGrowthStoryDetailVO existed = internalMemberGrowthStoryMapper.selectStoryDetailByUserId(userId);
        int affected;
        if (existed == null) {
            affected = internalMemberGrowthStoryMapper.insertStory(
                    userId,
                    rendered.getStoryTitle(),
                    rendered.getStorySummary(),
                    rendered.getStoryContent(),
                    rendered.getFactsJson(),
                    GENERATE_STATUS_SUCCESS,
                    0,
                    null,
                    userId
            );
        } else {
            affected = internalMemberGrowthStoryMapper.updateStory(
                    userId,
                    rendered.getStoryTitle(),
                    rendered.getStorySummary(),
                    rendered.getStoryContent(),
                    rendered.getFactsJson(),
                    GENERATE_STATUS_SUCCESS,
                    userId
            );
        }
        if (affected <= 0) {
            throw new ServiceException("Failed to generate growth story");
        }
        return internalMemberGrowthStoryMapper.selectStoryDetailByUserId(userId);
    }

    @Override
    public InternalMemberGrowthStoryDetailVO getMyStory(Long userId) {
        ensurePaidMember(userId);
        return internalMemberGrowthStoryMapper.selectStoryDetailByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InternalMemberGrowthStoryDetailVO updateMyStoryVisibility(Long userId,
                                                                     InternalMemberGrowthStoryVisibilityUpdateDTO dto) {
        ensurePaidMember(userId);
        InternalMemberGrowthStoryDetailVO existed = internalMemberGrowthStoryMapper.selectStoryDetailByUserId(userId);
        if (existed == null) {
            throw new ServiceException("Generate your growth story first");
        }
        int affected = internalMemberGrowthStoryMapper.updateStoryVisibility(userId, dto.getIsPublic(), userId);
        if (affected <= 0) {
            throw new ServiceException("Failed to update growth story visibility");
        }
        return internalMemberGrowthStoryMapper.selectStoryDetailByUserId(userId);
    }

    @Override
    public List<InternalMemberGrowthStoryPageVO> selectPublicStoryPage(Long userId,
                                                                       InternalMemberGrowthStoryPageQueryDTO query) {
        ensureCanViewOthers(userId);
        String keyword = query == null ? null : StringUtils.trim(query.getKeyword());
        return internalMemberGrowthStoryMapper.selectPublicStoryPage(keyword, userId);
    }

    @Override
    public InternalMemberGrowthStoryDetailVO getStoryDetail(Long userId, Long targetUserId) {
        if (targetUserId == null) {
            throw new ServiceException("Target user id cannot be empty");
        }
        if (targetUserId.equals(userId)) {
            ensurePaidMember(userId);
            return internalMemberGrowthStoryMapper.selectStoryDetailByUserId(userId);
        }
        ensureCanViewOthers(userId);
        InternalMemberGrowthStoryDetailVO detail = internalMemberGrowthStoryMapper.selectPublicStoryDetailByUserId(targetUserId);
        if (detail == null) {
            throw new ServiceException("Growth story not found or not public");
        }
        return detail;
    }

    private GenerationContext buildGenerationContext(Long userId) {
        InternalMemberStudySummaryVO studySummary = internalMemberStudyService.selectMemberStudySummary(userId);
        InternalMemberStudyAttitudeVO studyAttitude = internalMemberStudyService.selectStudyAttitude(userId);
        InternalMemberEmploymentProfileVO employmentProfile = internalMemberService.selectEmploymentProfile(userId);
        ensureGenerationEligible(employmentProfile);
        InternalMemberResumeSummaryVO resumeSummary = internalMemberService.selectResumeSummary(userId);
        InternalMemberInterviewSummaryVO interviewSummary = internalMemberService.selectInterviewSummary(userId);
        InternalMemberExamSummaryVO examSummary = internalMemberService.selectExamSummary(userId);

        GenerationContext context = new GenerationContext();
        context.setStudySummary(studySummary);
        context.setStudyAttitude(studyAttitude);
        context.setEmploymentProfile(employmentProfile);
        context.setResumeSummary(resumeSummary);
        context.setInterviewSummary(interviewSummary);
        context.setExamSummary(examSummary);
        return context;
    }

    private void ensureCanViewOthers(Long userId) {
        InternalMemberEmploymentProfileVO employmentProfile = ensurePaidMember(userId);
        ensureGenerationEligible(employmentProfile);
        InternalMemberGrowthStoryDetailVO ownStory = internalMemberGrowthStoryMapper.selectStoryDetailByUserId(userId);
        if (ownStory == null || !Integer.valueOf(1).equals(ownStory.getIsPublic())) {
            throw new ServiceException("Publish your own growth story before viewing others");
        }
    }

    private InternalMemberEmploymentProfileVO ensurePaidMember(Long userId) {
        return internalMemberService.selectEmploymentProfile(userId);
    }

    private void ensureGenerationEligible(InternalMemberEmploymentProfileVO employmentProfile) {
        if (employmentProfile == null) {
            throw new ServiceException("Complete your employment profile before generating a growth story");
        }
        if (!isEmployed(employmentProfile)) {
            throw new ServiceException("Only employed users can generate a growth story");
        }
        if (StringUtils.isEmpty(employmentProfile.getResumeFileKey())) {
            throw new ServiceException("Upload your resume before generating a growth story");
        }
        if (StringUtils.isEmpty(employmentProfile.getOfferFileKey())) {
            throw new ServiceException("Upload your offer before generating a growth story");
        }
    }

    private boolean isEmployed(InternalMemberEmploymentProfileVO employmentProfile) {
        return JobStatusEnum.EMPLOYED.getCode().equals(employmentProfile.getJobStatus())
                || StringUtils.isNotEmpty(employmentProfile.getEntryTime());
    }

    private StoryRenderResult renderStory(GenerationContext context) {
        InternalMemberStudySummaryVO studySummary = context.getStudySummary();
        InternalMemberStudyAttitudeVO studyAttitude = context.getStudyAttitude();
        InternalMemberEmploymentProfileVO employmentProfile = context.getEmploymentProfile();
        InternalMemberResumeSummaryVO resumeSummary = context.getResumeSummary();
        InternalMemberInterviewSummaryVO interviewSummary = context.getInterviewSummary();
        InternalMemberExamSummaryVO examSummary = context.getExamSummary();

        String username = defaultText(studySummary.getUsername(), "该用户");
        String title = username + "的成长故事";
        String summary = buildSummary(username, studySummary, resumeSummary, interviewSummary, employmentProfile);
        String content = buildStoryContent(username, studySummary, studyAttitude, resumeSummary,
                interviewSummary, examSummary, employmentProfile);
        String factsJson = buildFactsJson(studySummary, studyAttitude, resumeSummary, interviewSummary,
                examSummary, employmentProfile);

        StoryRenderResult result = new StoryRenderResult();
        result.setStoryTitle(title);
        result.setStorySummary(summary);
        result.setStoryContent(content);
        result.setFactsJson(factsJson);
        return result;
    }

    private String buildSummary(String username,
                                InternalMemberStudySummaryVO studySummary,
                                InternalMemberResumeSummaryVO resumeSummary,
                                InternalMemberInterviewSummaryVO interviewSummary,
                                InternalMemberEmploymentProfileVO employmentProfile) {
        StringBuilder builder = new StringBuilder();
        builder.append(username)
                .append("从")
                .append(defaultText(studySummary.getPlanName(), "付费学习计划"))
                .append("出发，完成了")
                .append(valueText(studySummary.getCompletedCourseCount()))
                .append("门课程，累计投递")
                .append(valueText(resumeSummary.getResumeDeliveryCount()))
                .append("次简历、参与")
                .append(valueText(interviewSummary.getInterviewCompanyCount()))
                .append("家公司面试，最终在")
                .append(defaultText(employmentProfile.getEntryTime(), "就业阶段"))
                .append("入职")
                .append(defaultText(employmentProfile.getCompanyName(), "目标公司"))
                .append(defaultPosition(employmentProfile.getPositionName()))
                .append("。");
        return builder.toString();
    }

    private String buildStoryContent(String username,
                                     InternalMemberStudySummaryVO studySummary,
                                     InternalMemberStudyAttitudeVO studyAttitude,
                                     InternalMemberResumeSummaryVO resumeSummary,
                                     InternalMemberInterviewSummaryVO interviewSummary,
                                     InternalMemberExamSummaryVO examSummary,
                                     InternalMemberEmploymentProfileVO employmentProfile) {
        StringBuilder builder = new StringBuilder();
        builder.append(username)
                .append("加入")
                .append(defaultText(studySummary.getPlanName(), "付费学习计划"))
                .append("后，正式开启了自己的成长路径。")
                .append("从")
                .append(defaultText(studySummary.getFirstLearnTime(), "学习起点"))
                .append("开始，到")
                .append(defaultText(studySummary.getLastLearnTime(), "最近一次学习"))
                .append("为止，TA一共开启了")
                .append(valueText(studySummary.getStartedCourseCount()))
                .append("门课程，完成了")
                .append(valueText(studySummary.getCompletedCourseCount()))
                .append("门，整体学习进度达到")
                .append(decimalText(studySummary.getOverallProgress()))
                .append("%。");

        builder.append("\n\n");
        builder.append("在学习状态上，TA当前的学习态度评估为")
                .append(attitudeLabel(studyAttitude.getAttitude()))
                .append("，学习得分为")
                .append(valueText(studyAttitude.getScore()))
                .append("分。")
                .append("这说明TA在课程推进、活跃度和完成度上已经形成了相对稳定的节奏，也为后续求职打下了基础。");

        builder.append("\n\n");
        builder.append("进入求职阶段后，TA累计投递了")
                .append(valueText(resumeSummary.getResumeDeliveryCount()))
                .append("次简历，参与了")
                .append(valueText(interviewSummary.getInterviewCompanyCount()))
                .append("家公司面试，完成了")
                .append(valueText(interviewSummary.getCompletedInterviewCount()))
                .append("场面试流程。")
                .append("在面试轮次上，总计经历了")
                .append(valueText(interviewSummary.getInterviewRoundCount()))
                .append("轮，持续把学习成果转化为真实的求职反馈。");

        if (examSummary != null && safeNumber(examSummary.getReportedCount()) > 0) {
            builder.append("\n\n");
            builder.append("在阶段性测评方面，TA共参与了")
                    .append(valueText(examSummary.getReportedCount()))
                    .append("次考试或测评，其中通过")
                    .append(valueText(examSummary.getPassedCount()))
                    .append("次。")
                    .append("这些阶段性结果帮助TA及时调整了自己的学习和求职节奏。");
        }

        builder.append("\n\n");
        builder.append("最终，TA在")
                .append(defaultText(employmentProfile.getEntryTime(), "完成求职后"))
                .append("成功入职")
                .append(defaultText(employmentProfile.getCompanyName(), "目标公司"))
                .append(defaultPosition(employmentProfile.getPositionName()));
        if (StringUtils.isNotEmpty(employmentProfile.getCity())) {
            builder.append("，工作城市为").append(employmentProfile.getCity());
        }
        builder.append("。")
                .append("这段路径并不是一次结果性的展示，而是从学习投入、过程坚持到求职兑现的完整积累。");

        return builder.toString();
    }

    private String buildFactsJson(InternalMemberStudySummaryVO studySummary,
                                  InternalMemberStudyAttitudeVO studyAttitude,
                                  InternalMemberResumeSummaryVO resumeSummary,
                                  InternalMemberInterviewSummaryVO interviewSummary,
                                  InternalMemberExamSummaryVO examSummary,
                                  InternalMemberEmploymentProfileVO employmentProfile) {
        Map<String, Object> facts = new LinkedHashMap<String, Object>();
        facts.put("username", studySummary.getUsername());
        facts.put("memberType", studySummary.getMemberType());
        facts.put("planName", studySummary.getPlanName());
        facts.put("firstLearnTime", studySummary.getFirstLearnTime());
        facts.put("lastLearnTime", studySummary.getLastLearnTime());
        facts.put("startedCourseCount", studySummary.getStartedCourseCount());
        facts.put("completedCourseCount", studySummary.getCompletedCourseCount());
        facts.put("overallProgress", studySummary.getOverallProgress());
        facts.put("attitude", studyAttitude.getAttitude());
        facts.put("attitudeScore", studyAttitude.getScore());
        facts.put("attitudeSignals", studyAttitude.getSignals());
        facts.put("resumeDeliveryCount", resumeSummary.getResumeDeliveryCount());
        facts.put("interviewCompanyCount", interviewSummary.getInterviewCompanyCount());
        facts.put("completedInterviewCount", interviewSummary.getCompletedInterviewCount());
        facts.put("interviewRoundCount", interviewSummary.getInterviewRoundCount());
        facts.put("examReportedCount", examSummary.getReportedCount());
        facts.put("examPassedCount", examSummary.getPassedCount());
        facts.put("companyName", employmentProfile.getCompanyName());
        facts.put("positionName", employmentProfile.getPositionName());
        facts.put("city", employmentProfile.getCity());
        facts.put("entryTime", employmentProfile.getEntryTime());
        return JSON.toJSONString(facts);
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.isNotEmpty(value) ? value : fallback;
    }

    private String defaultPosition(String positionName) {
        if (StringUtils.isEmpty(positionName)) {
            return "";
        }
        return positionName + "岗位";
    }

    private String valueText(Number value) {
        return String.valueOf(safeNumber(value));
    }

    private int safeNumber(Number value) {
        return value == null ? 0 : value.intValue();
    }

    private String decimalText(Number value) {
        return value == null ? "0" : String.valueOf(value);
    }

    private String attitudeLabel(String attitude) {
        if ("positive".equals(attitude)) {
            return "积极";
        }
        if ("negative".equals(attitude)) {
            return "待提升";
        }
        return "稳定";
    }

    private static class GenerationContext {
        private InternalMemberStudySummaryVO studySummary;
        private InternalMemberStudyAttitudeVO studyAttitude;
        private InternalMemberEmploymentProfileVO employmentProfile;
        private InternalMemberResumeSummaryVO resumeSummary;
        private InternalMemberInterviewSummaryVO interviewSummary;
        private InternalMemberExamSummaryVO examSummary;

        private InternalMemberStudySummaryVO getStudySummary() {
            return studySummary;
        }

        private void setStudySummary(InternalMemberStudySummaryVO studySummary) {
            this.studySummary = studySummary;
        }

        private InternalMemberStudyAttitudeVO getStudyAttitude() {
            return studyAttitude;
        }

        private void setStudyAttitude(InternalMemberStudyAttitudeVO studyAttitude) {
            this.studyAttitude = studyAttitude;
        }

        private InternalMemberEmploymentProfileVO getEmploymentProfile() {
            return employmentProfile;
        }

        private void setEmploymentProfile(InternalMemberEmploymentProfileVO employmentProfile) {
            this.employmentProfile = employmentProfile;
        }

        private InternalMemberResumeSummaryVO getResumeSummary() {
            return resumeSummary;
        }

        private void setResumeSummary(InternalMemberResumeSummaryVO resumeSummary) {
            this.resumeSummary = resumeSummary;
        }

        private InternalMemberInterviewSummaryVO getInterviewSummary() {
            return interviewSummary;
        }

        private void setInterviewSummary(InternalMemberInterviewSummaryVO interviewSummary) {
            this.interviewSummary = interviewSummary;
        }

        private InternalMemberExamSummaryVO getExamSummary() {
            return examSummary;
        }

        private void setExamSummary(InternalMemberExamSummaryVO examSummary) {
            this.examSummary = examSummary;
        }
    }

    private static class StoryRenderResult {
        private String storyTitle;
        private String storySummary;
        private String storyContent;
        private String factsJson;

        private String getStoryTitle() {
            return storyTitle;
        }

        private void setStoryTitle(String storyTitle) {
            this.storyTitle = storyTitle;
        }

        private String getStorySummary() {
            return storySummary;
        }

        private void setStorySummary(String storySummary) {
            this.storySummary = storySummary;
        }

        private String getStoryContent() {
            return storyContent;
        }

        private void setStoryContent(String storyContent) {
            this.storyContent = storyContent;
        }

        private String getFactsJson() {
            return factsJson;
        }

        private void setFactsJson(String factsJson) {
            this.factsJson = factsJson;
        }
    }
}
