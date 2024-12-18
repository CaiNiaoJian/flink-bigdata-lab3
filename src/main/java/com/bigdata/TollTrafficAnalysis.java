package com.bigdata;

import com.bigdata.config.KafkaConfig;
import com.bigdata.model.TollEvent;
import com.bigdata.model.TollStats;
import com.bigdata.operator.TollStatsWindowFunction;
import com.bigdata.operator.TrafficPatternProcessor;
import com.bigdata.sink.HBaseSinkFunction;
import com.bigdata.utils.TimeUtils;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.cep.CEP;
import org.apache.flink.cep.PatternStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public class TollTrafficAnalysis {
    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // 从Kafka读取数据
        DataStream<TollEvent> events = env
                .fromSource(
                    KafkaConfig.createKafkaSource(),
                    WatermarkStrategy
                        .<String>forBoundedOutOfOrder(Duration.ofSeconds(5))
                        .withTimestampAssigner((event, timestamp) -> {
                            String[] fields = event.split(",");
                            // 使用TimeUtils解析GCSJ字段（过车时间）
                            return TimeUtils.parseDateTime(fields[4]).toEpochMilli();
                        }),
                    "Kafka Source"
                )
                .map(line -> {
                    String[] fields = line.split(",");
                    return new TollEvent(
                            fields[0],    // GCXH - 过车序号
                            fields[1],    // XZQHMC - 行政区划名称
                            fields[2],    // KKMC - 卡口名称
                            fields[3],    // FXLX - 方向类型
                            TimeUtils.parseDateTime(fields[4]),  // GCSJ - 过车时间
                            fields[5],    // HPZL - 号牌种类
                            fields[6],    // HP - 号牌
                            fields[7]     // CLPPXH - 车辆品牌型号
                    );
                });

        // 1. 实时统计
        DataStream<TollStats> statistics = events
                .keyBy(TollEvent::getKkmc)  // 按卡口名称分组
                .window(TumblingEventTimeWindows.of(Time.minutes(1)))
                .process(new TollStatsWindowFunction());

        // 2. CEP模式检测
        PatternStream<TollEvent> patternStream = CEP.pattern(
                events.keyBy(TollEvent::getKkmc),
                TrafficPatternProcessor.createTrafficPattern()
        );

        // 处理匹配的事件序列
        DataStream<String> alerts = patternStream.process(
                (Map<String, List<TollEvent>> pattern, Context ctx) -> {
                    TollEvent first = pattern.get("first").get(0);
                    return TrafficPatternProcessor.createAlertMessage(first);
                }
        );

        // 输出预警信息
        alerts.print();

        // 将统计结果保存到HBase
        statistics.addSink(new HBaseSinkFunction());

        env.execute("Toll Traffic Analysis");
    }
} 