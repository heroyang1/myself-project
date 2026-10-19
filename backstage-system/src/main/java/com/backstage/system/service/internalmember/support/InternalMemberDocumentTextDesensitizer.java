package com.backstage.system.service.internalmember.support;

import com.backstage.common.utils.DesensitizedUtil;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class InternalMemberDocumentTextDesensitizer {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("(?i)([A-Z0-9._%+-])[A-Z0-9._%+-]*(@[A-Z0-9.-]+\\.[A-Z]{2,})");
    private static final Pattern PHONE_PATTERN = Pattern.compile("(?<!\\d)(1[3-9]\\d{9})(?!\\d)");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("(?<![0-9Xx])([1-9]\\d{5}(?:18|19|20)?\\d{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx])(?![0-9Xx])");
    private static final Pattern GITHUB_URL_PATTERN = Pattern.compile("https?://(?:www\\.)?github\\.com/\\S+", Pattern.CASE_INSENSITIVE);

    public String desensitize(String text) {
        if (text == null) {
            return null;
        }
        String masked = text;
        masked = replacePhone(masked);
        masked = replaceEmail(masked);
        masked = replaceIdCard(masked);
        masked = replaceGithubUrl(masked);
        masked = replaceLabeledValue(masked, "(?im)^(\\s*(?:姓名|名字|姓\\s*名|Name|Full\\s*Name|Candidate\\s*Name)\\s*[:：]\\s*).*$", "$1[姓名已脱敏]");
        masked = replaceLabeledValue(masked, "(?im)^(\\s*(?:家庭住址|家庭地址|现居住址|居住地址|联系地址)\\s*[:：]\\s*).*$", "$1[地址已脱敏]");
        masked = replaceLabeledValue(masked, "(?im)^(\\s*(?:紧急联系人|紧急联系姓名|Emergency\\s*Contact)\\s*[:：]\\s*).*$", "$1[紧急联系人已脱敏]");
        masked = replaceLabeledValue(masked, "(?im)^(\\s*(?:毕业院校|毕业学校|学校名称|院校|毕业学校名称|School|University|College)\\s*[:：]\\s*).*$", "$1[学校名称已脱敏]");
        masked = replaceLabeledValue(masked, "(?im)^(\\s*(?:公司名称|工作公司|任职公司|就职公司|所在公司|工作单位|单位名称|Employer|Company)\\s*[:：]\\s*).*$", "$1[公司名称已脱敏]");
        masked = replaceLabeledValue(masked, "(?im)^(\\s*(?:GitHub|Github|github|微信|WeChat|wechat|QQ|qq)\\s*[:：]\\s*).*$", "$1[账号已脱敏]");
        return masked;
    }

    private String replacePhone(String text) {
        Matcher matcher = PHONE_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(DesensitizedUtil.maskPhone(matcher.group(1))));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String replaceEmail(String text) {
        Matcher matcher = EMAIL_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String replacement = matcher.group(1) + "***" + matcher.group(2);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String replaceIdCard(String text) {
        Matcher matcher = ID_CARD_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String value = matcher.group(1);
            String replacement = value.length() <= 7
                    ? value
                    : value.substring(0, 3) + repeat('*', value.length() - 7) + value.substring(value.length() - 4);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String replaceGithubUrl(String text) {
        return GITHUB_URL_PATTERN.matcher(text).replaceAll("[账号已脱敏]");
    }

    private String replaceLabeledValue(String text, String pattern, String replacement) {
        return text.replaceAll(pattern, replacement);
    }

    private String repeat(char c, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append(c);
        }
        return builder.toString();
    }
}
