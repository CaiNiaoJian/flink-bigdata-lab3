package com.bigdata.config;

import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;

public class KafkaConfig {
    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String TOPIC = "toll-events";
    private static final String GROUP_ID = "toll-analysis-group";

    public static KafkaSource<String> createKafkaSource() {
        return KafkaSource.<String>builder()
                .setBootstrapServers(BOOTSTRAP_SERVERS)
                .setTopics(TOPIC)
                .setGroupId(GROUP_ID)
                .setStartingOffsets(OffsetsInitializer.latest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();
    }
} 