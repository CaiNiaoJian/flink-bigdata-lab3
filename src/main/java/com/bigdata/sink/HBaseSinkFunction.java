package com.bigdata.sink;

import com.bigdata.config.HBaseConfig;
import com.bigdata.model.TollStats;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;

public class HBaseSinkFunction extends RichSinkFunction<TollStats> {
    private transient Connection connection;
    private transient Table table;

    @Override
    public void open(Configuration parameters) throws Exception {
        connection = HBaseConfig.createConnection();
        table = connection.getTable(TableName.valueOf(HBaseConfig.TABLE_NAME));
    }

    @Override
    public void invoke(TollStats record, Context context) throws Exception {
        Put put = new Put(Bytes.toBytes(record.getKkmc() + "_" + record.getWindowStart()));
        
        put.addColumn(Bytes.toBytes(HBaseConfig.COLUMN_FAMILY), 
                Bytes.toBytes("kkmc"), 
                Bytes.toBytes(record.getKkmc()));
        put.addColumn(Bytes.toBytes(HBaseConfig.COLUMN_FAMILY), 
                Bytes.toBytes("xzqhmc"), 
                Bytes.toBytes(record.getXzqhmc()));
        put.addColumn(Bytes.toBytes(HBaseConfig.COLUMN_FAMILY), 
                Bytes.toBytes("window_start"), 
                Bytes.toBytes(record.getWindowStart()));
        put.addColumn(Bytes.toBytes(HBaseConfig.COLUMN_FAMILY), 
                Bytes.toBytes("window_end"), 
                Bytes.toBytes(record.getWindowEnd()));
        put.addColumn(Bytes.toBytes(HBaseConfig.COLUMN_FAMILY), 
                Bytes.toBytes("vehicle_count"), 
                Bytes.toBytes(record.getVehicleCount()));
        put.addColumn(Bytes.toBytes(HBaseConfig.COLUMN_FAMILY), 
                Bytes.toBytes("fxlx"), 
                Bytes.toBytes(record.getFxlx()));
        
        table.put(put);
    }

    @Override
    public void close() throws Exception {
        if (table != null) {
            table.close();
        }
        if (connection != null) {
            connection.close();
        }
    }
} 