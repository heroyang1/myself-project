package com.backstage.system.controller.exam;

import com.backstage.common.annotation.Anonymous;
import com.backstage.common.core.controller.BaseController;
import com.backstage.common.core.domain.R;
import com.backstage.common.response.PageResponse;
import com.backstage.system.domain.dto.exam.ExamQuestionSaveDto;
import com.backstage.system.domain.dto.exam.ExamSaveDto;
import com.backstage.system.domain.dto.exam.ExamSearchDto;
import com.backstage.system.domain.exam.OshExamTag;
import com.backstage.system.domain.vo.exam.ExamDetailVo;
import com.backstage.system.service.exam.IOshExamService;
import com.backstage.system.domain.vo.exam.ExamVo;
import com.backstage.system.utils.UserContextUtil;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 考试模块 Controller
 * 路径保持 /pc/testpaper（兼容旧前端），新增管理接口走 /pc/exam
 */
@RestController
public class OshExamController extends BaseController {

    @Autowired
    private IOshExamService examService;

    // ─────────────────────────────────────────────
    // 公开接口（兼容旧路径 /pc/testpaper）
    // ─────────────────────────────────────────────

    /**
     * 考试列表（支持搜索/标签/资源过滤，回填 is_test/is_collected）
     * 兼容旧接口：GET /pc/testpaper/list?page=1&limit=10
     * 新接口：POST /pc/exam/search
     */
    @ApiOperation("考试列表")
    @PostMapping("/pc/exam/search")
    @Anonymous
    public R<PageResponse<ExamVo>> searchExams(@RequestBody ExamSearchDto dto) {
        Long userId = null;
        try { userId = UserContextUtil.getCurrentUserId(); } catch (Exception ignored) {} // 尽力获取登录上下文，匿名/无上下文时降级为 null，属预期行为
        return R.ok(examService.searchExams(dto, userId));
    }

    /**
     * 旧接口兼容：GET /pc/testpaper/list
     */
    @ApiOperation("考试列表（旧接口兼容）")
    @GetMapping("/pc/testpaper/list")
    @Anonymous
    public R<PageResponse<ExamVo>> listLegacy(
            @RequestParam(defaultValue = "1")  Integer page,
            @RequestParam(defaultValue = "12") Integer limit) {
        ExamSearchDto dto = new ExamSearchDto();
        dto.setPageNum(page);
        dto.setPageSize(limit);
        Long userId = null;
        try { userId = UserContextUtil.getCurrentUserId(); } catch (Exception ignored) {} // 尽力获取登录上下文，匿名/无上下文时降级为 null，属预期行为
        return R.ok(examService.searchExams(dto, userId));
    }

    /**
     * 开始考试 - 获取试卷详情（自动创建/复用考试记录，返回真实 user_test_id）
     * 需要 VIP+ 权限（level >= 2），后端校验
     */
    @ApiOperation("开始考试")
    @GetMapping("/pc/testpaper/read")
    @Anonymous
    public R<ExamDetailVo> read(@RequestParam Long id) {
        if (id == null) return R.fail("试卷ID不能为空");

        Long userId = null;
        try { userId = UserContextUtil.getCurrentUserId(); } catch (Exception ignored) {} // 尽力获取登录上下文，匿名/无上下文时降级为 null，属预期行为

        if (userId == null) return R.fail("请先登录");

        // 权限校验：level >= 2 才能参加考试
        // getCurrentLevel() 在 ThreadLocal 没有值时会抛 NPE，用 -1 兜底
        int level = -1;
        try {
            Integer lv = UserContextUtil.getCurrentLevel();
            if (lv != null) level = lv;
        } catch (Exception ignored) {} // 尽力获取登录上下文，匿名/无上下文时降级为 null，属预期行为

        if (level < 2) {
            return R.fail("需要 VIP 及以上等级才能参加考试");
        }

        ExamDetailVo detail = examService.getExamDetail(id, userId);
        if (detail == null) return R.fail("未找到相关考试内容");
        return R.ok(detail);
    }

