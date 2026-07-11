package com.backstage.system.payment.huifu.client;

/** 包内 HTTP 抽象，测试时可替换；生产实现不得记录请求/响应原文。 */
interface HuifuHttpTransport {
    String postJson(String url,
                    String requestJson,
                    int connectTimeoutMs,
                    int readTimeoutMs,
                    String proxyToken);
}
