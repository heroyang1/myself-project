package com.backstage.system.payment.huifu.exception;

public class HuifuConfigurationException extends HuifuPayException {
    public HuifuConfigurationException(String message) {
        super(message);
    }

    public HuifuConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
