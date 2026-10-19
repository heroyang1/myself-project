package com.backstage.system.service.homepage.es;

import com.backstage.system.config.properties.SearchEsProperties;
import com.backstage.system.mapper.homepage.AbstractOshHomePageAliasEsMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

/**
 * 首页卡片“ES 优先 + MySQL 兜底”模板基类。
 *
 * @param <D> ES 文档类型
 * @param <V> 卡片 VO 类型
 */
public abstract class AbstractOshHomePageEsCardService<D, V> implements OshHomePageEsSyncable {

    protected static final Logger log = LoggerFactory.getLogger(AbstractOshHomePageEsCardService.class);
    private static final DateTimeFormatter BATCH_ID_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    @Autowired
    protected SearchEsProperties searchEsProperties;

    public final List<V> getCards(int requestedLimit) {
        int effectiveLimit = normalizeLimit(requestedLimit);
        if (effectiveLimit <= 0) {
            return Collections.emptyList();
        }
        if (searchEsProperties.isEnabled()) {
            try {
                List<D> docs = queryFromEs(effectiveLimit);
                if (docs != null && !docs.isEmpty()) {
                    return toVos(docs);
                }
                log.warn("[homepage-{}] es empty, fallback mysql, limit={}", cardType(), effectiveLimit);
            } catch (Exception e) {
                log.warn("[homepage-{}] es failed, fallback mysql, limit={}, error={}",
                        cardType(), effectiveLimit, e.getMessage(), e);
            }
        }
        return queryFromMysql(effectiveLimit);
    }

    @Override
    public final int syncToEs() {
        String batchId = buildSyncBatchId();
        long start = System.currentTimeMillis();
        String currentIndex = null;
        String targetIndex = null;
        try {
            AbstractOshHomePageAliasEsMapper<D> esMapper = esMapper();
            currentIndex = esMapper.resolveCurrentAliasIndex();
            targetIndex = esMapper.resolveTargetIndex(currentIndex);
            log.info("[homepage-{}-sync] start, batchId={}, alias={}, currentIndex={}, targetIndex={}",
                    cardType(),
                    batchId,
                    esMapper.readAlias(),
                    currentIndex == null ? "none" : currentIndex,
                    targetIndex);

            List<V> vos = queryFromMysqlForSync(syncLimit());
            LocalDateTime syncTime = LocalDateTime.now();
            List<D> docs = toDocs(vos, syncTime);
            fillSyncBatchIdIfSupported(docs, batchId);

            log.info("[homepage-{}-sync] mysql loaded, batchId={}, count={}",
                    cardType(), batchId, docs.size());

            String mappingJson = esMapper.loadIndexMappingJson();
            esMapper.createIndexIfAbsent(targetIndex, mappingJson);
            esMapper.deleteAll(targetIndex);
            log.info("[homepage-{}-sync] target index cleared, batchId={}, targetIndex={}",
                    cardType(), batchId, targetIndex);

            int written = esMapper.bulkUpsert(targetIndex, docs);
            log.info("[homepage-{}-sync] bulk upsert success, batchId={}, targetIndex={}, written={}",
                    cardType(), batchId, targetIndex, written);

            if (!esMapper.validateIndex(targetIndex, docs.size(), batchId)) {
                throw new IllegalStateException("validate target index failed, targetIndex=" + targetIndex
                        + ", batchId=" + batchId);
            }
            log.info("[homepage-{}-sync] validate success, batchId={}, targetIndex={}, validatedCount={}",
                    cardType(), batchId, targetIndex, docs.size());

            esMapper.switchAlias(currentIndex, targetIndex);
            log.info("[homepage-{}-sync] alias switched, batchId={}, alias={}, from={}, to={}",
                    cardType(),
                    batchId,
                    esMapper.readAlias(),
                    currentIndex == null ? "none" : currentIndex,
                    targetIndex);

            log.info("[homepage-{}-sync] finished, batchId={}, onlineIndex={}, costMs={}",
                    cardType(), batchId, targetIndex, System.currentTimeMillis() - start);
            return written;
        } catch (Exception exception) {
            log.error("[homepage-{}-sync] failed, batchId={}, currentIndex={}, targetIndex={}, error={}",
                    cardType(),
                    batchId,
                    currentIndex == null ? "none" : currentIndex,
                    targetIndex == null ? "none" : targetIndex,
                    exception.getMessage(),
                    exception);
            throw new IllegalStateException("sync homepage " + cardType() + " to es failed", exception);
        }
    }

    @Override
    public void ensureIndexReady() {
        try {
            AbstractOshHomePageAliasEsMapper<D> esMapper = esMapper();
            if (!esMapper.indexExists()) {
                log.info("[homepage-{}] index/alias not found, initialize a/b indexes and sync automatically",
                        cardType());
                esMapper.recreateIndex(esMapper.loadIndexMappingJson());
                syncToEs();
                return;
            }
            if (!esMapper.aliasExists()) {
                log.info("[homepage-{}] alias missing, trigger sync to rebuild read model", cardType());
                syncToEs();
            }
        } catch (Exception exception) {
            log.warn("[homepage-{}] initialize index failed, error={}",
                    cardType(), exception.getMessage(), exception);
        }
    }

    protected List<D> queryFromEs(int limit) throws IOException {
        return esMapper().searchTop(limit);
    }

    protected List<V> queryFromMysqlForSync(int limit) {
        return queryFromMysql(limit);
    }

    protected String buildSyncBatchId() {
        return LocalDateTime.now().format(BATCH_ID_FORMATTER);
    }

    protected void fillSyncBatchIdIfSupported(List<D> docs, String syncBatchId) {
        if (docs == null || docs.isEmpty()) {
            return;
        }
        for (D doc : docs) {
            if (doc == null) {
                continue;
            }
            try {
                Method method = doc.getClass().getMethod("setSyncBatchId", String.class);
                method.invoke(doc, syncBatchId);
            } catch (Exception ignored) {
                // Only some homepage cards need syncBatchId persisted in ES.
            }
        }
    }

    protected int maxLimit() {
        return 5;
    }

    protected int syncLimit() {
        return maxLimit();
    }

    private int normalizeLimit(int requested) {
        if (requested <= 0) {
            return 0;
        }
        return Math.min(requested, maxLimit());
    }

    public abstract String cardType();

    protected abstract AbstractOshHomePageAliasEsMapper<D> esMapper();

    protected abstract List<V> queryFromMysql(int limit);

    protected abstract List<V> toVos(List<D> docs);

    protected abstract List<D> toDocs(List<V> vos, LocalDateTime syncTime);
}
