package com.bachstage.group.job_realtime_stats.config;

import com.bachstage.course.job_stream_from_kafka_to_es.config.ApplicationPropertiesConfig;

/**
 * 拼团实时统计任务配置
 *
 * @author system
 * @date 2026-07-12
 */
public class GroupRealTimeStatsConfig {

    private final String kafkaBootstrapServers;
    private final String kafkaGroupId;
    private final String orderTopic;
    private final String redisHost;
    private final int redisPort;
    private final String redisPassword;

    private GroupRealTimeStatsConfig(String kafkaBootstrapServers, String kafkaGroupId,
                                      String orderTopic, String redisHost, int redisPort,
                                      String redisPassword) {
        this.kafkaBootstrapServers = kafkaBootstrapServers;
        this.kafkaGroupId = kafkaGroupId;
        this.orderTopic = orderTopic;
        this.redisHost = redisHost;
        this.redisPort = redisPort;
        this.redisPassword = redisPassword;
    }

    public static GroupRealTimeStatsConfig fromSystem() {
        return new GroupRealTimeStatsConfig(
                ApplicationPropertiesConfig.read("kafka.bootstrap-servers", "KAFKA_BOOTSTRAP_SERVERS", "43.242.200.25:9092"),
                ApplicationPropertiesConfig.read("group.stats.group-id", "GROUP_STATS_GROUP_ID", "backstage-group-stats-flink"),
                ApplicationPropertiesConfig.read("group.order.topic", "GROUP_ORDER_TOPIC", "osh.group.order"),
                ApplicationPropertiesConfig.read("redis.host", "REDIS_HOST", "43.242.200.25"),
                Integer.parseInt(ApplicationPropertiesConfig.read("redis.port", "REDIS_PORT", "6379")),
                ApplicationPropertiesConfig.read("redis.password", "REDIS_PASSWORD", ""));
    }

    public String getKafkaBootstrapServers() { return kafkaBootstrapServers; }
    public String getKafkaGroupId() { return kafkaGroupId; }
    public String getOrderTopic() { return orderTopic; }
    public String getRedisHost() { return redisHost; }
    public int getRedisPort() { return redisPort; }
    public String getRedisPassword() { return redisPassword; }
}
