package com.backstage.system.controller.homepage;

import com.backstage.common.annotation.Anonymous;
import com.backstage.common.core.controller.BaseController;
import com.backstage.common.core.domain.R;

import com.backstage.system.domain.homepage.vo.HotCourseLatestSectionVO;
import com.backstage.system.domain.homepage.vo.HotCourseVO;
import com.backstage.system.service.homepage.IOshHomePageCourseService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 首页热门课程 Controller
 *
 * @author jayTatum
 */
@Api(tags = "首页-热门课程")
@RestController
@RequestMapping("/pc/homepage/course")
public class OshHomePageCourseController extends BaseController {

    @Autowired
    private IOshHomePageCourseService homePageCourseService;

    /**
     * 首页热门课程（无需登录）
     * 基于热度公式排序，返回 Top N 课程及其标签
     */
    @ApiOperation("首页热门课程")
    @GetMapping("/hot")
    @Anonymous
    public R<List<HotCourseVO>> hotCourses(
            @ApiParam(value = "返回数量", defaultValue = "5")
            @RequestParam(defaultValue = "5") Integer limit) {
        return R.ok(homePageCourseService.getHotCourses(limit));
    }

    @ApiOperation("首页热门课程最新更新小节")
    @GetMapping("/latest-updates")
    @Anonymous
    public R<List<HotCourseLatestSectionVO>> latestUpdates(
            @ApiParam(value = "课程ID", required = true)
            @RequestParam Long courseId,
            @ApiParam(value = "返回数量", defaultValue = "2")
            @RequestParam(defaultValue = "2") Integer limit) {
        return R.ok(homePageCourseService.getLatestCourseUpdates(courseId, limit));
    }
}
