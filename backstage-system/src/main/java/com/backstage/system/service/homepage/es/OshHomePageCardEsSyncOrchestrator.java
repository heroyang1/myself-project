package com.backstage.system.service.homepage.es;

import com.backstage.system.config.properties.SearchEsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 首页卡片 ES 同步编排器。
 * Spring 自动注入所有 {@link OshHomePageEsSyncable} 实现，由 xxl-job handler 统一调用。
 *
 * @author jayTatum
 */
@Component
public class OshHomePageCardEsSyncOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(OshHomePageCardEsSyncOrchestrator.class);

    @Autowired(required = false)
    private List<OshHomePageEsSyncable> syncables = Collections.emptyList();

    @Autowired
    private SearchEsProperties searchEsProperties;

    /**
     * 同步所有卡片。单卡失败不中断 batch；返回每个 cardType 的写入数，失败为 -1。
     */
    public Map<String, Integer> syncAll() {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (!searchEsProperties.isEnabled()) {
            log.warn("[homepage-sync] es disabled, skip all");
            return result;
        }
        for (OshHomePageEsSyncable service : syncables) {
            String card = service.cardType();
            try {
                int written = service.syncToEs();
                result.put(card, written);
                log.info("[homepage-sync] {} done, written={}", card, written);
            } catch (Exception e) {
                result.put(card, -1);
                log.error("[homepage-sync] {} failed, error={}", card, e.getMessage(), e);
            }
        }
        return result;
    }

    /**
     * 按 cardType 同步单个卡片，找不到时抛 {@link IllegalArgumentException}。
     */
    public int syncOne(String cardType) {
        if (!searchEsProperties.isEnabled()) {
            log.warn("[homepage-sync] es disabled, skip {}", cardType);
            return 0;
        }
        for (OshHomePageEsSyncable service : syncables) {
            if (service.cardType().equalsIgnoreCase(cardType)) {
                return service.syncToEs();
            }
        }
        throw new IllegalArgumentException("unknown homepage cardType: " + cardType);
    }
}
