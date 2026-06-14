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
import com.backstage.system.domain.internalmember.vo.InternalMemberCourseProgressVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberEmploymentProfileVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberFollowRecordVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberOutcomeVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberStudyOverviewVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberStudySummaryVO;
import com.backstage.system.service.internalmember.IInternalMemberService;
import com.backstage.system.service.internalmember.IInternalMemberStudyService;
import com.backstage.system.utils.UserContextUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@Api(tags = "内部成员模块-管理后台接口")
@RestController
@RequestMapping("/pc/admin/internal-member")
public class InternalMemberAdminController extends BaseController {
    @Resource
    private IInternalMemberService internalMemberService;
    @Resource
    private IInternalMemberStudyService internalMemberStudyService;

    @ApiOperation("分页查询付费成员学习总览列表")
    @PreAuthorize("hasAuthority('internal:member:list')")
    @GetMapping("/page")
    public TableDataInfo page(InternalMemberPageQueryDTO query) {
        ensureAdmin();
        startPage();
        List<InternalMemberStudyOverviewVO> list = internalMemberStudyService.selectPaidMemberStudyOverview(query);
        return getDataTable(list);
    }

    @ApiOperation("分页查询成员就业结果统计列表")
    @PreAuthorize("hasAuthority('internal:member:list')")
    @GetMapping("/outcome/page")
    public TableDataInfo outcomePage(InternalMemberOutcomeQueryDTO query) {
        ensureAdmin();
        startPage();
        List<InternalMemberOutcomeVO> list = internalMemberService.selectOutcomeList(query);
        return getDataTable(list);
    }

    @ApiOperation("查询单个付费成员学习汇总信息")
    @PreAuthorize("hasAuthority('internal:member:detail')")
    @GetMapping("/{userId}/summary")
    public R<InternalMemberStudySummaryVO> summary(
            @ApiParam(value = "付费成员用户ID", required = true) @PathVariable("userId") Long userId) {
        ensureAdmin();
        return R.ok(internalMemberStudyService.selectMemberStudySummary(userId));
    }

    @ApiOperation("分页查询单个付费成员课程学习进度明细")
    @PreAuthorize("hasAuthority('internal:member:detail')")
    @GetMapping("/{userId}/courses")
    public TableDataInfo courseDetail(
            @ApiParam(value = "付费成员用户ID", required = true) @PathVariable("userId") Long userId,
            InternalMemberCourseDetailQueryDTO query) {
        ensureAdmin();
        startPage();
        List<InternalMemberCourseProgressVO> list = internalMemberStudyService.selectMemberCourseProgressList(userId, query);
        return getDataTable(list);
    }

    @ApiOperation("查询成员就业档案详情")
    @PreAuthorize("hasAuthority('internal:member:detail')")
    @GetMapping("/{userId}/employment-profile")
    public R<InternalMemberEmploymentProfileVO> employmentProfile(
            @ApiParam(value = "付费成员用户ID", required = true) @PathVariable("userId") Long userId) {
        ensureAdmin();
        return R.ok(internalMemberService.selectEmploymentProfile(userId));
    }

    @ApiOperation("新增或更新成员就业档案")
    @PreAuthorize("hasAuthority('internal:member:detail')")
    @PutMapping("/{userId}/employment-profile")
    public R<Void> saveEmploymentProfile(
            @ApiParam(value = "付费成员用户ID", required = true) @PathVariable("userId") Long userId,
            @RequestBody InternalMemberEmploymentProfileSaveDTO dto) {
        ensureAdmin();
        internalMemberService.saveEmploymentProfile(userId, dto, currentOperatorId());
        return R.ok();
    }

    @ApiOperation("查询成员跟进记录列表")
    @PreAuthorize("hasAuthority('internal:member:detail')")
    @GetMapping("/{userId}/follow-records")
    public R<List<InternalMemberFollowRecordVO>> followRecords(
            @ApiParam(value = "付费成员用户ID", required = true) @PathVariable("userId") Long userId) {
        ensureAdmin();
        return R.ok(internalMemberService.selectFollowRecordList(userId));
    }

    @ApiOperation("新增成员跟进记录")
    @PreAuthorize("hasAuthority('internal:member:detail')")
    @PostMapping("/{userId}/follow-records")
    public R<Void> addFollowRecord(
            @ApiParam(value = "付费成员用户ID", required = true) @PathVariable("userId") Long userId,
            @RequestBody InternalMemberFollowRecordSaveDTO dto) {
        ensureAdmin();
        internalMemberService.addFollowRecord(userId, dto, currentOperatorId());
        return R.ok();
    }

    private Long currentOperatorId() {
        Long userId = UserContextUtil.getCurrentUserId();
        if (userId == null) {
            throw new ServiceException("User not logged in", HttpStatus.UNAUTHORIZED);
        }
        return userId;
    }

    private void ensureAdmin() {
        try {
            Integer level = UserContextUtil.getCurrentLevel();
            if (level == null || level < 4) {
                throw new ServiceException("No permission", HttpStatus.FORBIDDEN);
            }
        } catch (ServiceException exception) {
            throw exception;
        } catch (Exception e) {
            throw new ServiceException("Permission validation failed", HttpStatus.FORBIDDEN);
        }
    }
}
