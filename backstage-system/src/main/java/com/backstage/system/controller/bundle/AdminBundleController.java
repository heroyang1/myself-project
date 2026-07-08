package com.backstage.system.controller.bundle;

import com.backstage.common.annotation.OshUserLevel;
import com.backstage.common.core.domain.R;
import com.backstage.system.domain.bundle.dto.BundlePageQuery;
import com.backstage.system.domain.bundle.dto.BundleResourceSearchQuery;
import com.backstage.system.domain.bundle.dto.BundleSaveRequest;
import com.backstage.system.domain.bundle.dto.BundleStatusRequest;
import com.backstage.system.domain.bundle.vo.BundleDetailVO;
import com.backstage.system.domain.bundle.vo.BundleListVO;
import com.backstage.system.domain.bundle.vo.BundleResourceVO;
import com.backstage.system.service.bundle.ProductBundleService;
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
@RequestMapping("/pc/admin/bundle")
public class AdminBundleController {
    @Resource
    private ProductBundleService productBundleService;

    @PostMapping("/page")
    @OshUserLevel(5)
    public R<LinkedHashMap<String, Object>> page(@RequestBody(required = false) BundlePageQuery query) {
        PageInfo<BundleListVO> pageInfo = productBundleService.pageAdminBundles(query);
        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("total", pageInfo.getTotal());
        data.put("rows", pageInfo.getList());
        return R.ok(data);
    }

    @GetMapping("/detail")
    @OshUserLevel(5)
    public R<BundleDetailVO> detail(@RequestParam Long id) {
        return R.ok(productBundleService.getDetail(id));
    }

    @PostMapping("/save")
    @OshUserLevel(5)
    public R<BundleDetailVO> save(@RequestBody BundleSaveRequest request) {
        return R.ok(productBundleService.save(request));
    }

    @PostMapping("/status")
    @OshUserLevel(5)
    public R<Void> status(@RequestBody BundleStatusRequest request) {
        productBundleService.updateStatus(request);
        return R.ok();
    }

    @PostMapping("/delete")
    @OshUserLevel(5)
    public R<Void> delete(@RequestBody BundleStatusRequest request) {
        productBundleService.delete(request.getId());
        return R.ok();
    }

    @PostMapping("/resource/search")
    @OshUserLevel(5)
    public R<LinkedHashMap<String, Object>> searchResources(@RequestBody(required = false) BundleResourceSearchQuery query) {
        PageInfo<BundleResourceVO> pageInfo = productBundleService.searchResources(query);
        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("total", pageInfo.getTotal());
        data.put("rows", pageInfo.getList());
        return R.ok(data);
    }
}
