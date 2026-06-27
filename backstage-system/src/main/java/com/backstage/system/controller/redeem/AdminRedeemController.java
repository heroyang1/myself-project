package com.backstage.system.controller.redeem;

import com.backstage.common.annotation.OshUserLevel;
import com.backstage.common.core.domain.R;
import com.backstage.system.domain.redeem.OshRedeemChannel;
import com.backstage.system.domain.redeem.OshRedeemCode;
import com.backstage.system.domain.redeem.dto.RedeemChannelCreateRequest;
import com.backstage.system.domain.redeem.dto.RedeemCodeCreateRequest;
import com.backstage.system.domain.redeem.dto.RedeemCodePageQuery;
import com.backstage.system.domain.redeem.dto.RedeemResourceSearchQuery;
import com.backstage.system.domain.redeem.vo.RedeemCodeVO;
import com.backstage.system.domain.redeem.vo.RedeemCodeResultVO;
import com.backstage.system.domain.redeem.vo.RedeemResourceVO;
import com.backstage.system.service.redeem.RedeemService;
import com.github.pagehelper.PageInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;

@RestController
@RequestMapping("/pc/admin/redeem")
public class AdminRedeemController {
    @Resource
    private RedeemService redeemService;

    @GetMapping("/channel/list")
    @OshUserLevel(5)
    public R<List<OshRedeemChannel>> listChannels() {
        return R.ok(redeemService.listChannels());
    }

    @PostMapping("/channel/create")
    @OshUserLevel(5)
    public R<OshRedeemChannel> createChannel(@RequestBody RedeemChannelCreateRequest request) {
        return R.ok(redeemService.createChannel(request));
    }

    @GetMapping("/resource/resolve")
    @OshUserLevel(5)
    public R<RedeemResourceVO> resolveResource(@RequestParam String resourceNo) {
        return R.ok(redeemService.resolveResource(resourceNo));
    }

    @PostMapping("/resource/search")
    @OshUserLevel(5)
    public R<LinkedHashMap<String, Object>> searchResources(@RequestBody(required = false) RedeemResourceSearchQuery query) {
        PageInfo<RedeemResourceVO> pageInfo = redeemService.searchResources(query);
        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("total", pageInfo.getTotal());
        data.put("rows", pageInfo.getList());
        return R.ok(data);
    }

    @PostMapping("/code/create")
    @OshUserLevel(5)
    public R<RedeemCodeResultVO> createCode(@RequestBody RedeemCodeCreateRequest request) {
        OshRedeemCode code = redeemService.createCode(request);
        return R.ok(new RedeemCodeResultVO(code.getCode(), code.getOrderNo(), code.getStatus()));
    }

    @PostMapping("/code/page")
    @OshUserLevel(5)
    public R<LinkedHashMap<String, Object>> pageCodes(@RequestBody(required = false) RedeemCodePageQuery query) {
        PageInfo<RedeemCodeVO> pageInfo = redeemService.pageAdminCodes(query);
        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("total", pageInfo.getTotal());
        data.put("rows", pageInfo.getList());
        return R.ok(data);
    }

    @PostMapping("/code/cancel")
    @OshUserLevel(5)
    public R<Void> cancelCode(@RequestParam Long id) {
        redeemService.cancelCode(id);
        return R.ok();
    }
}