    /**
     * 标签列表
     */
    @ApiOperation("考试标签列表")
    @GetMapping("/pc/exam/tag/list")
    @Anonymous
    public R<List<OshExamTag>> tagList() {
        return R.ok(examService.getTagList());
    }

    // ─────────────────────────────────────────────
    // 管理接口（需要对应权限）
    // ─────────────────────────────────────────────

    /**
     * 新增/修改考试
     */
    @ApiOperation("新增/修改考试")
    @PostMapping("/pc/exam/save")
    @PreAuthorize("hasAuthority('exam:create') or hasAuthority('exam:update') or @examLevelAuth.canManageExamByRoleLevel()")
    public R<Long> saveExam(@RequestBody ExamSaveDto dto) {
        String operator = null;
        try {
            com.backstage.system.domain.user.OshUser user = UserContextUtil.getCurrentUser();
            if (user != null) operator = user.getUsername();
        } catch (Exception ignored) {} // 尽力获取登录上下文，匿名/无上下文时降级为 null，属预期行为
        return examService.saveExam(dto, operator != null ? operator : "system");
    }

    /**
     * 删除考试
     */
    @ApiOperation("删除考试")
    @PostMapping("/pc/exam/delete")
    @PreAuthorize("hasAuthority('exam:delete') or @examLevelAuth.canManageExamByRoleLevel()")
    public R<String> deleteExam(@RequestParam Long id) {
        String operator = null;
        try {
            com.backstage.system.domain.user.OshUser user = UserContextUtil.getCurrentUser();
            if (user != null) operator = user.getUsername();
        } catch (Exception ignored) {} // 尽力获取登录上下文，匿名/无上下文时降级为 null，属预期行为
        return examService.deleteExam(id, operator != null ? operator : "system");
    }

    /**
     * 新增或修改一道题目
     */
    @ApiOperation("保存考试题目")
    @PostMapping("/pc/exam/question/save")
    @PreAuthorize("hasAuthority('exam:question:save') or hasAuthority('exam:update') or @examLevelAuth.canManageExamByRoleLevel()")
    public R<Long> saveExamQuestion(@RequestBody ExamQuestionSaveDto dto) {
        String operator = null;
        try {
            com.backstage.system.domain.user.OshUser user = UserContextUtil.getCurrentUser();
            if (user != null) operator = user.getUsername();
        } catch (Exception ignored) {} // 尽力获取登录上下文，匿名/无上下文时降级为 null，属预期行为
        return examService.saveExamQuestion(dto, operator != null ? operator : "system");
    }

    /**
     * 删除一道题目（软删除）
     */
    @ApiOperation("删除考试题目")
    @PostMapping("/pc/exam/question/delete")
    @PreAuthorize("hasAuthority('exam:question:delete') or hasAuthority('exam:delete') or @examLevelAuth.canManageExamByRoleLevel()")
    public R<String> deleteExamQuestion(@RequestParam Long id, @RequestParam Long examId) {
        String operator = null;
        try {
            com.backstage.system.domain.user.OshUser user = UserContextUtil.getCurrentUser();
            if (user != null) operator = user.getUsername();
        } catch (Exception ignored) {} // 尽力获取登录上下文，匿名/无上下文时降级为 null，属预期行为
        return examService.deleteExamQuestion(id, examId, operator != null ? operator : "system");
    }

    /**
     * 收藏/取消收藏考试
     */
    @ApiOperation("收藏/取消收藏考试")
    @PostMapping("/pc/exam/collect")
    @Anonymous
    public R<String> toggleCollect(@RequestParam Long examId) {
        Long userId = null;
        try { userId = UserContextUtil.getCurrentUserId(); } catch (Exception ignored) {} // 尽力获取登录上下文，匿名/无上下文时降级为 null，属预期行为
        if (userId == null) return R.fail("请先登录");

        String operator = null;
        try {
            com.backstage.system.domain.user.OshUser user = UserContextUtil.getCurrentUser();
            if (user != null) operator = user.getUsername();
        } catch (Exception ignored) {} // 尽力获取登录上下文，匿名/无上下文时降级为 null，属预期行为

        return examService.toggleCollect(examId, userId, operator != null ? operator : String.valueOf(userId));
    }
}
