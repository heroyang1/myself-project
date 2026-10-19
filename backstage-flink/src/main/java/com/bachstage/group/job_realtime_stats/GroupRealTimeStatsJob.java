package com.bachstage.group.job_realtime_stats;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.bachstage.group.job_realtime_stats.config.GroupRealTimeStatsConfig;
import com.bachstage.group.job_realtime_stats.sink.GroupStatsResult;
import com.bachstage.group.job_realtime_stats.sink.RedisStatsSink;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.contrib.streaming.state.EmbeddedRocksDBStateBackend;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

/**
 * 拼团实时统计仪表盘 Flink 作业
 *
 * <p>消费 Kafka Topic: {@code osh.group.order}，按 activityId 分组后使用 1 小时滚动窗口聚合：</p>
 * <ul>
 *   <li><b>GMV</b>：窗口内所有 ORDER_PAID 事件的 price 累加</li>
 *   <li><b>订单数</b>：窗口内 ORDER_PAID 事件的条数</li>
 * </ul>
 *
 * <p>聚合结果写入 Redis Hash {@code group:stats:{activityId}}，供前端仪表盘 REST API 读取。</p>
 *
 * @author system
 * @date 2026-07-12
 */
public class GroupRealTimeStatsJob {

    private static final Logger log = LoggerFactory.getLogger(GroupRealTimeStatsJob.class);

    private static final String EVENT_TYPE_ORDER_PAID = "ORDER_PAID";

    public static void main(String[] args) throws Exception {
        log.info("========================================");
        log.info("GroupRealTimeStatsJob 启动中...");
        log.info("========================================");

        GroupRealTimeStatsConfig config = GroupRealTimeStatsConfig.fromSystem();

        log.info("配置信息: Kafka={}, 消费者组={}, Topic={}, Redis={}:{}",
                config.getKafkaBootstrapServers(), config.getKafkaGroupId(),
                config.getOrderTopic(), config.getRedisHost(), config.getRedisPort());

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.enableCheckpointing(60000, CheckpointingMode.AT_LEAST_ONCE);
        // RocksDB StateBackend：窗口聚合状态可能随活动数增长，RocksDB 增量 Checkpoint 更优
        env.setStateBackend(new EmbeddedRocksDBStateBackend(true));
        env.getCheckpointConfig().setCheckpointStorage("file:///tmp/flink-checkpoints/group-realtime-stats");

        // 使用新版 KafkaSource API（替代 deprecated FlinkKafkaConsumer）
        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers(config.getKafkaBootstrapServers())
                .setTopics(config.getOrderTopic())
                .setGroupId(config.getKafkaGroupId())
                .setStartingOffsets(OffsetsInitializer.latest())
                .setProperty("commit.offsets.on.checkpoints", "true")
                .setProperty("auto.offset.reset", "latest")
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        // 解析 → 过滤 → 只保留 ORDER_PAID 事件
        DataStream<JSONObject> paidOrderStream = env
                .fromSource(source, WatermarkStrategy.noWatermarks(), "group-order-source")
                .map((MapFunction<String, JSONObject>) value -> {
                    try {
                        return JSON.parseObject(value);
                    } catch (Exception ex) {
                        log.error("【拼团统计】解析消息失败: {}", value, ex);
                        return null;
                    }
                })
                .name("group-order-parse")
                .filter(msg -> msg != null)
                .name("group-order-null-filter")
                .filter(msg -> EVENT_TYPE_ORDER_PAID.equals(msg.getString("eventType")))
                .name("group-order-paid-filter");

        // 按 activityId 分组 → 1小时滚动窗口 → GMV + 订单数聚合
        paidOrderStream
                .keyBy(msg -> {
                    Long id = msg.getLong("activityId");
                    return id != null ? String.valueOf(id) : "unknown";
                })
                .window(TumblingProcessingTimeWindows.of(Time.hours(1)))
                .aggregate(new GmvAggregateFunction())
                .name("group-stats-aggregate")
                .addSink(new RedisStatsSink(config))
                .name("group-stats-redis-sink");

        log.info("GroupRealTimeStatsJob 已启动，正在监听 Topic: {}", config.getOrderTopic());
        env.execute("group-realtime-stats-job");
    }

    /**
     * GMV 聚合函数
     *
     * <p>累加窗口内所有 ORDER_PAID 事件的 price（BigDecimal），同时计数。</p>
     * <p>中间状态: [gmv, count, activityId]</p>
     */
    private static class GmvAggregateFunction
            implements AggregateFunction<JSONObject, GroupStatsAccumulator, GroupStatsResult> {

        @Override
        public GroupStatsAccumulator createAccumulator() {
            return new GroupStatsAccumulator();
        }

        @Override
        public GroupStatsAccumulator add(JSONObject event, GroupStatsAccumulator acc) {
            if (acc.activityId == null) {
                acc.activityId = event.getLong("activityId");
            }
            BigDecimal price = event.getBigDecimal("price");
            if (price != null) {
                acc.gmv = acc.gmv.add(price);
            }
            acc.orderCount++;
            return acc;
        }

        @Override
        public GroupStatsResult getResult(GroupStatsAccumulator acc) {
            return new GroupStatsResult(acc.activityId, acc.gmv, acc.orderCount, System.currentTimeMillis());
        }

        @Override
        public GroupStatsAccumulator merge(GroupStatsAccumulator a, GroupStatsAccumulator b) {
            GroupStatsAccumulator merged = new GroupStatsAccumulator();
            merged.activityId = a.activityId != null ? a.activityId : b.activityId;
            merged.gmv = a.gmv.add(b.gmv);
            merged.orderCount = a.orderCount + b.orderCount;
            return merged;
        }
    }

    /**
     * 聚合中间状态
     */
    private static class GroupStatsAccumulator {
        Long activityId;
        BigDecimal gmv = BigDecimal.ZERO;
        long orderCount = 0;
    }
}
