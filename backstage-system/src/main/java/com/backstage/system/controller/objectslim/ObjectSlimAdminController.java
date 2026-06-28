package com.backstage.system.controller.objectslim;

import com.backstage.common.annotation.OshUserLevel;
import com.backstage.common.core.domain.R;
import com.backstage.system.domain.objectslim.OshObjectSlimTask;
import com.backstage.system.domain.objectslim.dto.ObjectSlimDefaultModeRequest;
import com.backstage.system.domain.objectslim.dto.ObjectSlimEnabledRequest;
import com.backstage.system.domain.objectslim.dto.ObjectSlimPageQuery;
import com.backstage.system.domain.objectslim.dto.ObjectSlimRunRequest;
import com.backstage.system.domain.objectslim.dto.ObjectSlimScanRequest;
import com.backstage.system.domain.objectslim.vo.ObjectSlimConfigVO;
import com.backstage.system.domain.objectslim.vo.ObjectSlimOverviewVO;
import com.backstage.system.domain.objectslim.vo.ObjectSlimRunResultVO;
import com.backstage.system.service.objectslim.ObjectSlimService;
import com.github.pagehelper.PageInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.LinkedHashMap;

@RestController
@RequestMapping("/pc/admin/object-slim")
public class ObjectSlimAdminController {
    @Resource
    private ObjectSlimService objectSlimService;

    @GetMapping("/config")
    @OshUserLevel(5)
    public R<ObjectSlimConfigVO> config() {
        return R.ok(objectSlimService.getConfig());
    }

    @PostMapping("/config/enabled")
    @OshUserLevel(5)
    public R<ObjectSlimConfigVO> setEnabled(@RequestBody ObjectSlimEnabledRequest request) {
        boolean enabled = request != null && Boolean.TRUE.equals(request.getEnabled());
        return R.ok(objectSlimService.setEnabled(enabled));
    }

    @PostMapping("/config/default-mode")
    @OshUserLevel(5)
    public R<ObjectSlimConfigVO> setDefaultMode(@RequestBody ObjectSlimDefaultModeRequest request) {
        return R.ok(objectSlimService.setDefaultSlimMode(request == null ? null : request.getSlimMode()));
    }

    @GetMapping("/overview")
    @OshUserLevel(5)
    public R<ObjectSlimOverviewVO> overview() {
        return R.ok(objectSlimService.overview());
    }

    @PostMapping("/page")
    @OshUserLevel(5)
    public R<LinkedHashMap<String, Object>> page(@RequestBody(required = false) ObjectSlimPageQuery query) {
        PageInfo<OshObjectSlimTask> pageInfo = objectSlimService.page(query);
        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("total", pageInfo.getTotal());
        data.put("rows", pageInfo.getList());
        return R.ok(data);
    }

    @PostMapping("/scan")
    @OshUserLevel(5)
    public R<ObjectSlimRunResultVO> scan(@RequestBody(required = false) ObjectSlimScanRequest request) {
        return R.ok(objectSlimService.scan(request));
    }

    @PostMapping("/slim")
    @OshUserLevel(5)
    public R<ObjectSlimRunResultVO> slim(@RequestBody(required = false) ObjectSlimRunRequest request) {
        return R.ok(objectSlimService.runSlim(request));
    }

    @PostMapping("/retry")
    @OshUserLevel(5)
    public R<Void> retry(@RequestParam Long id) {
        objectSlimService.retry(id);
        return R.ok();
    }

    @PostMapping("/rollback")
    @OshUserLevel(5)
    public R<Void> rollback(@RequestParam Long id) {
        objectSlimService.rollback(id);
        return R.ok();
    }

    @PostMapping("/backup/delete")
    @OshUserLevel(5)
    public R<Void> deleteBackup(@RequestParam Long id) {
        objectSlimService.deleteBackup(id);
        return R.ok();
    }

    @GetMapping("/preview-url")
    @OshUserLevel(5)
    public R<String> previewUrl(@RequestParam Long id,
                                @RequestParam(value = "minute", required = false) Integer minute) {
        return R.ok(objectSlimService.getPreviewUrl(id, minute));
    }
}
