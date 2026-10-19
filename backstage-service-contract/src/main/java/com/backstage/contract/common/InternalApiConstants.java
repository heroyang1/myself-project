package com.backstage.contract.common;

/**
 * 内部服务调用契约常量
 *
 * <p>集中定义 /internal 端点路径、服务间透传头部与内部鉴权标识，作为服务端（单体控制器）
 * 与客户端（未来各业务服务 Feign）共同遵守的单一事实来源。所有值均为编译期常量，
 * 可直接用于 {@code @RequestMapping} 与 {@code @FeignClient} 注解。</p>
 *
 * <p>安全约定：/internal/** 仅供服务间内网调用，禁止经网关对外暴露；
 * 调用方必须携带 {@link #HEADER_INTERNAL_TOKEN}，服务端校验通过后才授予 {@link #AUTHORITY_INTERNAL_SERVICE}。</p>
 *
 * @author osh-backend
 * @date 2026-09-30
 */
public final class InternalApiConstants {

    private InternalApiConstants() {
    }

    /** 内部接口统一前缀 */
    public static final String BASE_PATH = "/internal";

    // ==================== 服务间透传头部 ====================

    /** 内部调用共享密钥头部（服务间鉴权，值由配置中心下发） */
    public static final String HEADER_INTERNAL_TOKEN = "X-Internal-Token";

    /** 全链路追踪 ID 头部 */
    public static final String HEADER_TRACE_ID = "X-Trace-Id";

    /** 发起调用的真实用户 ID 头部（内部接口不走用户 JWT，用户身份由此透传） */
    public static final String HEADER_USER_ID = "X-User-Id";

    // ==================== 内部鉴权标识 ====================

    /** 内部服务调用被授予的权限标识（SecurityConfig 用 hasAuthority 校验） */
    public static final String AUTHORITY_INTERNAL_SERVICE = "INTERNAL_SERVICE";

    // ==================== 资产（auth-user 域）====================

    /** 资产内部接口前缀 */
    public static final String ASSET_BASE = BASE_PATH + "/auth-user/asset";

    /** 扣减积分 */
    public static final String ASSET_DEDUCT = ASSET_BASE + "/deduct";

    /** 退还积分（SAGA 补偿） */
    public static final String ASSET_REFUND = ASSET_BASE + "/refund";

    /** 查询积分余额 */
    public static final String ASSET_BALANCE = ASSET_BASE + "/balance";

    // ==================== 交易（trade 域）====================

    /** 交易内部接口前缀 */
    public static final String TRADE_BASE = BASE_PATH + "/trade/order";

    /** 订单结算（创建订单并发起支付） */
    public static final String TRADE_CHECKOUT = TRADE_BASE + "/checkout";

    /** 按订单号关单 */
    public static final String TRADE_CANCEL = TRADE_BASE + "/cancel";

    // ==================== 平台（platform 域）====================

    /** 平台内部接口前缀 */
    public static final String OSS_BASE = BASE_PATH + "/platform/oss";

    /** 对象存储临时签名 URL */
    public static final String OSS_SIGN = OSS_BASE + "/sign";

    // ==================== 课程（course 域）====================

    /** 课程内部接口前缀 */
    public static final String QA_BASE = BASE_PATH + "/course/qa";

    /** 工具提问归属 course 域，由其暴露内部端点 */
    public static final String QA_TOOL_QUESTION = QA_BASE + "/tool-question";
}
