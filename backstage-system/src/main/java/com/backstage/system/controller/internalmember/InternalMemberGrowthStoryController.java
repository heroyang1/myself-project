package com.backstage.system.controller.internalmember;

import com.backstage.common.core.controller.BaseController;
import com.backstage.common.core.domain.R;
import com.backstage.common.core.page.TableDataInfo;
import com.backstage.common.exception.ServiceException;
import com.backstage.system.domain.internalmember.dto.InternalMemberGrowthStoryPageQueryDTO;
import com.backstage.system.domain.internalmember.dto.InternalMemberGrowthStoryVisibilityUpdateDTO;
import com.backstage.system.domain.internalmember.vo.InternalMemberGrowthStoryDetailVO;
import com.backstage.system.domain.internalmember.vo.InternalMemberGrowthStoryPageVO;
import com.backstage.system.service.internalmember.IInternalMemberGrowthStoryService;
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
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@Api(tags = "Internal Member Growth Story APIs")
@RestController
@Validated
@RequestMapping("/pc/user/internal-member/growth-story")
public class InternalMemberGrowthStoryController extends BaseController {
    @Resource
    private IInternalMemberGrowthStoryService internalMemberGrowthStoryService;

    @ApiOperation("Generate my growth story")
    @PostMapping("/generate")
    public R<InternalMemberGrowthStoryDetailVO> generate() {
        return R.ok(internalMemberGrowthStoryService.generateMyStory(currentUserId()));
    }

    @ApiOperation("Get my growth story")
    @GetMapping("/me")
    public R<InternalMemberGrowthStoryDetailVO> me() {
        return R.ok(internalMemberGrowthStoryService.getMyStory(currentUserId()));
    }

    @ApiOperation("Update my growth story visibility")
    @PutMapping("/visibility")
    public R<InternalMemberGrowthStoryDetailVO> updateVisibility(
            @Validated @RequestBody InternalMemberGrowthStoryVisibilityUpdateDTO dto) {
        return R.ok(internalMemberGrowthStoryService.updateMyStoryVisibility(currentUserId(), dto));
    }

    @ApiOperation("Page public growth stories")
    @GetMapping("/page")
    public TableDataInfo page(InternalMemberGrowthStoryPageQueryDTO query) {
        startPage();
        List<InternalMemberGrowthStoryPageVO> list =
                internalMemberGrowthStoryService.selectPublicStoryPage(currentUserId(), query);
        return getDataTable(list);
    }

    @ApiOperation("Get public growth story detail")
    @GetMapping("/{targetUserId}")
    public R<InternalMemberGrowthStoryDetailVO> detail(
            @ApiParam(value = "Target user id", required = true) @PathVariable("targetUserId") Long targetUserId) {
        return R.ok(internalMemberGrowthStoryService.getStoryDetail(currentUserId(), targetUserId));
    }

    private Long currentUserId() {
        Long userId = UserContextUtil.getCurrentUserIdSafely();
        if (userId == null) {
            throw new ServiceException("Please login first");
        }
        return userId;
    }
}
