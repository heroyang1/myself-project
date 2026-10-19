package com.backstage.system.payment.huifu.exception;

/** 网络/HTTP 层异常，不携带密钥、签名或完整报文。 */
public class HuifuTransportException extends HuifuPayException {
    public HuifuTransportException(String message, Throwable cause) {
        super(message, cause);
    }
}
