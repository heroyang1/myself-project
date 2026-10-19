package com.backstage.system.controller.contentbg;

import com.backstage.common.annotation.Anonymous;
import com.backstage.common.annotation.OshUserLevel;
import com.backstage.common.core.domain.R;
import com.backstage.common.exception.ServiceException;
import com.backstage.system.domain.user.OshUser;
import com.backstage.system.domain.vo.contentbg.ContentBackgroundOptionsVO;
import com.backstage.system.domain.vo.contentbg.ContentBgImageUploadVO;
import com.backstage.system.request.contentbg.ContentBgSelectRequest;
import com.backstage.system.service.contentbg.IContentBackgroundService;
import com.backstage.system.utils.UserContextUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@Api(tags = "内容背景管理")
@RestController
@RequestMapping("/pc/content-background")
public class ContentBackgroundController {

    @Autowired
    private IContentBackgroundService contentBackgroundService;

    @ApiOperation("按风格类型上传内容背景图")
    @PostMapping("/image/upload")
    @OshUserLevel(value = 5)
    public R<ContentBgImageUploadVO> uploadImage(@RequestParam("styleCode") String styleCode,
                                                 @RequestParam(value = "name", required = false) String name,
                                                 @RequestParam("file") MultipartFile file,
                                                 @RequestParam(value = "dominantColor", required = false) String dominantColor,
                                                 @RequestParam(value = "overlayColor", required = false) String overlayColor,
                                                 @RequestParam(value = "opacity", required = false) BigDecimal opacity,
                                                 @RequestParam(value = "sort", required = false) Integer sort,
                                                 @RequestParam(value = "isDefault", required = false) Boolean isDefault) {
        OshUser currentUser = UserContextUtil.getCurrentUser();
        String operator = currentUser == null ? "system" : currentUser.getUsername();
        try {
            return R.ok(contentBackgroundService.uploadImage(
                    styleCode,
                    name,
                    file,
                    dominantColor,
                    overlayColor,
                    opacity,
                    sort,
                    isDefault,
                    operator
            ));
        } catch (ServiceException | IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        } catch (Exception ex) {
            return R.fail("上传背景图失败：" + ex.getMessage());
        }
    }

    @ApiOperation("获取内容背景切换选项")
    @GetMapping("/options")
    @Anonymous
    public R<ContentBackgroundOptionsVO> getOptions() {
        Long userId = UserContextUtil.getCurrentUserIdSafely();
        return R.ok(contentBackgroundService.getOptions(userId));
    }

    @ApiOperation("保存内容背景选择")
    @PostMapping("/setting/save")
    @OshUserLevel(value = 1)
    public R<ContentBackgroundOptionsVO> saveSetting(@RequestBody ContentBgSelectRequest request) {
        OshUser currentUser = UserContextUtil.getCurrentUser();
        if (currentUser == null) {
            return R.fail("请先登录");
        }
        try {
            return R.ok(contentBackgroundService.saveSelection(currentUser.getId(), currentUser.getUsername(), request));
        } catch (ServiceException | IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        } catch (Exception ex) {
            return R.fail("保存背景选择失败：" + ex.getMessage());
        }
    }

    @ApiOperation("删除内容背景图")
    @PostMapping("/image/delete")
    @OshUserLevel(value = 5)
    public R<Void> deleteImage(@RequestParam("imageId") Long imageId) {
        OshUser currentUser = UserContextUtil.getCurrentUser();
        if (currentUser == null) {
            return R.fail("请先登录");
        }
        try {
            contentBackgroundService.deleteImage(currentUser.getId(), currentUser.getUsername(), imageId);
            return R.ok();
        } catch (ServiceException | IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        } catch (Exception ex) {
            return R.fail("删除背景图失败：" + ex.getMessage());
        }
    }
}
