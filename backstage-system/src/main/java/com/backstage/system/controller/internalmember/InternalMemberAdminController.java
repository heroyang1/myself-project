package com.backstage.system.controller.internalmember;

import com.backstage.common.constant.HttpStatus;
import com.backstage.common.core.controller.BaseController;
import com.backstage.common.core.domain.R;
import com.backstage.common.core.page.TableDataInfo;
import com.backstage.common.exception.ServiceException;
import com.backstage.system.domain.internalmember.dto.InternalMemberCourseDetailQueryDTO;
import com.backstage.system.domain.internalmember.dto.InternalMemberEmploymentProfileSaveDTO;
import com.backstage.system.domain.internalmember.dto.InternalMemberFollowRecordSaveDTO;
import com.backstage.system.domain.internalmember.dto.InternalMemberOutcomeQueryDTO;
import com.backstage.system.domain.internalmember.dto.InternalMemberPageQueryDTO;
import com.backstage.system.domain.internalmember.vo.InternalMemberAdminUserOptionVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberCourseProgressVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberExamSummaryVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberEmploymentProfileVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberFollowRecordVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberInterviewSummaryVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberOutcomeVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberResumeSummaryVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberSalarySummaryVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberStudyAttitudeVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberStudyOverviewVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberStudySummaryVO;
import com.backstage.system.service.internalmember.IInternalMemberService;
import com.backstage.system.service.internalmember.IInternalMemberStudyService;
import com.backstage.system.utils.UserContextUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@Api(tags = "Internal Member Admin APIs")
@RestController
@Validated
@RequestMapping("/pc/admin/internal-member")
public class InternalMemberAdminController extends BaseController {
    @Resource
    private IInternalMemberService internalMemberService;
    @Resource
    private IInternalMemberStudyService internalMemberStudyService;

    @ApiOperation("Admin user option list")
    @GetMapping("/admin-users")
    public R<List<InternalMemberAdminUserOptionVO>> adminUsers(
            @ApiParam(value = "Optional keyword for username/email/role search")
            @RequestParam(value = "keyword", required = false) String keyword) {
        return R.ok(internalMemberService.selectAdminUserOptions(keyword));
    }

    @ApiOperation("Paid member overview page")
    @GetMapping("/page")
    public TableDataInfo page(InternalMemberPageQueryDTO query) {
        startPage();
        List<InternalMemberStudyOverviewVO> list = internalMemberStudyService.selectPaidMemberStudyOverview(query);
        return getDataTable(list);
    }

    @ApiOperation("Paid member outcome page")
    @GetMapping("/outcome/page")
    public TableDataInfo outcomePage(InternalMemberOutcomeQueryDTO query) {
        startPage();
        List<InternalMemberOutcomeVO> list = internalMemberService.selectOutcomeList(query);
        return getDataTable(list);
    }

    @ApiOperation("Member study summary")
    @GetMapping("/{userId}/summary")
    public R<InternalMemberStudySummaryVO> summary(
            @ApiParam(value = "Paid member user id", required = true) @PathVariable("userId") Long userId) {
        return R.ok(internalMemberStudyService.selectMemberStudySummary(userId));
    }

    @ApiOperation("Member course progress list")
    @GetMapping("/{userId}/courses")
    public TableDataInfo courseDetail(
            @ApiParam(value = "Paid member user id", required = true) @PathVariable("userId") Long userId,
            InternalMemberCourseDetailQueryDTO query) {
        startPage();
        List<InternalMemberCourseProgressVO> list = internalMemberStudyService.selectMemberCourseProgressList(userId, query);
        return getDataTable(list);
    }

    @ApiOperation("Employment profile detail")
    @GetMapping("/{userId}/employment-profile")
    public R<InternalMemberEmploymentProfileVO> employmentProfile(
            @ApiParam(value = "Paid member user id", required = true) @PathVariable("userId") Long userId) {
        return R.ok(internalMemberService.selectEmploymentProfile(userId));
    }

    @ApiOperation("Create or update employment profile")
    @PutMapping("/{userId}/employment-profile")
    public R<Void> saveEmploymentProfile(
            @ApiParam(value = "Paid member user id", required = true) @PathVariable("userId") Long userId,
            @Validated @RequestBody InternalMemberEmploymentProfileSaveDTO dto) {
        internalMemberService.saveEmploymentProfile(userId, dto, currentOperatorId());
        return R.ok();
    }

    @ApiOperation("Follow records")
    @GetMapping("/{userId}/follow-records")
    public R<List<InternalMemberFollowRecordVO>> followRecords(
            @ApiParam(value = "Paid member user id", required = true) @PathVariable("userId") Long userId) {
        return R.ok(internalMemberService.selectFollowRecordList(userId));
    }

    @ApiOperation("Add follow record")
    @PostMapping("/{userId}/follow-records")
    public R<Void> addFollowRecord(
            @ApiParam(value = "Paid member user id", required = true) @PathVariable("userId") Long userId,
            @Validated @RequestBody InternalMemberFollowRecordSaveDTO dto) {
        internalMemberService.addFollowRecord(userId, dto, currentOperatorId());
        return R.ok();
    }

    @ApiOperation("Exam summary")
    @GetMapping("/{userId}/exam-summary")
    public R<InternalMemberExamSummaryVO> examSummary(
            @ApiParam(value = "Paid member user id", required = true) @PathVariable("userId") Long userId) {
        return R.ok(internalMemberService.selectExamSummary(userId));
    }

    @ApiOperation("Study attitude")
    @GetMapping("/{userId}/study-attitude")
    public R<InternalMemberStudyAttitudeVO> studyAttitude(
            @ApiParam(value = "Paid member user id", required = true) @PathVariable("userId") Long userId) {
        return R.ok(internalMemberStudyService.selectStudyAttitude(userId));
    }

    @ApiOperation("Salary summary")
    @GetMapping("/{userId}/salary-summary")
    public R<InternalMemberSalarySummaryVO> salarySummary(
            @ApiParam(value = "Paid member user id", required = true) @PathVariable("userId") Long userId) {
        return R.ok(internalMemberService.selectSalarySummary(userId));
    }

    @ApiOperation("Resume delivery summary")
    @GetMapping("/{userId}/resume-summary")
    public R<InternalMemberResumeSummaryVO> resumeSummary(
            @ApiParam(value = "Paid member user id", required = true) @PathVariable("userId") Long userId) {
        return R.ok(internalMemberService.selectResumeSummary(userId));
    }

    @ApiOperation("Interview summary")
    @GetMapping("/{userId}/interview-summary")
    public R<InternalMemberInterviewSummaryVO> interviewSummary(
            @ApiParam(value = "Paid member user id", required = true) @PathVariable("userId") Long userId) {
        return R.ok(internalMemberService.selectInterviewSummary(userId));
    }

    private Long currentOperatorId() {
        Long userId = UserContextUtil.getCurrentUserId();
        if (userId == null) {
            throw new ServiceException("User not logged in", HttpStatus.UNAUTHORIZED);
        }
        return userId;
    }
}
