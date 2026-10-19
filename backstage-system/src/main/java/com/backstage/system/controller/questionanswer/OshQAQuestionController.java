package com.backstage.system.controller.questionanswer;

import com.backstage.common.annotation.Anonymous;
import com.backstage.common.annotation.OshUserEvent;
import com.backstage.common.annotation.OshUserLevel;
import com.backstage.common.constant.ResourceType;
import com.backstage.common.core.domain.R;
import com.backstage.common.core.page.TableDataInfo;
import com.backstage.system.domain.questionanswer.dto.AddQuestionDTO;
import com.backstage.system.domain.questionanswer.dto.DeleteQuestionDTO;
import com.backstage.system.domain.questionanswer.dto.EditQuestionDTO;
import com.backstage.system.domain.questionanswer.dto.FollowQuestionDTO;
import com.backstage.system.domain.questionanswer.dto.PublishQuestionDTO;
import com.backstage.system.domain.questionanswer.dto.QueryQuestionDetailDTO;
import com.backstage.system.domain.questionanswer.dto.QueryQuestionListDTO;
import com.backstage.system.domain.questionanswer.dto.SolveQuestionDTO;
import com.backstage.system.domain.questionanswer.vo.QueryQuestionDetailVO;
import com.backstage.system.domain.questionanswer.vo.QueryQuestionListVO;
import com.backstage.system.mapper.course.OshCourseSectionMapper;
import com.backstage.system.service.IOshCourseService;
import com.backstage.system.service.questionanswer.IOshQAQuestionService;
import com.backstage.system.utils.UserContextUtil;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/pc/qna/question")
public class OshQAQuestionController {

    @Autowired
    private IOshQAQuestionService oshQAQuestionService;

    @Autowired
    private IOshCourseService oshCourseService;

    @Autowired
    private OshCourseSectionMapper oshCourseSectionMapper;

    @ApiOperation("新增问题")
    @PostMapping("/create")
    @OshUserEvent(module = "答疑模块", actionType = "新增", description = "新增问题", resourceType = ResourceType.QA_TAG_TYPE)
    @PreAuthorize("hasAuthority('qna:question:create')")
    public R<String> addQuestion(@RequestBody AddQuestionDTO addQuestionDTO) {
        Long userId = UserContextUtil.getCurrentUserId();
        if (userId == null) {
            return R.fail("请先登录");
        }
        if (!canAskQuestion(addQuestionDTO, userId)) {
            return R.fail("需要 VIP 及以上等级，或已购买该课程，才能提问");
        }
        return oshQAQuestionService.addQuestion(userId,
                addQuestionDTO == null ? null : addQuestionDTO.getResourceNo(),
                addQuestionDTO == null ? null : addQuestionDTO.getResourceType(),
                addQuestionDTO == null ? null : addQuestionDTO.getContent(),
                addQuestionDTO == null ? null : addQuestionDTO.getIsPaidOnly(),
                addQuestionDTO == null ? null : addQuestionDTO.getTags());
    }

    @ApiOperation("发布问题")
    @PostMapping("/publish")
    @OshUserEvent(module = "答疑模块", actionType = "新增", description = "发布问题", resourceType = ResourceType.QA_QUESTION_TYPE)
    @PreAuthorize("hasAuthority('qna:question:publish')")
    public R<String> publishQuestion(@RequestBody PublishQuestionDTO publishQuestionDTO) {
        return oshQAQuestionService.publishQuestion(UserContextUtil.getCurrentUserId(),
                publishQuestionDTO == null ? null : publishQuestionDTO.getQuestionId());
    }

    @ApiOperation("我的草稿")
    @GetMapping("/my/draft")
    @OshUserEvent(module = "答疑模块", actionType = "查询", description = "我的草稿", resourceType = ResourceType.QA_QUESTION_TYPE)
    @PreAuthorize("hasAuthority('qna:question:myDraft')")
    public R<List<QueryQuestionListVO>> myDraft() {
        return oshQAQuestionService.myDraft(UserContextUtil.getCurrentUserId());
    }

    @ApiOperation("编辑问题")
    @PostMapping("/my/draft/edit")
    @OshUserEvent(module = "答疑模块", actionType = "编辑", description = "编辑问题", resourceType = ResourceType.QA_QUESTION_TYPE)
    @PreAuthorize("hasAuthority('qna:question:myDraftEdit')")
    public R<String> editQuestion(@RequestBody EditQuestionDTO editQuestionDTO) {
        return oshQAQuestionService.editQuestion(UserContextUtil.getCurrentUserId(),
                editQuestionDTO == null ? null : editQuestionDTO.getQuestionId(),
                editQuestionDTO == null ? null : editQuestionDTO.getResourceNo(),
                editQuestionDTO == null ? null : editQuestionDTO.getResourceType(),
                editQuestionDTO == null ? null : editQuestionDTO.getContent(),
                editQuestionDTO == null ? null : editQuestionDTO.getIsPaidOnly(),
                editQuestionDTO == null ? null : editQuestionDTO.getTags());
    }

