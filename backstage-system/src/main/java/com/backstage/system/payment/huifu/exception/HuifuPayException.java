package com.backstage.system.payment.huifu.exception;

/** 汇付客户端异常基类。 */
public class HuifuPayException extends RuntimeException {

    public HuifuPayException(String message) {
        super(message);
    }

    public HuifuPayException(String message, Throwable cause) {
        super(message, cause);
    }
}
