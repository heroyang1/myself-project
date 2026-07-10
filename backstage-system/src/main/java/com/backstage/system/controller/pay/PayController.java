package com.backstage.system.controller.pay;


import com.backstage.common.annotation.OshUserLevel;
import com.backstage.common.core.domain.R;
import com.backstage.system.domain.order.OrderStatusResult;
import com.backstage.system.service.order.OrderService;
import com.backstage.system.utils.UserContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pc/pay")
public class PayController {

    @Autowired
    private OrderService orderService;

    // 不提供通用公网下单入口。课程、套餐、电子书、会员、工具等必须走各自的
    // 服务端定价接口，用户、商品和金额都不能由浏览器直接提交。

    /**
     * 查询订单支付状态（前端轮询）。
     *
     * @param orderNo 订单编号
     * @return 订单支付状态mmi
     */
    @GetMapping("/status")
//    @PreAuthorize("hasAuthority('pay:status')")
    @OshUserLevel(value = 1)
    public R<OrderStatusResult> status(@RequestParam String orderNo) {
        return R.ok(orderService.getOrderStatusForUser(orderNo, UserContextUtil.getCurrentUserIdSafely()));
    }

    /**
     * 取消支付（用户主动取消）。
     *
     * @param orderNo 订单编号
     * @return 取消支付结果
     */

    @PostMapping("/cancel")
    public R<Void> cancel(@RequestParam String orderNo) {
        orderService.cancelPaymentByOrderNoForUser(orderNo, UserContextUtil.getCurrentUserIdSafely());
        return R.ok();
    }
}
