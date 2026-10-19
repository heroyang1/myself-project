package com.bachstage.group.job_stream_from_kafka_to_es.config;

import com.bachstage.course.job_stream_from_kafka_to_es.config.ApplicationPropertiesConfig;

/**
 * 拼团活动索引同步任务配置
 */
public class GroupActivityJobConfig {

    private final String kafkaBootstrapServers;
    private final String kafkaGroupId;
    private final String topic;
    private final String esHosts;
    private final String esIndex;

    private GroupActivityJobConfig(String kafkaBootstrapServers, String kafkaGroupId,
                                   String topic, String esHosts, String esIndex) {
        this.kafkaBootstrapServers = kafkaBootstrapServers;
        this.kafkaGroupId = kafkaGroupId;
        this.topic = topic;
        this.esHosts = esHosts;
        this.esIndex = esIndex;
    }

    public static GroupActivityJobConfig fromSystem() {
        return new GroupActivityJobConfig(
                ApplicationPropertiesConfig.read("kafka.bootstrap-servers", "KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"),
                ApplicationPropertiesConfig.read("group.activity.index.group-id", "GROUP_ACTIVITY_INDEX_GROUP_ID", "backstage-group-activity-index-flink"),
                ApplicationPropertiesConfig.read("group.activity.index.topic", "GROUP_ACTIVITY_INDEX_TOPIC", "osh.group.activity.index"),
                ApplicationPropertiesConfig.read("elasticsearch.hosts", "ES_HOSTS", "http://localhost:9200"),
                ApplicationPropertiesConfig.read("group.activity.index.es-index", "GROUP_ACTIVITY_ES_INDEX", "osh_group_activity_search"));
    }

    public String getKafkaBootstrapServers() { return kafkaBootstrapServers; }
    public String getKafkaGroupId() { return kafkaGroupId; }
    public String getTopic() { return topic; }
    public String getEsHosts() { return esHosts; }
    public String getEsIndex() { return esIndex; }
}