    @ApiOperation("删除问题")
    @PostMapping("/delete")
    @OshUserEvent(module = "答疑模块", actionType = "删除", description = "删除问题", resourceType = ResourceType.QA_QUESTION_TYPE)
    @PreAuthorize("hasAuthority('qna:question:delete')")
    public R<String> deleteQuestion(@RequestBody DeleteQuestionDTO deleteQuestionDTO) {
        return oshQAQuestionService.deleteQuestion(UserContextUtil.getCurrentUserId(),
                deleteQuestionDTO == null ? null : deleteQuestionDTO.getQuestionId());
    }

    @ApiOperation("关注问题")
    @PostMapping("/follow")
    @OshUserEvent(module = "答疑模块", actionType = "关注", description = "关注问题", resourceType = ResourceType.QA_QUESTION_TYPE)
    @PreAuthorize("hasAuthority('qna:question:follow')")
    public R<String> followQuestion(@RequestBody FollowQuestionDTO followQuestionDTO) {
        return oshQAQuestionService.followQuestion(UserContextUtil.getCurrentUserId(),
                followQuestionDTO == null ? null : followQuestionDTO.getQuestionId());
    }

    @ApiOperation("取消关注问题")
    @PostMapping("/follow/cancel")
    @OshUserEvent(module = "答疑模块", actionType = "取消关注", description = "取消关注问题", resourceType = ResourceType.QA_QUESTION_TYPE)
    @PreAuthorize("hasAuthority('qna:question:cancelFollow')")
    public R<String> cancelFollowQuestion(@RequestBody FollowQuestionDTO followQuestionDTO) {
        return oshQAQuestionService.cancelFollowQuestion(UserContextUtil.getCurrentUserId(),
                followQuestionDTO == null ? null : followQuestionDTO.getQuestionId());
    }

    @ApiOperation("问题列表")
    @PostMapping("/list")
    @OshUserEvent(module = "答疑模块", actionType = "查询", description = "问题列表", resourceType = ResourceType.QA_QUESTION_TYPE)
    @Anonymous
    public TableDataInfo list(@RequestBody(required = false) QueryQuestionListDTO queryQuestionListDTO) {
        if (queryQuestionListDTO == null) {
            queryQuestionListDTO = new QueryQuestionListDTO();
        }
        return oshQAQuestionService.list(UserContextUtil.getCurrentUserIdSafely(), queryQuestionListDTO.getResourceNo(),
                queryQuestionListDTO.getResourceType(), queryQuestionListDTO.getType(), queryQuestionListDTO.getKeyword(),
                queryQuestionListDTO.getPageNum(), queryQuestionListDTO.getPageSize());
    }

    @ApiOperation("采纳回答")
    @PostMapping("/solve")
    @OshUserEvent(module = "答疑模块", actionType = "修改", description = "采纳回答", resourceType = ResourceType.QA_ANSWER_TYPE)
    @OshUserLevel(value = 2)
    public R<String> solve(@RequestBody SolveQuestionDTO solveQuestionDTO) {
        return oshQAQuestionService.solve(UserContextUtil.getCurrentUserId(),
                solveQuestionDTO == null ? null : solveQuestionDTO.getQuestionId(),
                solveQuestionDTO == null ? null : solveQuestionDTO.getAnswerId());
    }

    @ApiOperation("取消采纳回答")
    @PostMapping("/cancel/solve")
    @OshUserEvent(module = "答疑模块", actionType = "修改", description = "取消采纳回答", resourceType = ResourceType.QA_ANSWER_TYPE)
    @OshUserLevel(value = 2)
    public R<String> cancelSolve(@RequestBody SolveQuestionDTO solveQuestionDTO) {
        return oshQAQuestionService.cancelSolve(UserContextUtil.getCurrentUserId(),
                solveQuestionDTO == null ? null : solveQuestionDTO.getQuestionId(),
                solveQuestionDTO == null ? null : solveQuestionDTO.getAnswerId());
    }

    @ApiOperation("问题详情")
    @PostMapping("/detail")
    @OshUserEvent(module = "答疑模块", actionType = "查询", description = "查询问题详情", resourceType = ResourceType.QA_QUESTION_TYPE)
    @Anonymous
    public R<QueryQuestionDetailVO> detail(@RequestBody(required = false) QueryQuestionDetailDTO queryQuestionDetailDTO) {
        Long questionId = queryQuestionDetailDTO == null ? null : queryQuestionDetailDTO.getQuestionId();
        return oshQAQuestionService.detail(UserContextUtil.getCurrentUserIdSafely(), questionId);
    }

    private boolean canAskQuestion(AddQuestionDTO addQuestionDTO, Long userId) {
        boolean hasPermission = false;
        try {
            hasPermission = UserContextUtil.getCurrentLevel() >= 2;
        } catch (Exception ignored) {
            // 无登录上下文时等级判断抛异常，按无权限处理（fail-closed）
        }
        Long sectionId = addQuestionDTO == null ? null : addQuestionDTO.getResourceNo();
        if (sectionId == null || hasPermission) {
            return true;
        }
        try {
            java.util.Map<String, Object> section = oshCourseSectionMapper.selectSectionById(sectionId);
            if (section == null || section.get("course_id") == null) {
                return false;
            }
            Long courseId = Long.parseLong(section.get("course_id").toString());
            return oshCourseService.hasUserBoughtCourse(courseId, userId);
        } catch (Exception ignored) {
            // 查询购买关系失败时拒绝放行（fail-closed），避免越权提问
            return false;
        }
    }
}
