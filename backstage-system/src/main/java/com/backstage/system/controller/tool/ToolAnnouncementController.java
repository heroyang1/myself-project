package com.backstage.system.controller.tool;

import com.backstage.common.annotation.Anonymous;
import com.backstage.common.core.domain.R;
import com.backstage.system.domain.vo.tool.ToolAnnouncementVO;
import com.backstage.system.service.tool.IToolAnnouncementService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api(tags = "工具公告")
@RestController
@RequestMapping("/pc/tool/announcement")
public class ToolAnnouncementController {

    @Autowired
    private IToolAnnouncementService toolAnnouncementService;

    @ApiOperation("查询工具模块系统公告")
    @GetMapping("/notice")
    @Anonymous
    public R<List<ToolAnnouncementVO>> listNoticeAnnouncements() {
        return R.ok(toolAnnouncementService.listNoticeAnnouncements());
    }

    @ApiOperation("查询工具模块业务动态")
    @GetMapping("/dynamic")
    @Anonymous
    public R<List<ToolAnnouncementVO>> listDynamicAnnouncements() {
        return R.ok(toolAnnouncementService.listDynamicAnnouncements());
    }

    @ApiOperation("查询工具模块系统通知")
    @GetMapping("/systemNotice/latest")
    @Anonymous
    public R<List<ToolAnnouncementVO>> listLatestSystemNotices() {
        return R.ok(toolAnnouncementService.listLatestSystemNotices());
    }

    @ApiOperation("查询工具模块业务公告")
    @GetMapping("/userNotice/latest")
    @Anonymous
    public R<List<ToolAnnouncementVO>> listLatestUserNotices() {
        return R.ok(toolAnnouncementService.listLatestUserNotices());
    }
}
