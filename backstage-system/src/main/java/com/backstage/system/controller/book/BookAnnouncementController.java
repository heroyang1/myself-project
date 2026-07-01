package com.backstage.system.controller.book;

import com.backstage.common.annotation.Anonymous;
import com.backstage.common.core.domain.R;
import com.backstage.common.enums.AnnouncementChannelEnum;
import com.backstage.common.enums.AnnouncementModuleEnum;
import com.backstage.system.domain.announcement.vo.AnnouncementMarqueeVO;
import com.backstage.system.service.announcement.IAnnouncementMarqueeQueryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@Api(tags = "电子书公告")
@RestController
@RequestMapping("/pc/book/announcement")
public class BookAnnouncementController {

    @Resource
    private IAnnouncementMarqueeQueryService announcementMarqueeQueryService;

    @ApiOperation("查询电子书模块通知")
    @GetMapping("/notice")
    @Anonymous
    public R<List<AnnouncementMarqueeVO>> listNoticeAnnouncements(@RequestParam(defaultValue = "5") int limit) {
        return R.ok(announcementMarqueeQueryService.list(
                AnnouncementModuleEnum.BOOK,
                AnnouncementChannelEnum.SYSTEM_NOTICE,
                limit
        ));
    }

    @ApiOperation("查询电子书模块动态")
    @GetMapping("/dynamic")
    @Anonymous
    public R<List<AnnouncementMarqueeVO>> listDynamicAnnouncements(@RequestParam(defaultValue = "5") int limit) {
        return R.ok(announcementMarqueeQueryService.list(
                AnnouncementModuleEnum.BOOK,
                AnnouncementChannelEnum.USER_NOTICE,
                limit
        ));
    }
}
