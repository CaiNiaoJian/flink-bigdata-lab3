package com.bigdata.config;

import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;

public class HBaseConfig {
    private static final String ZOOKEEPER_QUORUM = "localhost";
    private static final String ZOOKEEPER_CLIENT_PORT = "2181";
    public static final String TABLE_NAME = "toll_stats";
    public static final String COLUMN_FAMILY = "stats";

    public static org.apache.hadoop.conf.Configuration getHBaseConfig() {
        org.apache.hadoop.conf.Configuration config = HBaseConfiguration.create();
        config.set("hbase.zookeeper.quorum", ZOOKEEPER_QUORUM);
        config.set("hbase.zookeeper.property.clientPort", ZOOKEEPER_CLIENT_PORT);
        return config;
    }

    public static Connection createConnection() throws Exception {
        return ConnectionFactory.createConnection(getHBaseConfig());
    }
} 