package com.backstage.system.controller.pay;

import com.backstage.common.annotation.Anonymous;
import com.backstage.system.payment.huifu.exception.HuifuApiException;
import com.backstage.system.payment.huifu.exception.HuifuProtocolException;
import com.backstage.system.payment.huifu.exception.HuifuSignatureException;
import com.backstage.system.payment.huifu.service.HuifuPayNotifyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 汇付服务器异步回调。必须返回纯文本，不能返回统一 JSON 包装。
 */
@RestController
@RequestMapping("/pc/pay/huifu")
public class HuifuPayNotifyController {

    private static final Logger log = LoggerFactory.getLogger(HuifuPayNotifyController.class);
    private static final String FAILURE = "FAIL";

    private final HuifuPayNotifyService notifyService;

    public HuifuPayNotifyController(HuifuPayNotifyService notifyService) {
        this.notifyService = notifyService;
    }

    @Anonymous
    @PostMapping(value = "/notify",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> notify(@RequestParam("resp_code") String responseCode,
                                         @RequestParam(value = "resp_desc", required = false) String responseMessage,
                                         @RequestParam("sign") String signature,
                                         @RequestParam("resp_data") String rawResponseData) {
        try {
            return ResponseEntity.ok(notifyService.handle(
                    responseCode, responseMessage, signature, rawResponseData));
        } catch (HuifuSignatureException | HuifuProtocolException | HuifuApiException e) {
            log.warn("拒绝非法汇付通知, reason={}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(FAILURE);
        } catch (Exception e) {
            // 本地事务/配置异常返回 5xx，让汇付按协议重试。
            log.error("汇付通知本地处理失败, error={}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(FAILURE);
        }
    }
}
