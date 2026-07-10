package com.backstage.system.service.impl.course;

import java.math.BigDecimal;

import com.backstage.common.exception.ServiceException;
import com.backstage.system.domain.course.OshCourse;
import com.backstage.system.domain.order.OrderStatusResult;
import com.backstage.system.domain.order.enums.ProductTypeEnum;
import com.backstage.system.domain.vo.pay.OrderCheckoutRespVO;
import com.backstage.system.enums.CourseResourceEnum;
import com.backstage.system.mapper.course.CourseBuyMapper;
import com.backstage.system.mapper.course.OshCourseMapper;
import com.backstage.system.service.course.ICoursePayService;
import com.backstage.system.service.order.OrderService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * Course payment service backed by the unified order checkout flow.
 */
@Service
public class CoursePayServiceImpl implements ICoursePayService {

    private static final String PAY_TYPE_ALIPAY = "alipay";

    @Resource
    private OshCourseMapper oshCourseMapper;

    @Resource
    private CourseBuyMapper courseBuyMapper;

    @Resource
    private CourseBundlePayCheckoutService courseBundlePayCheckoutService;

    @Resource
    private OrderService orderService;

    @Override
    public OrderCheckoutRespVO createCoursePay(Long courseId, String payType, String clientIp, Long userId) {
        if (courseId == null || courseId <= 0) {
            throw new ServiceException("course_id 不能为空");
        }
        if (userId == null || userId <= 0) {
            throw new ServiceException("请先登录后再支付");
        }

        String channel = normalizePayType(payType);
        if (courseBuyMapper.countPaidByUserAndCourse(userId, courseId) > 0) {
            throw new ServiceException("该课程已购买，请勿重复支付");
        }

        OshCourse course = oshCourseMapper.selectCourseById(courseId);
        if (course == null) {
            throw new ServiceException("课程不存在: " + courseId);
        }
        ensureVipCoursePayable(course);
        BigDecimal price = course.getPrice();
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("课程价格无效");
        }

        OrderCheckoutRespVO checkoutResp = courseBundlePayCheckoutService.checkout(
                userId,
                ProductTypeEnum.COURSE,
                course.getId(),
                course.getTitle() == null ? ("课程#" + course.getId()) : course.getTitle(),
                course.getPrice(),
                course.getPrice(),
                channel,
                normalizeClientIp(clientIp)
        );
        courseBuyMapper.upsertPendingOrder(userId, courseId, checkoutResp.getOrderNo(), channel, course.getPrice(), course.getPrice());
        return checkoutResp;
    }

    @Override
    public boolean isCoursePaid(String orderNo, Long userId) {
        if (orderNo == null || orderNo.isEmpty() || userId == null || userId <= 0) {
            return false;
        }
        if (courseBuyMapper.countPaidByOrderNoAndUserId(orderNo, userId) > 0) {
            return true;
        }
        try {
            OrderStatusResult status = orderService.getOrderStatusForUser(orderNo, userId);
            return status != null && status.isPayStatus();
        } catch (ServiceException e) {
            return false;
        }
    }

    private String normalizePayType(String payType) {
        if (payType == null || payType.trim().isEmpty()) {
            return PAY_TYPE_ALIPAY;
        }
        String lower = payType.trim().toLowerCase();
        if (PAY_TYPE_ALIPAY.equals(lower)
                || "wxpay".equals(lower)
                || "wechat".equals(lower)
                || "union".equals(lower)
                || "unionpay".equals(lower)) {
            return PAY_TYPE_ALIPAY;
        }
        throw new ServiceException("不支持的支付方式: " + payType);
    }

    private String normalizeClientIp(String clientIp) {
        if (clientIp == null || clientIp.trim().isEmpty() || "0:0:0:0:0:0:0:1".equals(clientIp.trim())) {
            return "127.0.0.1";
        }
        String ip = clientIp.trim();
        int commaIndex = ip.indexOf(',');
        if (commaIndex >= 0) {
            ip = ip.substring(0, commaIndex).trim();
        }
        return ip.isEmpty() ? "127.0.0.1" : ip;
    }

    private void ensureVipCoursePayable(OshCourse course) {
        CourseResourceEnum resourceEnum = CourseResourceEnum.fromCode(course.getResourceType());
        if (resourceEnum == CourseResourceEnum.VIP
                || resourceEnum == CourseResourceEnum.CASH_ONLY
                || resourceEnum == CourseResourceEnum.CASH_POINT) {
            return;
        }
        if (resourceEnum == null && Integer.valueOf(2).equals(course.getLevel())) {
            return;
        }
        throw new ServiceException("仅 VIP 专属课程支持单独购买，小班专属/内部专属课程请使用对应角色权限访问");
    }
}
