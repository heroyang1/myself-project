package com.backstage.system.mapper.homepage;

import com.backstage.system.domain.homepage.es.OshHomePageHotFeedbackEsDocument;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class OshHomePageHotFeedbackEsMapper extends AbstractOshHomePageAliasEsMapper<OshHomePageHotFeedbackEsDocument> {

    public static final String READ_ALIAS = "homepage_hot_feedback_read";
    public static final String INDEX_A = "homepage_hot_feedback_a";
    public static final String INDEX_B = "homepage_hot_feedback_b";
    private static final String MAPPING_RESOURCE = "es/homepage_hot_feedback_index.json";

    public List<OshHomePageHotFeedbackEsDocument> searchTopFeedback(int limit) throws IOException {
        return searchTop(limit);
    }

    public static String loadMappingJson() throws IOException {
        return AbstractOshHomePageAliasEsMapper.loadMappingJson(MAPPING_RESOURCE);
    }

    @Override
    protected String cardType() {
        return "homepage hot feedback";
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
    protected Class<OshHomePageHotFeedbackEsDocument> documentClass() {
        return OshHomePageHotFeedbackEsDocument.class;
    }
}
