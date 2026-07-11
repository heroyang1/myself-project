package com.backstage.common.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 支付配置
 *
 */
@Component
public class PayConfig {

    /**
     * 当前新订单使用的支付提供方。legacy 保留旧链路，huifu 启用汇付斗拱。
     */
    @Value("${pay.provider:legacy}")
    public String PROVIDER;

    /**
     * 商户ID
     */
    @Value("${pay.merchant-id}")
    public  String PID;

    /**
     * 商户KEY
     */
    @Value("${pay.merchant-key}")
    public  String KEY;

    /**
     * 易支付接口 获取支付url
     */
    @Value("${pay.api-url}")
    public  String API_URL;


    /**
     * 查询订单支付状态的接口
     */
    @Value("${pay.status-url}")
    public  String STATUS_URL;

    /**
     * 支付超时时间（分钟）
     */
    @Value("${pay.expire-minutes:5}")
    public Integer EXPIRE_MINUTES;

    /**
     * 回调地址 需要回调到后端的话，内网无法进来
     */
    @Value("${pay.notify-url:}")
    public String NOTIFY_URL;

    // 支付成功的跳转地址 (感觉没必要，前端直接轮询自己跳转就好了)
    public static final String RETURN_URL = "http://127.0.0.1:5173/return";
}
