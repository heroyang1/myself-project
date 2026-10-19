package com.backstage.system.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class FlexibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter MILLIS_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Override
    public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.currentToken() == JsonToken.VALUE_NULL) {
            return null;
        }

        if (parser.currentToken().isNumeric()) {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(parser.getLongValue()), ZoneId.systemDefault());
        }

        String text = parser.getValueAsString();
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        return parse(text.trim());
    }

    private LocalDateTime parse(String text) throws IOException {
        // 多格式逐一尝试解析，解析失败属预期行为（继续尝试下一种格式），
        // 全部失败后在最后一个分支抛出 IOException。
        // 纯数字：毫秒时间戳（ES 的 date 字段在 convertValue 时会被序列化为字符串）
        if (text.matches("\\d+")) {
            try {
                return LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(text)), ZoneId.systemDefault());
            } catch (NumberFormatException ignored) {
                // 数字超出毫秒时间戳范围，继续尝试字符串格式
            }
        }

        try {
            return OffsetDateTime.parse(text).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            // 非 ISO 偏移格式，继续尝试下一种
        }

        try {
            return LocalDateTime.parse(text);
        } catch (DateTimeParseException ignored) {
            // 非 ISO 本地时间格式，继续尝试下一种
        }

        try {
            return LocalDateTime.parse(text, DEFAULT_FORMATTER);
        } catch (DateTimeParseException ignored) {
            // 非 yyyy-MM-dd HH:mm:ss 格式，继续尝试下一种
        }

        try {
            return LocalDateTime.parse(text, MILLIS_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new IOException("Cannot parse LocalDateTime value: " + text, ex);
        }
    }
}
