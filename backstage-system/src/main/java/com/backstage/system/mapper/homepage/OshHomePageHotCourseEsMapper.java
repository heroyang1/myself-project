package com.backstage.system.mapper.homepage;

import com.backstage.system.domain.homepage.es.OshHomePageHotCourseEsDocument;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class OshHomePageHotCourseEsMapper extends AbstractOshHomePageAliasEsMapper<OshHomePageHotCourseEsDocument> {

    public static final String READ_ALIAS = "homepage_hot_course_read";
    public static final String INDEX_A = "homepage_hot_course_a";
    public static final String INDEX_B = "homepage_hot_course_b";
    private static final String MAPPING_RESOURCE = "es/homepage_hot_course_index.json";

    public List<OshHomePageHotCourseEsDocument> searchTopCourses(int limit) throws IOException {
        return searchTop(limit);
    }

    public List<OshHomePageHotCourseEsDocument> searchTopCourses(String indexOrAlias, int limit) throws IOException {
        return searchTop(indexOrAlias, limit);
    }

    public static String loadMappingJson() throws IOException {
        return AbstractOshHomePageAliasEsMapper.loadMappingJson(MAPPING_RESOURCE);
    }

    @Override
    protected String cardType() {
        return "homepage hot course";
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
    protected Class<OshHomePageHotCourseEsDocument> documentClass() {
        return OshHomePageHotCourseEsDocument.class;
    }
}
