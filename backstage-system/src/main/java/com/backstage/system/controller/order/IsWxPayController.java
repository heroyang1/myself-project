package com.backstage.system.controller.order;

import com.backstage.common.annotation.Anonymous;
import com.backstage.common.core.domain.R;
import com.backstage.system.domain.order.OshOrderSave;
import com.backstage.system.service.order.IOshOrderSaveService;
import com.backstage.system.service.order.PayService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/pc/order/iswxpay")
public class IsWxPayController {

    @Autowired
    private IOshOrderSaveService orderSaveService;

    @Autowired
    private PayService payService;

    @Anonymous
    @PostMapping
    public R isWxPay(@RequestBody Map<String, String> params){
        String orderNo = params == null ? null : params.get("no");
        R r = new R();
        r.setCode(200);
        r.setMsg("ok");

        Map map = new HashMap();
        map.put("trade_state", "NOTPAY");

        if (StringUtils.isBlank(orderNo)) {
            r.setData(map);
            return r;
        }

        OshOrderSave order = orderSaveService.selectOshOrderSaveByNo(orderNo);
        if (order == null) {
            r.setData(map);
            return r;
        }

        if ("success".equals(order.getStatus())) {
            map.put("trade_state", "SUCCESS");
            r.setData(map);
            return r;
        }

        Map<String, String> status = payService.queryPay(
                WxPayController.legacyPaymentNo(orderNo),
                payService.currentProvider(),
                order.getPrice() == null ? BigDecimal.ZERO : order.getPrice()
        );
        if (status != null && "1".equals(status.get("status"))) {
            OshOrderSave update = new OshOrderSave();
            update.setNo(orderNo);
            update.setStatus("success");
            orderSaveService.updateOshOrderSave(update);
            map.put("trade_state", "SUCCESS");
        }

        r.setData(map);

        return r;
    }
}
