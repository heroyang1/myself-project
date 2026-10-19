package com.backstage.common.utils.aijudge;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.Gson;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 纯净的 AI 通用工具类 (业务无关)
 */
@Component
public class AiUtil {
    private static final Logger log = LoggerFactory.getLogger(AiUtil.class);

    private static final Gson gson = new Gson();

    // 关键改动：调大 ReadTimeout，防止简答题评分时 SocketTimeout
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(100, TimeUnit.SECONDS) // AI 思考慢，给够 100 秒
            .build();

    /**
     * 最通用的调用接口
     */
    public String sendRequest(String apiUrl, String apiKey, String model, String prompt) {
        // 打印发送前的 Prompt 方便调试
        System.out.println("==== [AI 请求开始] ====");
        System.out.println("Model: " + model);
        System.out.println("Prompt 内容: \n" + prompt);

        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);

        JsonArray messages = new JsonArray();
        messages.add(message);

        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.add("messages", messages);
        // 建议增加这个参数，让 AI 生成结果更稳定（打分不需要太随机）
        body.addProperty("temperature", 0.3);

        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(mediaType, gson.toJson(body));

        Request request = new Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String rawBody = (response.body() != null) ? response.body().string() : "";

            // 打印 AI 返回的原始全文（包含 token 消耗等信息）
            System.out.println("==== [AI 原始响应 JSON] ====");
            System.out.println(rawBody);

            if (response.isSuccessful() && !rawBody.isEmpty()) {
                JsonObject jsonResponse = gson.fromJson(rawBody, JsonObject.class);
                String result = jsonResponse.getAsJsonArray("choices")
                        .get(0).getAsJsonObject()
                        .get("message").getAsJsonObject()
                        .get("content").getAsString();

                // 打印 AI 提取后的核心内容
                System.out.println("==== [AI 核心解析内容] ====");
                System.out.println(result);
                return result;
            } else {
                System.err.println("AI 请求失败，状态码: " + response.code());
            }
        } catch (Exception e) {
            log.error("AI 调用链路发生异常, model={}, apiUrl={}", model, apiUrl, e);
        }
        return null;
    }
}