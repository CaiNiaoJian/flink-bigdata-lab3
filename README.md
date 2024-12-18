# 收费站交通流量分析系统

基于Apache Flink的实时交通流量分析系统，用于处理收费站车流数据并进行实时分析和预警。

## 功能特性

- 实时数据处理：从Kafka接收收费站实时数据
- 统计分析：
  - 按分钟统计每个卡口的车流量
  - 统计主要车流方向
- 异常检测：
  - 使用Flink CEP检测交通异常
  - 支持车流量激增预警
- 数据存储：
  - 统计结果存入HBase
  - 支持历史数据查询

## 技术栈

- Apache Flink 1.17.0
- Apache Kafka
- Apache HBase
- Java 8

## 项目结构

```
src/main/java/com/bigdata/
├── TollTrafficAnalysis.java     # 主程序入口
├── config/                      # 配置类
│   ├── KafkaConfig.java        # Kafka配置
│   └── HBaseConfig.java        # HBase配置
├── model/                       # 数据模型
│   ├── TollEvent.java         # 收费站事件
│   └── TollStats.java         # 统计结果
├── operator/                    # 算子
│   ├── TollStatsWindowFunction.java   # 窗口统计
│   └── TrafficPatternProcessor.java   # CEP处理
├── sink/                        # 数据输出
│   └── HBaseSinkFunction.java  # HBase写入
└── utils/                       # 工具类
    └── TimeUtils.java          # 时间处理
```

## 代码详细说明

### 1. 数据模型 (model/)

#### TollEvent.java - 收费站事件数据模型
```java
public class TollEvent {
    private String gcxh;      // 过车序号
    private String xzqhmc;    // 行政区划名称
    private String kkmc;      // 卡口名称
    private String fxlx;      // 方向类型
    private Instant gcsj;     // 过车时间
    private String hpzl;      // 号牌种类
    private String hp;        // 号牌
    private String clppxh;    // 车辆品牌型号
    // ... getters and setters
}
```

#### TollStats.java - 统计结果数据模型
```java
public class TollStats {
    private String kkmc;          // 卡口名称
    private String xzqhmc;        // 行政区划名称
    private String windowStart;    // 窗口开始时间
    private String windowEnd;      // 窗口结束时间
    private long vehicleCount;     // 车辆数量
    private String fxlx;          // 主要方向类型
    // ... getters and setters
}
```

### 2. 配置类 (config/)

#### KafkaConfig.java - Kafka配置
- `createKafkaSource()`: 创建Kafka数据源
  - 配置服务器地址、主题、消费者组
  - 设置反序列化器

#### HBaseConfig.java - HBase配置
- `getHBaseConfig()`: 获取HBase配置
- `createConnection()`: 创建HBase连接

### 3. 算子类 (operator/)

#### TollStatsWindowFunction.java - 窗口统计处理
主要功能：
- 按卡口分组统计车流量
- 统计各方向车流量
- 确定主要车流方向
- 生成统计结果

关键方法：
```java
public void process(String kkmc,
                   Context context,
                   Iterable<TollEvent> events,
                   Collector<TollStats> out) {
    // 统计逻辑
}
```

#### TrafficPatternProcessor.java - 交通模式处理
主要功能：
- 定义交通异常模式
- 生成预警消息
- 判断预警触发条件

关键方法：
```java
public static Pattern<TollEvent, TollEvent> createTrafficPattern()
public static boolean shouldTriggerAlert(long currentCount, long averageCount)
public static String createAlertMessage(TollEvent event)
```

### 4. Sink类 (sink/)

#### HBaseSinkFunction.java - HBase数据写入
主要功能：
- 建立HBase连接
- 写入统计数据
- 管理连接生命周期

关键方法：
```java
public void invoke(TollStats record, Context context)
```

### 5. 工具类 (utils/)

#### TimeUtils.java - 时间处理工具
主要功能：
- 时间格式转换
- 时间计算
- 时间范围判断

关键方法：
```java
public static Instant parseDateTime(String dateTimeStr)
public static String formatDateTime(Instant instant)
public static String formatWindowTime(long windowTime)
public static long getMinutesBetween(long timestamp1, long timestamp2)
```

### 6. 主程序 (TollTrafficAnalysis.java)

主要功能：
1. 配置Flink执行环境
2. 创建Kafka数据源
3. 数据转换和处理
4. 设置时间窗口
5. 配置CEP模式匹配
6. 输出结果到HBase

关键代码段：
```java
// 1. 实时统计
DataStream<TollStats> statistics = events
        .keyBy(TollEvent::getKkmc)
        .window(TumblingEventTimeWindows.of(Time.minutes(1)))
        .process(new TollStatsWindowFunction());

// 2. CEP模式检测
PatternStream<TollEvent> patternStream = CEP.pattern(
        events.keyBy(TollEvent::getKkmc),
        TrafficPatternProcessor.createTrafficPattern()
);
```

## 接口说明

### Kafka数据接入接口

1. **Topic**: `toll-events`

2. **消息格式**: CSV格式字符串，字段间用逗号分隔
   ```
   GCXH,XZQHMC,KKMC,FXLX,GCSJ,HPZL,HP,CLPPXH
   ```

3. **字段说明**:
   - `GCXH`: 过车序号 (字符串)
   - `XZQHMC`: 行政区划名称 (字符串)
   - `KKMC`: 卡口名称 (字符串)
   - `FXLX`: 方向类型 (字符串)
   - `GCSJ`: 过车时间 (格式：yyyy-MM-dd HH:mm:ss)
   - `HPZL`: 号牌种类 (字符串)
   - `HP`: 号牌 (字符串)
   - `CLPPXH`: 车辆品牌型号 (字符串)

### HBase存储接口

1. **表名**: `toll_stats`

2. **列族**: `stats`

3. **行键设计**: `kkmc_windowStart`
   - 示例：`西湖卡口_1703062500000`

4. **列定义**:
   ```
   stats:kkmc           - 卡口名称
   stats:xzqhmc         - 行政区划名称
   stats:window_start   - 窗口开始时间
   stats:window_end     - 窗口结束时间
   stats:vehicle_count  - 车辆数量
   stats:fxlx          - 主要方向类型
   ```

## 快速开始

1. 环境要求
   - Java 8+
   - Maven 3.6+
   - Kafka
   - HBase

2. 构建项目
```bash
mvn clean package
```

3. 运行项目
```bash
java -jar target/toll-traffic-analysis-1.0-SNAPSHOT.jar
```

## 配置说明

1. Kafka配置
   - 主题：toll-events
   - 数据格式：CSV (见上述字段说明)

2. HBase配置
   - 表名：toll_stats
   - 列族：stats

## 监控和维护

- 使用Flink Web UI监控作业状态
- 通过log4j日志系统记录运行信息
- 支持作业检查点和状态恢复