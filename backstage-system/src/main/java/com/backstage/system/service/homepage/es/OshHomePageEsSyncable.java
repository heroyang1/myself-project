package com.backstage.system.service.homepage.es;

/**
 * 首页卡片 ES 同步能力标记接口。
 * 任何首页卡片 Service 实现该接口后，会被同步编排器自动发现并参与统一同步。
 *
 * @author jayTatum
 */
public interface OshHomePageEsSyncable {

    /**
     * 卡片唯一标识，用于日志与排查（如 "course"、"feedback"）。
     */
    String cardType();

    /**
     * 全量同步当前卡片到 ES，返回写入文档数量。
     */
    int syncToEs();

    /**
     * 启动时索引检查与首同步。默认空实现，需要的子类覆盖。
     */
    default void ensureIndexReady() {
        // no-op
    }
}
