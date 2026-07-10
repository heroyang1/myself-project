package com.backstage.system.payment.huifu.client;

import com.backstage.system.payment.huifu.exception.HuifuTransportException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** 不输出请求体、签名或响应原文的 JDK HTTP 实现。 */
final class JdkHuifuHttpTransport implements HuifuHttpTransport {

    private static final int MAX_RESPONSE_BYTES = 1024 * 1024;

    @Override
    public String postJson(String url,
                           String requestJson,
                           int connectTimeoutMs,
                           int readTimeoutMs,
                           String proxyToken) {
        HttpURLConnection connection = null;
        try {
            byte[] requestBytes = requestJson.getBytes(StandardCharsets.UTF_8);
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(connectTimeoutMs);
            connection.setReadTimeout(readTimeoutMs);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json");
            if (!isBlank(proxyToken)) {
                connection.setRequestProperty("X-OSH-Huifu-Proxy-Token", proxyToken.trim());
            }
            connection.setFixedLengthStreamingMode(requestBytes.length);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(requestBytes);
            }

            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new HuifuTransportException("汇付支付网关返回异常 HTTP 状态: " + status, null);
            }
            try (InputStream input = connection.getInputStream()) {
                return readLimited(input);
            }
        } catch (HuifuTransportException e) {
            throw e;
        } catch (IOException e) {
            throw new HuifuTransportException("请求汇付支付网关失败", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String readLimited(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            total += read;
            if (total > MAX_RESPONSE_BYTES) {
                throw new HuifuTransportException("汇付支付网关响应体过大", null);
            }
            output.write(buffer, 0, read);
        }
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
