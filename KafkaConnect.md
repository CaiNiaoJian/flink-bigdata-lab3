# Kafka 连接与操作指南

## 一、Kafka连接原理

### 1. 连接架构
```
Flink Application -> Kafka Consumer -> Kafka Broker -> ZooKeeper
```

### 2. 连接流程
1. **消费者协调**
   - Kafka使用ZooKeeper管理broker和消费者组
   - 消费者通过ZooKeeper发现broker
   - 消费者组自动负载均衡

2. **连接建立过程**
   ```
   1. 初始化Kafka配置
   2. 创建消费者组
   3. 订阅主题
   4. 开始消费数据
   ```

### 3. 程序中的实现
```java
// 1. Kafka配置类
public class KafkaConfig {
    public static KafkaSource<String> createKafkaSource() {
        return KafkaSource.<String>builder()
                .setBootstrapServers("localhost:9092")
                .setTopics("toll-events")
                .setGroupId("toll-analysis-group")
                .setStartingOffsets(OffsetsInitializer.latest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();
    }
}

// 2. 在Flink中使用
DataStream<TollEvent> events = env
    .fromSource(
        KafkaConfig.createKafkaSource(),
        WatermarkStrategy
            .<String>forBoundedOutOfOrder(Duration.ofSeconds(5))
            .withTimestampAssigner((event, timestamp) -> {
                String[] fields = event.split(",");
                return TimeUtils.parseDateTime(fields[4]).toEpochMilli();
            }),
        "Kafka Source"
    );
```

## 二、Kafka主题设计

### 1. 主题创建
```bash
kafka-topics.sh --create --topic toll-events \
    --bootstrap-server localhost:9092 \
    --partitions 3 \
    --replication-factor 1
```

### 2. 数据格式
- 格式：CSV字符串，字段用逗号分隔
- 字段：
  ```
  GCXH,XZQHMC,KKMC,FXLX,GCSJ,HPZL,HP,CLPPXH
  ```
- 示例：
  ```
  001,杭州市,西湖卡口,进城,2023-12-18 10:15:30,小型汽车,浙A12345,奥迪A6
  ```

### 3. 分区策略
- 分区数：3（可根据数据量调整）
- 分区键：卡口名称（KKMC）
- 目的：保证同一卡口的数据进入同一分区，保持顺序性

## 三、Linux环境配置与操作

### 1. Kafka环境配置

1. **下载和解压**
```bash
wget https://downloads.apache.org/kafka/3.4.0/kafka_2.12-3.4.0.tgz
tar -xzf kafka_2.12-3.4.0.tgz
mv kafka_2.12-3.4.0 /usr/local/kafka
```

2. **配置环境变量**
```bash
vim ~/.bashrc

# 添加以下内容
export KAFKA_HOME=/usr/local/kafka
export PATH=$PATH:$KAFKA_HOME/bin

# 使配置生效
source ~/.bashrc
```

3. **修改Kafka配置**
```bash
cd $KAFKA_HOME/config
vim server.properties
```

关键配置：
```properties
# broker ID
broker.id=0

# 监听地址
listeners=PLAINTEXT://localhost:9092

# 日志目录
log.dirs=/tmp/kafka-logs

# ZooKeeper连接
zookeeper.connect=localhost:2181
```

### 2. Kafka常用命令

1. **服务启动和停止**
```bash
# 启动ZooKeeper（如果使用Kafka内置的ZooKeeper）
bin/zookeeper-server-start.sh -daemon config/zookeeper.properties

# 启动Kafka
bin/kafka-server-start.sh -daemon config/server.properties

# 停止Kafka
bin/kafka-server-stop.sh

# 停止ZooKeeper
bin/zookeeper-server-stop.sh
```

2. **主题管理**
```bash
# 创建主题
kafka-topics.sh --create --topic toll-events \
    --bootstrap-server localhost:9092 \
    --partitions 3 \
    --replication-factor 1

# 查看主题列表
kafka-topics.sh --list --bootstrap-server localhost:9092

# 查看主题详情
kafka-topics.sh --describe --topic toll-events \
    --bootstrap-server localhost:9092

# 删除主题
kafka-topics.sh --delete --topic toll-events \
    --bootstrap-server localhost:9092
```

3. **生产和消费测试**
```bash
# 生产者测试
kafka-console-producer.sh --broker-list localhost:9092 \
    --topic toll-events

# 消费者测试
kafka-console-consumer.sh --bootstrap-server localhost:9092 \
    --topic toll-events \
    --from-beginning
```

4. **数据导入导出**
```bash
# 从文件导入数据
kafka-console-producer.sh --broker-list localhost:9092 \
    --topic toll-events < toll_data.csv

# 导出数据到文件
kafka-console-consumer.sh --bootstrap-server localhost:9092 \
    --topic toll-events \
    --from-beginning > toll_data_export.csv
```

### 3. 故障排查

1. **检查服务状态**
```bash
# 检查进程
jps | grep -E 'Kafka|QuorumPeerMain'

# 检查端口
netstat -nltp | grep -E '2181|9092'

# 查看日志
tail -f $KAFKA_HOME/logs/server.log
```

2. **常见问题解决**
```bash
# 清理日志
rm -rf /tmp/kafka-logs/*
rm -rf /tmp/zookeeper/*

# 重启服务
bin/kafka-server-stop.sh
bin/zookeeper-server-stop.sh
bin/zookeeper-server-start.sh -daemon config/zookeeper.properties
bin/kafka-server-start.sh -daemon config/server.properties
```

## 四、数据生产和消费示例

### 1. 生产者示例
```bash
# 使用CSV文件生产数据
while IFS= read -r line
do
    echo "$line" | kafka-console-producer.sh \
        --broker-list localhost:9092 \
        --topic toll-events
done < toll_data.csv
```

### 2. 消费者示例
```bash
# 实时监控数据
kafka-console-consumer.sh --bootstrap-server localhost:9092 \
    --topic toll-events \
    --property print.timestamp=true

# 指定消费者组消费
kafka-console-consumer.sh --bootstrap-server localhost:9092 \
    --topic toll-events \
    --group toll-analysis-group
```

### 3. 性能测试
```bash
# 生产者性能测试
kafka-producer-perf-test.sh --topic toll-events \
    --num-records 100000 \
    --record-size 1000 \
    --throughput 10000 \
    --producer-props bootstrap.servers=localhost:9092

# 消费者性能测试
kafka-consumer-perf-test.sh --bootstrap-server localhost:9092 \
    --topic toll-events \
    --messages 100000
```

## 五、性能优化建议

1. **生产者优化**
   - 适当的批量大小
   - 压缩配置
   - 合理的重试策略

2. **消费者优化**
   - 合理的分区数
   - 适当的消费者数量
   - 批量提交配置

3. **系统优化**
   - 合理的JVM配置
   - 磁盘和网络优化
   - 监控和告警配置 