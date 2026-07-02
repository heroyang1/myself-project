package com.backstage.system.service.homepage.es;

import com.backstage.system.config.properties.SearchEsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 首页卡片 ES 索引启动初始化器。
 * 对所有 {@link OshHomePageEsSyncable} 实现调用 {@link OshHomePageEsSyncable#ensureIndexReady()}。
 *
 * @author jayTatum
 */
@Component
public class OshHomePageEsIndexInitializer implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(OshHomePageEsIndexInitializer.class);

    @Autowired(required = false)
    private List<OshHomePageEsSyncable> syncables = Collections.emptyList();

    @Autowired
    private SearchEsProperties searchEsProperties;

    @Override
    public void afterPropertiesSet() {
        if (!searchEsProperties.isEnabled()) {
            log.info("[homepage-sync] es disabled, skip index initialization");
            return;
        }
        for (OshHomePageEsSyncable service : syncables) {
            try {
                service.ensureIndexReady();
            } catch (Exception e) {
                log.warn("[homepage-sync] {} init failed, error={}",
                        service.cardType(), e.getMessage(), e);
            }
        }
    }
}
