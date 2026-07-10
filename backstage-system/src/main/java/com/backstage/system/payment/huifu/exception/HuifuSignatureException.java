package com.backstage.system.payment.huifu.exception;

public class HuifuSignatureException extends HuifuPayException {
    public HuifuSignatureException(String message) {
        super(message);
    }

    public HuifuSignatureException(String message, Throwable cause) {
        super(message, cause);
    }
}
