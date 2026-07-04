package com.backstage.system.controller.questionanswer;

import com.backstage.common.annotation.Anonymous;
import com.backstage.common.annotation.OshUserEvent;
import com.backstage.common.constant.ResourceType;
import com.backstage.common.core.domain.R;
import com.backstage.system.domain.announcement.vo.AnnouncementMarqueeVO;
import com.backstage.system.service.questionanswer.IOshQAAnnouncementService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/pc/qna/announcement")
public class OshQAAnnouncementController {

    @Autowired
    private IOshQAAnnouncementService oshQAAnnouncementService;

    @ApiOperation("查询答疑模块系统公告")
    @GetMapping("/notice")
    @Anonymous
    @OshUserEvent(module = "答疑模块", actionType = "查询", description = "查询答疑模块系统公告", resourceType = ResourceType.QA_QUESTION_TYPE, recordAnonymous = true)
    public R<List<AnnouncementMarqueeVO>> listNoticeAnnouncements(
            @RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        return R.ok(oshQAAnnouncementService.listNoticeAnnouncements(limit));
    }

    @ApiOperation("查询答疑模块业务公告")
    @GetMapping("/dynamic")
    @Anonymous
    @OshUserEvent(module = "答疑模块", actionType = "查询", description = "查询答疑模块业务公告", resourceType = ResourceType.QA_QUESTION_TYPE, recordAnonymous = true)
    public R<List<AnnouncementMarqueeVO>> listDynamicAnnouncements(
            @RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        return R.ok(oshQAAnnouncementService.listDynamicAnnouncements(limit));
    }
}
