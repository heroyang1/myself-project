package com.bachstage.group.job_stream_from_kafka_to_es.kafka;

import com.bachstage.group.job_stream_from_kafka_to_es.config.GroupActivityJobConfig;

import java.util.Properties;

/**
 * 拼团活动索引 Kafka 消费参数工厂
 *
 * @author system
 * @date 2026-07-12
 */
public class GroupActivityKafkaPropertiesFactory {

    public static Properties create(GroupActivityJobConfig config) {
        Properties props = new Properties();
        props.setProperty("bootstrap.servers", config.getKafkaBootstrapServers());
        props.setProperty("group.id", config.getKafkaGroupId());
        props.setProperty("auto.offset.reset", "latest");
        props.setProperty("enable.auto.commit", "false");
        props.setProperty("auto.commit.interval.ms", "1000");
        props.setProperty("session.timeout.ms", "30000");
        props.setProperty("request.timeout.ms", "40000");
        return props;
    }
}
