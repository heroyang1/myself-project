package com.backstage.system.controller.course;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import com.backstage.common.annotation.Anonymous;
import com.backstage.common.annotation.OshUserEvent;
import com.backstage.common.constant.ResourceType;
import com.backstage.common.core.domain.R;
import com.backstage.common.exception.ServiceException;
import com.backstage.system.domain.order.OrderPaymentInfo;
import com.backstage.system.domain.vo.pay.OrderCheckoutRespVO;
import com.backstage.system.mapper.course.CourseBuyMapper;
import com.backstage.system.service.course.ICoursePayService;
import com.backstage.system.utils.IpUtils;
import com.backstage.system.utils.UserContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pc/course/pay")
public class CoursePayController {

    @Autowired
    private ICoursePayService coursePayService;

    @Autowired
    private CourseBuyMapper courseBuyMapper;

    @Anonymous
    @PostMapping("/create")
    @OshUserEvent(module = "支付模块", actionType = "支付", resourceType = ResourceType.COURSE_TYPE,
            resourceIdExpression = "#p0['course_id']", description = "创建课程支付订单",
            recordAnonymous = true, successOnly = true)
    public R<Map<String, Object>> create(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = UserContextUtil.getCurrentUserIdSafely();
        if (userId == null) {
            return R.fail("请先登录后再支付");
        }
        Long courseId = parseCourseId(body.get("course_id"));
        String payType = body.get("pay_type") == null ? "wxpay" : body.get("pay_type").toString();
        String clientIp = resolveClientIp(request);

        try {
            OrderCheckoutRespVO checkoutResp = coursePayService.createCoursePay(courseId, payType, clientIp, userId);
            return R.ok(toCreateResponse(checkoutResp, payType, courseId));
        } catch (ServiceException e) {
            return R.fail(e.getMessage());
        } catch (Exception e) {
            return R.fail("创建支付订单失败: " + e.getMessage());
        }
    }

    @Anonymous
    @GetMapping("/status")
    @OshUserEvent(module = "支付模块", actionType = "消费", resourceType = ResourceType.COURSE_TYPE,
            resourceIdExpression = "#result.data['course_id']",
            resourceNameExpression = "#result.data['order_no']",
            recordConditionExpression = "#result.data['consume_recorded'] == true",
            description = "课程支付成功消费", recordAnonymous = true, successOnly = true)
    public R<Map<String, Object>> status(@RequestParam(value = "out_trade_no", required = false) String outTradeNo,
                                         @RequestParam(value = "orderNo", required = false) String orderNo) {
        Long userId = UserContextUtil.getCurrentUserIdSafely();
        if (userId == null) {
            return R.fail("请先登录后再查询支付状态");
        }
        String queryOrderNo = orderNo == null || orderNo.trim().isEmpty() ? outTradeNo : orderNo;
        if (queryOrderNo == null || queryOrderNo.trim().isEmpty()) {
            return R.fail("订单号不能为空");
        }

        Map<String, Object> before = courseBuyMapper.selectByOrderNoAndUserId(queryOrderNo, userId);
        boolean wasPaid = isPaid(before);
        boolean paid = coursePayService.isCoursePaid(queryOrderNo, userId);
        Map<String, Object> latest = courseBuyMapper.selectByOrderNoAndUserId(queryOrderNo, userId);
        boolean changedToPaid = paid && !wasPaid && isPaid(latest);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("paid", paid);
        data.put("consume_recorded", changedToPaid);
        data.put("order_no", queryOrderNo);
        data.put("orderNo", queryOrderNo);
        putIfNotNull(data, "course_id", latest == null ? null : latest.get("courseId"));
        putIfNotNull(data, "pay_price", latest == null ? null : latest.get("payPrice"));
        return R.ok(data);
    }

    private Map<String, Object> toCreateResponse(OrderCheckoutRespVO checkoutResp, String payType, Long courseId) {
        OrderPaymentInfo payment = checkoutResp.getPayment();
        String qrcode = payment == null ? null : firstNonBlank(payment.getQrcode(), payment.getPayUrl());
        String payUrl = payment == null ? null : payment.getPayUrl();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("needPay", checkoutResp.isNeedPay());
        data.put("qrcode", qrcode);
        data.put("payurl", payUrl);
        data.put("payUrl", payUrl);
        data.put("out_trade_no", checkoutResp.getOrderNo());
        data.put("orderNo", checkoutResp.getOrderNo());
        data.put("paymentNo", checkoutResp.getPaymentNo());
        data.put("pay_type", payment == null ? payType : payment.getChannel());
        data.put("course_id", courseId);
        data.put("price", checkoutResp.getPrice());
        data.put("pointsUsed", checkoutResp.getPointsUsed());
        data.put("pointsDeductAmount", checkoutResp.getPointsDeductAmount());
        data.put("remainingPoints", checkoutResp.getRemainingPoints());
        data.put("expireTime", checkoutResp.getExpireTime());
        data.put("closeExpireMinutes", checkoutResp.getCloseExpireMinutes());
        return data;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.trim().isEmpty()) {
            return first;
        }
        return second;
    }

    private boolean isPaid(Map<String, Object> courseBuy) {
        return courseBuy != null && "paid".equals(String.valueOf(courseBuy.get("payStatus")));
    }

    private void putIfNotNull(Map<String, Object> data, String key, Object value) {
        if (value != null) {
            data.put(key, value);
        }
    }

    private Long parseCourseId(Object raw) {
        if (raw == null) {
            throw new ServiceException("course_id 不能为空");
        }
        try {
            return Long.parseLong(raw.toString());
        } catch (NumberFormatException e) {
            throw new ServiceException("course_id 必须是整数");
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
