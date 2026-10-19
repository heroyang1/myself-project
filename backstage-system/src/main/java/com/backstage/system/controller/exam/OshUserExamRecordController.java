package com.backstage.system.controller.exam;

import com.backstage.common.annotation.Anonymous;
import com.backstage.common.core.controller.BaseController;
import com.backstage.common.core.domain.R;
import com.backstage.system.domain.dto.exam.UserExamSaveDto;
import com.backstage.system.domain.vo.exam.UserExamRecordVo;
import com.backstage.system.domain.vo.exam.UserExamResultVo;
import com.backstage.system.service.exam.IOshUserExamRecordService;
import com.backstage.system.utils.UserContextUtil;
import com.github.pagehelper.PageInfo;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/pc/user_test")
public class OshUserExamRecordController extends BaseController {

    @Autowired
    private IOshUserExamRecordService userTestService;

    /**
     * 我的考试记录列表
     */
    @ApiOperation("我的考试记录")
    @GetMapping("/list")
    @Anonymous
    public R list(Integer page) {
        Long userId = null;
        try { userId = UserContextUtil.getCurrentUserId(); } catch (Exception ignored) {} // 尽力获取登录上下文，匿名/无上下文时降级为 null，属预期行为
        if (userId == null) return R.fail("请先登录");

        if (page == null) page = 1;
        List<UserExamRecordVo> list = userTestService.selectUserTestList(page);
        Map<String, Object> data = new HashMap<>();
        data.put("rows", list);
        data.put("count", new PageInfo(list).getTotal());
        return R.ok(data);
    }

    /**
     * 提交答卷
     */
    @ApiOperation("提交答卷")
    @PostMapping("/save")
    @Anonymous
    public R save(@RequestBody UserExamSaveDto saveDto) {
        Long userId = null;
        try { userId = UserContextUtil.getCurrentUserId(); } catch (Exception ignored) {} // 尽力获取登录上下文，匿名/无上下文时降级为 null，属预期行为
        if (userId == null) return R.fail("请先登录");

        // 将真实 userId 注入 DTO，防止前端伪造
        saveDto.setUser_test_id(userId);

        boolean result = userTestService.saveUserExam(saveDto);
        return result ? R.ok("交卷成功") : R.fail("交卷失败");
    }

    /**
     * 考试结果详情与错题解析
     */
    @ApiOperation("考试结果详情")
    @GetMapping("/detail")
    @Anonymous
    public R detail(Long id) {
        Long userId = null;
        try { userId = UserContextUtil.getCurrentUserId(); } catch (Exception ignored) {} // 尽力获取登录上下文，匿名/无上下文时降级为 null，属预期行为
        if (userId == null) return R.fail("请先登录");

        UserExamResultVo result = userTestService.getUserExamResult(id);
        return result == null ? R.fail("考试结果不存在") : R.ok(result);
    }
}
