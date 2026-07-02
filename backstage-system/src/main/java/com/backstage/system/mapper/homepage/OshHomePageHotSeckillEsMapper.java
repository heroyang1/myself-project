package com.backstage.system.mapper.homepage;

import com.backstage.system.domain.homepage.es.OshHomePageHotSeckillEsDocument;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OshHomePageHotSeckillEsMapper extends AbstractOshHomePageAliasEsMapper<OshHomePageHotSeckillEsDocument> {

    public static final String READ_ALIAS = "homepage_hot_seckill_read";
    public static final String INDEX_A = "homepage_hot_seckill_a";
    public static final String INDEX_B = "homepage_hot_seckill_b";
    private static final String MAPPING_RESOURCE = "es/homepage_hot_seckill_index.json";

    public static String loadMappingJson() throws IOException {
        return AbstractOshHomePageAliasEsMapper.loadMappingJson(MAPPING_RESOURCE);
    }

    @Override
    protected String cardType() {
        return "homepage hot seckill";
    }

    @Override
    public String readAlias() {
        return READ_ALIAS;
    }

    @Override
    public String indexA() {
        return INDEX_A;
    }

    @Override
    public String indexB() {
        return INDEX_B;
    }

    @Override
    protected String mappingResource() {
        return MAPPING_RESOURCE;
    }

    @Override
    protected Class<OshHomePageHotSeckillEsDocument> documentClass() {
        return OshHomePageHotSeckillEsDocument.class;
    }

    @Override
    protected Long documentId(OshHomePageHotSeckillEsDocument document) {
        return document == null ? null : document.getActivityId();
    }

    @Override
    protected String tiebreakerField() {
        return "activityId";
    }
}
