package com.bachstage.seckill.job_stream_from_kafka_to_es.config;

import com.bachstage.course.job_stream_from_kafka_to_es.config.ApplicationPropertiesConfig;

/**
 * 绉掓潃鍟嗗搧鏄庣粏绱㈠紩鍚屾浠诲姟閰嶇疆
 */
public class SeckillItemJobConfig {

    private final String kafkaBootstrapServers;
    private final String kafkaGroupId;
    private final String topic;
    private final String esHosts;
    private final String esIndex;

    private SeckillItemJobConfig(String kafkaBootstrapServers, String kafkaGroupId,
                                  String topic, String esHosts, String esIndex) {
        this.kafkaBootstrapServers = kafkaBootstrapServers;
        this.kafkaGroupId = kafkaGroupId;
        this.topic = topic;
        this.esHosts = esHosts;
        this.esIndex = esIndex;
    }

    public static SeckillItemJobConfig fromSystem() {
        return new SeckillItemJobConfig(
                ApplicationPropertiesConfig.read("kafka.bootstrap-servers", "KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"),
                ApplicationPropertiesConfig.read("seckill.item.index.group-id", "SECKILL_ITEM_INDEX_GROUP_ID", "backstage-seckill-item-index-flink"),
                ApplicationPropertiesConfig.read("seckill.item.index.topic", "SECKILL_ITEM_INDEX_TOPIC", "osh.seckill.item.index"),
                ApplicationPropertiesConfig.read("elasticsearch.hosts", "ES_HOSTS", "http://localhost:9200"),
                ApplicationPropertiesConfig.read("seckill.item.index.es-index", "SECKILL_ITEM_ES_INDEX", "osh_seckill_item_search"));
    }

    public String getKafkaBootstrapServers() { return kafkaBootstrapServers; }
    public String getKafkaGroupId() { return kafkaGroupId; }
    public String getTopic() { return topic; }
    public String getEsHosts() { return esHosts; }
    public String getEsIndex() { return esIndex; }
}
