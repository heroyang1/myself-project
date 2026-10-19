package com.bachstage.group.job_stream_from_kafka_to_es.es;

import com.alibaba.fastjson2.JSONObject;
import com.bachstage.course.job_stream_from_kafka_to_es.config.ApplicationPropertiesConfig;
import com.bachstage.course.job_stream_from_kafka_to_es.config.ElasticsearchAuthConfig;
import com.bachstage.course.job_stream_from_kafka_to_es.es.ElasticsearchHostParser;
import com.bachstage.group.job_stream_from_kafka_to_es.config.GroupActivityJobConfig;
import com.bachstage.group.job_stream_from_kafka_to_es.serialize.GroupActivityJsonSerializer;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.streaming.connectors.elasticsearch.ElasticsearchSinkFunction;
import org.apache.flink.streaming.connectors.elasticsearch.RequestIndexer;
import org.apache.flink.streaming.connectors.elasticsearch6.ElasticsearchSink;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.util.Base64;
import java.util.List;

/**
 * 拼团活动索引 Elasticsearch Sink 工厂
 *
 * @author system
 * @date 2026-07-12
 */
public class GroupActivityElasticsearchSinkFactory {

    private static final Logger log = LoggerFactory.getLogger(GroupActivityElasticsearchSinkFactory.class);

    /**
     * 构建 upsert sink：将活动文档写入 ES（IndexRequest，覆盖写）
     */
    public static ElasticsearchSink<JSONObject> buildUpsertSink(GroupActivityJobConfig config)
            throws MalformedURLException {
        final String esIndex = config.getEsIndex();
        List<HttpHost> httpHosts = ElasticsearchHostParser.parseHosts(config.getEsHosts());

        ElasticsearchSink.Builder<JSONObject> builder = new ElasticsearchSink.Builder<>(
                httpHosts,
                new ElasticsearchSinkFunction<JSONObject>() {
                    @Override
                    public void process(JSONObject message, RuntimeContext ctx, RequestIndexer indexer) {
                        String jsonString = GroupActivityJsonSerializer.buildJsonWithTimestamp(message);
                        IndexRequest request = Requests.indexRequest()
                                .index(esIndex)
                                .type("_doc")
                                .id(String.valueOf(message.getLong("id")))
                                .source(jsonString, XContentType.JSON);
                        log.info("【拼团活动ES upsert】活动ID: {}, 标题: {}",
                                message.getLong("id"), message.getString("title"));
                        indexer.add(request);
                    }
                });
        configureBuilder(builder, "拼团活动upsert");
        return builder.build();
    }

    /**
     * 构建 delete sink：从 ES 删除活动文档（DeleteRequest）
     */
    public static ElasticsearchSink<JSONObject> buildDeleteSink(GroupActivityJobConfig config)
            throws MalformedURLException {
        final String esIndex = config.getEsIndex();
        List<HttpHost> httpHosts = ElasticsearchHostParser.parseHosts(config.getEsHosts());

        ElasticsearchSink.Builder<JSONObject> builder = new ElasticsearchSink.Builder<>(
                httpHosts,
                new ElasticsearchSinkFunction<JSONObject>() {
                    @Override
                    public void process(JSONObject message, RuntimeContext ctx, RequestIndexer indexer) {
                        Long activityId = message.getLong("id");
                        DeleteRequest request = new DeleteRequest(esIndex, "_doc", String.valueOf(activityId));
                        log.info("【拼团活动ES delete】活动ID: {}", activityId);
                        indexer.add(request);
                    }
                });
        configureBuilder(builder, "拼团活动delete");
        return builder.build();
    }

    private static void configureBuilder(ElasticsearchSink.Builder<JSONObject> builder, String label) {
        builder.setBulkFlushMaxActions(ApplicationPropertiesConfig.readInt(
                "elasticsearch.bulk-flush-max-actions", "ES_BULK_FLUSH_MAX_ACTIONS", 200));
        builder.setBulkFlushInterval(1000);
        builder.setFailureHandler((action, failure, restStatusCode, indexer) -> {
            log.error("【拼团活动ES写入失败】sink={}, status={}, action={}, error={}",
                    label, restStatusCode, action, failure.getMessage(), failure);
            throw failure;
        });

        String auth = ElasticsearchAuthConfig.fromProperties().buildBasicAuth();
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        Header[] headers = new Header[]{new BasicHeader("Authorization", "Basic " + encodedAuth)};
        builder.setRestClientFactory(restClientBuilder -> {
            restClientBuilder.setDefaultHeaders(headers);
            restClientBuilder.setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder
                    .setConnectTimeout(30000)
                    .setSocketTimeout(60000));
        });
    }
}
