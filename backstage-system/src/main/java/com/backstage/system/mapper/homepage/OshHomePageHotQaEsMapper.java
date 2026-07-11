package com.backstage.system.mapper.homepage;

import com.backstage.system.domain.homepage.es.OshHomePageHotQaEsDocument;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OshHomePageHotQaEsMapper extends AbstractOshHomePageAliasEsMapper<OshHomePageHotQaEsDocument> {

    public static final String READ_ALIAS = "homepage_hot_qa_read";
    public static final String INDEX_A = "homepage_hot_qa_a";
    public static final String INDEX_B = "homepage_hot_qa_b";
    private static final String MAPPING_RESOURCE = "es/homepage_hot_qa_index.json";

    public static String loadMappingJson() throws IOException {
        return AbstractOshHomePageAliasEsMapper.loadMappingJson(MAPPING_RESOURCE);
    }

    @Override
    protected String cardType() {
        return "homepage hot qa";
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
    protected Class<OshHomePageHotQaEsDocument> documentClass() {
        return OshHomePageHotQaEsDocument.class;
    }
}
