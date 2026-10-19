package com.backstage.system.controller.bundle;

import com.backstage.common.annotation.Anonymous;
import com.backstage.common.core.domain.R;
import com.backstage.common.exception.ServiceException;
import com.backstage.system.domain.bundle.vo.BundleDetailVO;
import com.backstage.system.domain.vo.pay.OrderCheckoutRespVO;
import com.backstage.system.service.bundle.ProductBundleService;
import com.backstage.system.utils.IpUtils;
import com.backstage.system.utils.UserContextUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/pc/bundle")
public class BundleController {
    @Resource
    private ProductBundleService productBundleService;

    @GetMapping("/recommend")
    public R<List<BundleDetailVO>> recommend(@RequestParam String resourceType, @RequestParam Long resourceId) {
        return R.ok(productBundleService.recommendByResource(resourceType, resourceId));
    }

    @Anonymous
    @PostMapping("/pay/create")
    public R<Map<String, Object>> createPay(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = UserContextUtil.getCurrentUserIdSafely();
        if (userId == null) {
            return R.fail("请先登录后再支付");
        }
        try {
            Long bundleId = parseLong(body.get("bundle_id"), "套餐ID不能为空");
            String payType = body.get("pay_type") == null ? "alipay" : body.get("pay_type").toString();
            OrderCheckoutRespVO checkout = productBundleService.createBundlePay(
                    bundleId, payType, resolveClientIp(request), userId);
            return R.ok(toPayResponse(checkout, bundleId, payType));
        } catch (ServiceException e) {
            return R.fail(e.getMessage());
        } catch (Exception e) {
            return R.fail("创建套餐支付订单失败: " + e.getMessage());
        }
    }

    @Anonymous
    @GetMapping("/pay/status")
    public R<Map<String, Object>> payStatus(@RequestParam("out_trade_no") String orderNo) {
        Long userId = UserContextUtil.getCurrentUserIdSafely();
        if (userId == null) {
            return R.fail("请先登录后再查询支付状态");
        }
        try {
            return R.ok(productBundleService.getBundlePayStatus(orderNo, userId));
        } catch (ServiceException e) {
            return R.fail(e.getMessage());
        }
    }

    private Map<String, Object> toPayResponse(OrderCheckoutRespVO checkout, Long bundleId, String payType) {
        Map<String, Object> data = new LinkedHashMap<>();
        String qrcode = checkout.getPayment() == null ? null : firstNonBlank(
                checkout.getPayment().getQrcode(),
                checkout.getPayment().getPayUrl());
        String payUrl = checkout.getPayment() == null ? null : checkout.getPayment().getPayUrl();
        data.put("needPay", checkout.isNeedPay());
        data.put("qrcode", qrcode);
        data.put("payurl", payUrl);
        data.put("payUrl", payUrl);
        data.put("out_trade_no", checkout.getOrderNo());
        data.put("orderNo", checkout.getOrderNo());
        data.put("paymentNo", checkout.getPaymentNo());
        data.put("pay_type", checkout.getPayment() == null ? payType : checkout.getPayment().getChannel());
        data.put("bundle_id", bundleId);
        data.put("price", checkout.getPrice());
        data.put("pointsUsed", checkout.getPointsUsed());
        data.put("pointsDeductAmount", checkout.getPointsDeductAmount());
        data.put("remainingPoints", checkout.getRemainingPoints());
        data.put("expireTime", checkout.getExpireTime());
        data.put("closeExpireMinutes", checkout.getCloseExpireMinutes());
        return data;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.trim().isEmpty()) {
            return first;
        }
        return second;
    }

    private Long parseLong(Object raw, String message) {
        if (raw == null) {
            throw new ServiceException(message);
        }
        try {
            return Long.parseLong(raw.toString());
        } catch (NumberFormatException e) {
            throw new ServiceException(message);
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String cfIp = request.getHeader("CF-Connecting-IP");
        if (cfIp != null && !cfIp.isEmpty() && !"unknown".equalsIgnoreCase(cfIp.trim())) {
            return cfIp.trim();
        }
        return IpUtils.getClientIp(request);
    }
}
