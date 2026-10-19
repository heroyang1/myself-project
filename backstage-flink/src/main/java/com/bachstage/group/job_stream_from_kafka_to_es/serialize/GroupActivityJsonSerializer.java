package com.bachstage.group.job_stream_from_kafka_to_es.serialize;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

/**
 * 拼团活动索引 JSON 序列化工具
 * 将时间字段从字符串格式转换为 epoch_millis，与 ES mapping 的 date 格式对齐
 *
 * @author system
 * @date 2026-07-12
 */
public class GroupActivityJsonSerializer {

    private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .optionalStart()
            .appendFraction(ChronoField.MILLI_OF_SECOND, 1, 3, true)
            .optionalEnd()
            .toFormatter();

    private static final String[] DATE_FIELDS = {"startTime", "serverStartTime", "serverEndTime", "createTime", "updateTime"};

    public static String buildJsonWithTimestamp(JSONObject message) {
        JSONObject json = new JSONObject(message);
        for (String field : DATE_FIELDS) {
            normalizeDateTimeField(json, field);
        }
        return JSON.toJSONString(json);
    }

    private static void normalizeDateTimeField(JSONObject json, String fieldName) {
        Object value = json.get(fieldName);
        if (!(value instanceof String)) {
            return;
        }
        String text = ((String) value).trim();
        if (text.isEmpty()) {
            json.remove(fieldName);
            return;
        }
        try {
            LocalDateTime ldt = LocalDateTime.parse(text, FORMATTER);
            long epochMillis = ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            json.put(fieldName, epochMillis);
        } catch (Exception e) {
            // 解析失败时保留原始值
        }
    }
}
