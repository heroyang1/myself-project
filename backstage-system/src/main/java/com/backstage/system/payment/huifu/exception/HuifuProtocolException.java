package com.backstage.system.payment.huifu.exception;

/** 汇付返回报文结构或关键字段不符合协议。 */
public class HuifuProtocolException extends HuifuPayException {
    public HuifuProtocolException(String message) {
        super(message);
    }

    public HuifuProtocolException(String message, Throwable cause) {
        super(message, cause);
    }
}
