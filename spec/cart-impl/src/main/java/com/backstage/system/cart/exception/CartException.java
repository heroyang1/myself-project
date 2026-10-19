package com.backstage.system.cart.exception;

/**
 * 购物车业务异常。携带可判定的错误码，供全局异常处理器映射为响应 code。
 */
public class CartException extends RuntimeException {

    /** 错误码（本沙盒自定；生产接入须并入 docs/reference/error-codes.md 统一段位）。 */
    public enum Code {
        INVALID_QUANTITY(40001, "加购数量非法"),
        UNAUTHORIZED(40100, "未登录，禁止加购"),
        IDEMPOTENCY_TOKEN_REQUIRED(40002, "缺少幂等 token"),
        SKU_NOT_FOUND(40401, "商品不存在"),
        SKU_OFF_SALE(40003, "商品已下架"),
        OUT_OF_STOCK(40004, "库存不足");

        private final int code;
        private final String message;

        Code(int code, String message) {
            this.code = code;
            this.message = message;
        }

        public int getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }

    private final Code code;

    public CartException(Code code) {
        super(code.getMessage());
        this.code = code;
    }

    public Code getCode() {
        return code;
    }
}
