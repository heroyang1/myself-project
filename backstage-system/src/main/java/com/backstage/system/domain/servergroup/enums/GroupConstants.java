package com.backstage.system.domain.servergroup.enums;

/**
 * 拼团模块常量定义
 * 
 * 消除代码中的魔法数字和字符串
 * 
 * @author system
 * @date 2026-07-09
 */
public final class GroupConstants {

    private GroupConstants() {}

    // ==================== 活动类型 ====================
    /** 服务器拼团活动类型 */
    public static final String ACTIVITY_TYPE_SERVER = "server";

    // ==================== 订单号前缀 ====================
    /** 拼团订单号前缀（统一为 GRP） */
    public static final String ORDER_NO_PREFIX = "GRP";

    // ==================== SSH 默认配置 ====================
    /** SSH 默认端口 */
    public static final int DEFAULT_SSH_PORT = 22;

    /** SSH 默认用户名 */
    public static final String DEFAULT_SSH_USER = "root";

    // ==================== 业务配置 ====================
    /** 招募过期天数（发起拼团后的招募期限） */
    public static final int RECRUIT_EXPIRE_DAYS = 7;

    /** 支付超时分钟数 */
    public static final int PAY_TIMEOUT_MINUTES = 30;

    /** 过期活动清理天数 */
    public static final int EXPIRED_ACTIVITY_CLEANUP_DAYS = 30;

    /** 服务器到期提醒天数 */
    public static final int SERVER_EXPIRE_NOTIFY_DAYS = 7;

    /** 最低拼团人数下限 */
    public static final int MIN_GROUP_SIZE = 2;

    /** 使用时长上限（月） */
    public static final int MAX_DURATION_MONTHS = 120;

    // ==================== 支付方式 ====================
    /** 支付宝支付方式 */
    public static final String PAY_METHOD_ALIPAY = "alipay";

    /** 默认支付方式：支付宝（统一归一化后） */
    public static final String DEFAULT_PAY_METHOD = PAY_METHOD_ALIPAY;

    // ==================== 订单来源类型 ====================
    /** 系统活动模板拼团 */
    public static final String SOURCE_TYPE_SYSTEM = "system";

    /** 用户发起拼团 */
    public static final String SOURCE_TYPE_INITIATED = "initiated";

    // ==================== 缓存 Key 前缀 ====================
    /** 活动列表缓存前缀 */
    public static final String CACHE_ACTIVITY_LIST_PREFIX = "group:activity:list:";

    /** 活动详情缓存前缀 */
    public static final String CACHE_ACTIVITY_DETAIL_PREFIX = "group:activity:detail:";

    /** 活动列表缓存 TTL（分钟） */
    public static final long CACHE_ACTIVITY_LIST_TTL = 5;

    /** 活动详情缓存 TTL（分钟） */
    public static final long CACHE_ACTIVITY_DETAIL_TTL = 10;

    // ==================== 分享 URL ====================
    /** 分享链接前缀（可通过配置注入覆盖） */
    public static final String SHARE_URL_PREFIX = "https://osh.net.cn/group/detail";

    // ==================== Redis Key 前缀（预留） ====================
    // 注意：当前拼团并发安全通过 SELECT FOR UPDATE + CAS 原子操作保证，
    // 不使用 Redis 分布式锁。以下常量保留供未来扩展使用。
    // 如需启用 Redis 锁，请配合 AOP 切面统一实现。

    // ==================== 日志模块标识 ====================
    /** 拼团模块日志标识 */
    public static final String LOG_GROUP = "[GROUP]";

    /** 参团日志标识 */
    public static final String LOG_JOIN = LOG_GROUP + "[JOIN]";

    /** 发起拼团日志标识 */
    public static final String LOG_CREATE = LOG_GROUP + "[CREATE]";

    /** 支付回调日志标识 */
    public static final String LOG_PAY_CALLBACK = LOG_GROUP + "[PAY_CALLBACK]";

    /** 支付请求日志标识 */
    public static final String LOG_PAY_REQUEST = LOG_GROUP + "[PAY_REQUEST]";

    /** 定时任务日志标识 */
    public static final String LOG_SCHEDULE = LOG_GROUP + "[SCHEDULE]";

    /** 手动添加日志标识 */
    public static final String LOG_ADD_USER = LOG_GROUP + "[ADD_USER]";

    /** SSH 查询日志标识 */
    public static final String LOG_SSH = LOG_GROUP + "[SSH]";

    /** 缓存日志标识 */
    public static final String LOG_CACHE = LOG_GROUP + "[CACHE]";

    /** 安全审计日志标识 */
    public static final String LOG_SECURITY = LOG_GROUP + "[SECURITY]";
}
