package com.backstage.system.controller.redeem;

import com.backstage.common.annotation.Anonymous;
import com.backstage.common.annotation.OshUserLevel;
import com.backstage.common.core.domain.R;
import com.backstage.common.exception.ServiceException;
import com.backstage.system.domain.redeem.OshRedeemCode;
import com.backstage.system.domain.redeem.dto.RedeemCodePageQuery;
import com.backstage.system.domain.redeem.dto.RedeemConsumeRequest;
import com.backstage.system.domain.redeem.vo.RedeemCodeVO;
import com.backstage.system.domain.redeem.vo.RedeemCodeResultVO;
import com.backstage.system.domain.redeem.vo.RedeemInviteLinkVO;
import com.backstage.system.service.redeem.RedeemService;
import com.backstage.system.utils.UserContextUtil;
import com.github.pagehelper.PageInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import java.util.LinkedHashMap;

@RestController
@RequestMapping("/pc/user/redeem")
public class UserRedeemController {
    @Resource
    private RedeemService redeemService;

    @GetMapping("/invite-link")
    @Anonymous
    public R<RedeemInviteLinkVO> getInviteLink(@RequestParam String token) {
        return R.ok(redeemService.getInviteLink(token, false));
    }

    @GetMapping("/invite-link/resolve")
    @OshUserLevel(1)
    public R<RedeemInviteLinkVO> resolveInviteLink(@RequestParam String token) {
        return R.ok(redeemService.getInviteLink(token, true));
    }

    @PostMapping("/page")
    @OshUserLevel(1)
    public R<LinkedHashMap<String, Object>> pageCodes(@RequestBody(required = false) RedeemCodePageQuery query) {
        PageInfo<RedeemCodeVO> pageInfo = redeemService.pageUserCodes(currentUserId(), query);
        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("total", pageInfo.getTotal());
        data.put("rows", pageInfo.getList());
        return R.ok(data);
    }

    @PostMapping("/consume")
    @OshUserLevel(1)
    public R<RedeemCodeResultVO> consume(@RequestBody RedeemConsumeRequest request) {
        if (request == null) {
            throw new ServiceException("请选择要核销的核销码");
        }
        Long userId = currentUserId();
        OshRedeemCode code = request.getId() == null
                ? redeemService.redeemByCode(userId, request.getCode())
                : redeemService.redeem(userId, request.getId());
        return R.ok(new RedeemCodeResultVO(code.getCode(), code.getOrderNo(), code.getStatus()));
    }

    private Long currentUserId() {
        Long userId = UserContextUtil.getCurrentUserIdSafely();
        if (userId == null) {
            throw new ServiceException("请先登录");
        }
        return userId;
    }
}
