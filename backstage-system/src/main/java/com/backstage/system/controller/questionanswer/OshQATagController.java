package com.backstage.system.controller.questionanswer;

import com.backstage.common.annotation.OshUserEvent;
import com.backstage.common.core.domain.R;
import com.backstage.system.domain.questionanswer.vo.QATagVO;
import com.backstage.system.service.questionanswer.IOshQATagService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/pc/qna/tag")
public class OshQATagController {

    @Autowired
    private IOshQATagService oshQATagService;

    @ApiOperation("标签录入")
    @GetMapping("/search")
    @OshUserEvent(module = "答疑模块", actionType = "查询", description = "标签录入")
    @PreAuthorize("hasAuthority('qna:tag:search')")
    public R<List<QATagVO>> searchTags(@RequestParam(value = "type", required = false) String type) {
        return oshQATagService.searchTags(type);
    }
}
