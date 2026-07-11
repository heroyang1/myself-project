package com.backstage.system.mapper.homepage;

import com.backstage.system.domain.homepage.es.OshHomePageHotWebsiteEsDocument;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OshHomePageHotWebsiteEsMapper extends AbstractOshHomePageAliasEsMapper<OshHomePageHotWebsiteEsDocument> {

    public static final String READ_ALIAS = "homepage_hot_website_read";
    public static final String INDEX_A = "homepage_hot_website_a";
    public static final String INDEX_B = "homepage_hot_website_b";
    private static final String MAPPING_RESOURCE = "es/homepage_hot_website_index.json";

    public static String loadMappingJson() throws IOException {
        return AbstractOshHomePageAliasEsMapper.loadMappingJson(MAPPING_RESOURCE);
    }

    @Override
    protected String cardType() {
        return "homepage hot website";
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
    protected Class<OshHomePageHotWebsiteEsDocument> documentClass() {
        return OshHomePageHotWebsiteEsDocument.class;
    }
}
