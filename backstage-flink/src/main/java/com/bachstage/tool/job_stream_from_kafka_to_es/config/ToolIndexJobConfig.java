package com.bachstage.tool.job_stream_from_kafka_to_es.config;

import com.bachstage.course.job_stream_from_kafka_to_es.config.ApplicationPropertiesConfig;

/**
 * 工具索引同步任务配置
 */
public class ToolIndexJobConfig
{
    private final String kafkaBootstrapServers;
    private final String kafkaGroupId;
    private final String kafkaStartMode;
    private final String topic;
    private final String esHosts;
    private final String esIndex;
    private final int parallelism;
    private final long checkpointIntervalMs;
    private final long checkpointMinPauseMs;
    private final long checkpointTimeoutMs;
    private final int maxConcurrentCheckpoints;
    private final int tolerableCheckpointFailureNumber;
    private final int restartAttempts;
    private final long restartDelayMs;

    private ToolIndexJobConfig(String kafkaBootstrapServers, String kafkaGroupId, String kafkaStartMode, String topic,
                               String esHosts, String esIndex, int parallelism,
                               long checkpointIntervalMs, long checkpointMinPauseMs, long checkpointTimeoutMs,
                               int maxConcurrentCheckpoints, int tolerableCheckpointFailureNumber,
                               int restartAttempts, long restartDelayMs)
    {
        this.kafkaBootstrapServers = kafkaBootstrapServers;
        this.kafkaGroupId = kafkaGroupId;
        this.kafkaStartMode = kafkaStartMode;
        this.topic = topic;
        this.esHosts = esHosts;
        this.esIndex = esIndex;
        this.parallelism = parallelism;
        this.checkpointIntervalMs = checkpointIntervalMs;
        this.checkpointMinPauseMs = checkpointMinPauseMs;
        this.checkpointTimeoutMs = checkpointTimeoutMs;
        this.maxConcurrentCheckpoints = maxConcurrentCheckpoints;
        this.tolerableCheckpointFailureNumber = tolerableCheckpointFailureNumber;
        this.restartAttempts = restartAttempts;
        this.restartDelayMs = restartDelayMs;
    }

    public static ToolIndexJobConfig fromSystem()
    {
        return new ToolIndexJobConfig(
                ApplicationPropertiesConfig.read("kafka.bootstrap-servers", "KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"),
                ApplicationPropertiesConfig.read("tool.index.group-id", "TOOL_INDEX_GROUP_ID", "backstage-tool-index-flink"),
                ApplicationPropertiesConfig.read("tool.index.start-mode", "TOOL_INDEX_START_MODE",
                        ApplicationPropertiesConfig.read("kafka.start-mode", "KAFKA_START_MODE", "earliest")),
                ApplicationPropertiesConfig.read("tool.index.topic", "TOOL_INDEX_TOPIC", "osh.tool.index"),
                ApplicationPropertiesConfig.read("elasticsearch.hosts", "ES_HOSTS", "http://localhost:9200"),
                ApplicationPropertiesConfig.read("tool.index.es-index", "TOOL_ES_INDEX", "osh_tool_search"),
                ApplicationPropertiesConfig.readInt("flink.parallelism", "FLINK_PARALLELISM", 1),
                ApplicationPropertiesConfig.readLong("flink.checkpoint.interval-ms", "FLINK_CHECKPOINT_INTERVAL_MS", 60000L),
                ApplicationPropertiesConfig.readLong("flink.checkpoint.min-pause-ms", "FLINK_CHECKPOINT_MIN_PAUSE_MS", 30000L),
                ApplicationPropertiesConfig.readLong("flink.checkpoint.timeout-ms", "FLINK_CHECKPOINT_TIMEOUT_MS", 600000L),
                ApplicationPropertiesConfig.readInt("flink.checkpoint.max-concurrent", "FLINK_CHECKPOINT_MAX_CONCURRENT", 1),
                ApplicationPropertiesConfig.readInt("flink.checkpoint.tolerable-failures", "FLINK_CHECKPOINT_TOLERABLE_FAILURES", 3),
                ApplicationPropertiesConfig.readInt("flink.restart.attempts", "FLINK_RESTART_ATTEMPTS", 3),
                ApplicationPropertiesConfig.readLong("flink.restart.delay-ms", "FLINK_RESTART_DELAY_MS", 10000L));
    }

    public String getKafkaBootstrapServers()
    {
        return kafkaBootstrapServers;
    }

    public String getKafkaGroupId()
    {
        return kafkaGroupId;
    }

    public String getKafkaStartMode()
    {
        return kafkaStartMode;
    }

    public String getTopic()
    {
        return topic;
    }

    public String getEsHosts()
    {
        return esHosts;
    }

    public String getEsIndex()
    {
        return esIndex;
    }

    public int getParallelism()
    {
        return parallelism;
    }

    public long getCheckpointIntervalMs()
    {
        return checkpointIntervalMs;
    }

    public long getCheckpointMinPauseMs()
    {
        return checkpointMinPauseMs;
    }

    public long getCheckpointTimeoutMs()
    {
        return checkpointTimeoutMs;
    }

    public int getMaxConcurrentCheckpoints()
    {
        return maxConcurrentCheckpoints;
    }

    public int getTolerableCheckpointFailureNumber()
    {
        return tolerableCheckpointFailureNumber;
    }

    public int getRestartAttempts()
    {
        return restartAttempts;
    }

    public long getRestartDelayMs()
    {
        return restartDelayMs;
    }
}
