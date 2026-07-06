package com.backstage.system.mapper.homepage;

import com.backstage.system.config.properties.SearchEsProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 首页卡片 ES 读写公共底座：
 * 单个业务卡片只需要提供索引命名、mapping 资源、文档类型即可复用 A/B + alias 切换能力。
 *
 * @param <D> ES document type
 */
public abstract class AbstractOshHomePageAliasEsMapper<D> {

    @Autowired
    protected RestHighLevelClient restHighLevelClient;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected SearchEsProperties searchEsProperties;

    public boolean indexExists() throws IOException {
        return aliasExists() || indexExists(indexA()) || indexExists(indexB());
    }

    public boolean indexExists(String indexName) throws IOException {
        if (isBlank(indexName)) {
            return false;
        }
        return restHighLevelClient.indices().exists(new GetIndexRequest(indexName), RequestOptions.DEFAULT);
    }

    public boolean aliasExists() {
        try {
            Request request = new Request("HEAD", "/_alias/" + readAlias());
            restHighLevelClient.getLowLevelClient().performRequest(request);
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    public String resolveCurrentAliasIndex() throws IOException {
        Request request = new Request("GET", "/_alias/" + readAlias());
        try {
            Response response = restHighLevelClient.getLowLevelClient().performRequest(request);
            String json = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            JsonNode root = objectMapper.readTree(json);
            return root.fieldNames().hasNext() ? root.fieldNames().next() : null;
        } catch (Exception exception) {
            return null;
        }
    }

    public String resolveTargetIndex(String currentIndex) {
        if (indexA().equalsIgnoreCase(currentIndex)) {
            return indexB();
        }
        return indexA();
    }

    public void createIndexIfAbsent(String indexName, String indexDefinitionJson) throws IOException {
        if (indexExists(indexName)) {
            return;
        }
        CreateIndexRequest createIndexRequest = new CreateIndexRequest(indexName);
        createIndexRequest.source(indexDefinitionJson, XContentType.JSON);
        restHighLevelClient.indices().create(createIndexRequest, RequestOptions.DEFAULT);
    }

    public List<D> searchTop(int limit) throws IOException {
        return searchTop(readAlias(), limit);
    }

    public List<D> searchTop(String indexOrAlias, int limit) throws IOException {
        if (limit <= 0 || isBlank(indexOrAlias)) {
            return Collections.emptyList();
        }
        if (!resourceExistsQuietly(indexOrAlias)) {
            return Collections.emptyList();
        }

        SearchRequest searchRequest = new SearchRequest(indexOrAlias);
        searchRequest.source(buildSearchSource(limit));

        SearchResponse searchResponse;
        try {
            searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        } catch (ElasticsearchStatusException exception) {
            if (exception.status() != null && exception.status().getStatus() == 404) {
                return Collections.emptyList();
            }
            throw exception;
        }

        if (searchResponse.isTimedOut()) {
            throw new IllegalStateException("search " + cardType() + " from es timed out");
        }

        List<D> documents = new ArrayList<>();
        for (SearchHit searchHit : searchResponse.getHits().getHits()) {
            documents.add(toDocument(searchHit.getSourceAsMap()));
        }
        return documents;
    }

    public int deleteAll(String indexName) throws IOException {
        if (!indexExists(indexName)) {
            return 0;
        }

        Request request = new Request("POST", "/" + indexName + "/_delete_by_query");
        request.addParameter("refresh", "true");
        request.addParameter("conflicts", "proceed");
        request.setJsonEntity("{\"query\":{\"match_all\":{}}}");

        Response response = restHighLevelClient.getLowLevelClient().performRequest(request);
        JsonNode root = objectMapper.readTree(EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8));
        return root.path("deleted").asInt(0);
    }

    public int bulkUpsert(String indexName, List<D> documents) throws IOException {
        if (documents == null || documents.isEmpty()) {
            return 0;
        }

        BulkRequest bulkRequest = new BulkRequest();
        for (D document : documents) {
            Long id = documentId(document);
            if (document == null || id == null) {
                continue;
            }
            bulkRequest.add(new IndexRequest(indexName)
                    .id(String.valueOf(id))
                    .source(objectMapper.writeValueAsString(document), XContentType.JSON));
        }

        if (bulkRequest.numberOfActions() == 0) {
            return 0;
        }

        BulkResponse bulkResponse = restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
        if (bulkResponse.hasFailures()) {
            throw new IllegalStateException("bulk upsert " + cardType() + " to es failed: "
                    + bulkResponse.buildFailureMessage());
        }
        return bulkRequest.numberOfActions();
    }

    public void switchAlias(String oldIndexName, String targetIndexName) throws IOException {
        if (isBlank(targetIndexName)) {
            throw new IllegalArgumentException("targetIndexName must not be blank");
        }

        String aliasJson;
        if (!isBlank(oldIndexName) && !oldIndexName.equals(targetIndexName)) {
            aliasJson = String.format(
                    "{\"actions\":[{\"remove\":{\"index\":\"%s\",\"alias\":\"%s\"}},{\"add\":{\"index\":\"%s\",\"alias\":\"%s\"}}]}",
                    oldIndexName, readAlias(), targetIndexName, readAlias());
        } else {
            aliasJson = String.format(
                    "{\"actions\":[{\"add\":{\"index\":\"%s\",\"alias\":\"%s\"}}]}",
                    targetIndexName, readAlias());
        }
        Request aliasRequest = new Request("POST", "/_aliases");
        aliasRequest.setJsonEntity(aliasJson);
        restHighLevelClient.getLowLevelClient().performRequest(aliasRequest);
    }

    public boolean validateIndex(String indexName, int expectedCount, String syncBatchId) throws IOException {
        if (!indexExists(indexName)) {
            return false;
        }
        List<D> docs = searchTop(indexName, Math.max(expectedCount, 5));
        if (docs.size() != expectedCount) {
            return false;
        }
        for (int i = 0; i < docs.size(); i++) {
            D doc = docs.get(i);
            Long id = documentId(doc);
            Integer sortNo = extractIntegerValue(doc, "getSortNo");
            if (id == null || sortNo == null || sortNo != i + 1) {
                return false;
            }
            String batchInDoc = extractStringValue(doc, "getSyncBatchId");
            if (batchInDoc != null && !safeEquals(syncBatchId, batchInDoc)) {
                return false;
            }
        }
        return true;
    }

    public void recreateIndex(String indexDefinitionJson) throws IOException {
        recreateIndex(indexA(), indexDefinitionJson);
        recreateIndex(indexB(), indexDefinitionJson);
    }

    public void recreateIndex(String indexName, String indexDefinitionJson) throws IOException {
        if (indexExists(indexName)) {
            restHighLevelClient.indices().delete(new DeleteIndexRequest(indexName), RequestOptions.DEFAULT);
        }

        CreateIndexRequest createIndexRequest = new CreateIndexRequest(indexName);
        createIndexRequest.source(indexDefinitionJson, XContentType.JSON);
        restHighLevelClient.indices().create(createIndexRequest, RequestOptions.DEFAULT);
    }

    public String loadIndexMappingJson() throws IOException {
        return loadMappingJson(mappingResource());
    }

    protected D toDocument(Map<String, Object> source) throws IOException {
        try {
            return objectMapper.convertValue(source == null ? Collections.emptyMap() : source, documentClass());
        } catch (IllegalArgumentException exception) {
            throw new IOException("deserialize " + cardType() + " es document failed", exception);
        }
    }

    protected SearchSourceBuilder buildSearchSource(int limit) {
        return new SearchSourceBuilder()
                .from(0)
                .size(limit)
                .timeout(TimeValue.timeValueMillis(searchEsProperties.getFallbackTimeoutMillis()))
                .sort(SortBuilders.fieldSort("sortNo").order(SortOrder.ASC).missing("_last").unmappedType("integer"))
                .sort(SortBuilders.fieldSort(tiebreakerField()).order(SortOrder.ASC).unmappedType("long"));
    }

    protected boolean resourceExistsQuietly(String indexOrAlias) {
        try {
            if (readAlias().equals(indexOrAlias)) {
                return aliasExists();
            }
            return indexExists(indexOrAlias);
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }

    protected static String loadMappingJson(String mappingResource) throws IOException {
        ClassPathResource resource = new ClassPathResource(mappingResource);
        return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    }

    protected Long documentId(D document) {
        return extractLongValue(document, "getId");
    }

    protected String tiebreakerField() {
        return "id";
    }

    protected String extractStringValue(D document, String methodName) {
        Object value = invokeGetter(document, methodName);
        return value == null ? null : String.valueOf(value);
    }

    protected Long extractLongValue(D document, String methodName) {
        Object value = invokeGetter(document, methodName);
        return value instanceof Number ? ((Number) value).longValue() : null;
    }

    protected Integer extractIntegerValue(D document, String methodName) {
        Object value = invokeGetter(document, methodName);
        return value instanceof Number ? ((Number) value).intValue() : null;
    }

    protected Object invokeGetter(D document, String methodName) {
        if (document == null) {
            return null;
        }
        try {
            Method method = document.getClass().getMethod(methodName);
            return method.invoke(document);
        } catch (Exception exception) {
            return null;
        }
    }

    protected boolean safeEquals(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }

    protected boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    protected abstract String cardType();

    public abstract String readAlias();

    public abstract String indexA();

    public abstract String indexB();

    protected abstract String mappingResource();

    protected abstract Class<D> documentClass();
}
