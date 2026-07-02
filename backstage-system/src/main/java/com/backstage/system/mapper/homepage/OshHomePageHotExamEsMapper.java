package com.backstage.system.mapper.homepage;

import com.backstage.system.domain.homepage.es.OshHomePageHotExamEsDocument;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OshHomePageHotExamEsMapper extends AbstractOshHomePageAliasEsMapper<OshHomePageHotExamEsDocument> {

    public static final String READ_ALIAS = "homepage_hot_exam_read";
    public static final String INDEX_A = "homepage_hot_exam_a";
    public static final String INDEX_B = "homepage_hot_exam_b";
    private static final String MAPPING_RESOURCE = "es/homepage_hot_exam_index.json";

    public static String loadMappingJson() throws IOException {
        return AbstractOshHomePageAliasEsMapper.loadMappingJson(MAPPING_RESOURCE);
    }

    @Override
    protected String cardType() {
        return "homepage hot exam";
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
    protected Class<OshHomePageHotExamEsDocument> documentClass() {
        return OshHomePageHotExamEsDocument.class;
    }
}
