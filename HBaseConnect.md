# HBase 连接与操作指南

## 一、HBase连接原理

### 1. 连接架构
```
Flink Application -> HBase Client -> ZooKeeper -> HBase RegionServers
```

### 2. 连接流程
1. **ZooKeeper协调**
   - HBase使用ZooKeeper维护集群状态
   - 客户端首先连接ZooKeeper获取RegionServer信息
   - ZooKeeper负责跟踪RegionServer的存活状态

2. **连接建立过程**
   ```
   1. 初始化HBase配置
   2. 通过ZooKeeper获取RegionServer位置
   3. 建立与RegionServer的连接
   4. 维护连接池
   ```

### 3. 程序中的实现
```java
// 1. 配置类
public class HBaseConfig {
    public static Configuration getHBaseConfig() {
        Configuration config = HBaseConfiguration.create();
        config.set("hbase.zookeeper.quorum", "localhost");
        config.set("hbase.zookeeper.property.clientPort", "2181");
        return config;
    }
}

// 2. 连接管理
public class HBaseSinkFunction extends RichSinkFunction<TollStats> {
    private Connection connection;
    
    @Override
    public void open(Configuration parameters) throws Exception {
        connection = ConnectionFactory.createConnection(HBaseConfig.getHBaseConfig());
    }
}
```

## 二、HBase表结构设计

### 1. 表创建
```sql
create 'toll_stats', 'stats'
```

### 2. 列族设计
- 列族名：`stats`
- 列：
  ```
  stats:kkmc           - 卡口名称
  stats:xzqhmc         - 行政区划名称
  stats:window_start   - 窗口开始时间
  stats:window_end     - 窗口结束时间
  stats:vehicle_count  - 车辆数量
  stats:fxlx          - 主要方向类型
  ```

### 3. RowKey设计
- 格式：`卡口名称_时间窗口开始时间`
- 示例：`西湖卡口_20231218101500`
- 设计原理：支持按卡口和时间范围快速查询

## 三、Linux环境配置与操作

### 1. HBase环境配置

1. **下载和解压**
```bash
wget https://downloads.apache.org/hbase/2.4.12/hbase-2.4.12-bin.tar.gz
tar -xzvf hbase-2.4.12-bin.tar.gz
mv hbase-2.4.12 /usr/local/hbase
```

2. **配置环境变量**
```bash
vim ~/.bashrc

# 添加以下内容
export HBASE_HOME=/usr/local/hbase
export PATH=$PATH:$HBASE_HOME/bin

# 使配置生效
source ~/.bashrc
```

3. **修改HBase配置**
```bash
cd $HBASE_HOME/conf
vim hbase-site.xml
```

添加配置：
```xml
<configuration>
    <property>
        <name>hbase.rootdir</name>
        <value>file:///home/hadoop/hbase</value>
    </property>
    <property>
        <name>hbase.zookeeper.property.dataDir</name>
        <value>/home/hadoop/zookeeper</value>
    </property>
</configuration>
```

### 2. HBase常用命令

1. **启动和停止**
```bash
# 启动HBase
start-hbase.sh

# 停止HBase
stop-hbase.sh

# 检查状态
hbase-daemon.sh status
```

2. **HBase Shell操作**
```bash
# 进入HBase Shell
hbase shell

# 创建表
create 'toll_stats', 'stats'

# 查看表结构
describe 'toll_stats'

# 查看表数据
scan 'toll_stats'

# 按RowKey查询
get 'toll_stats', '西湖卡口_20231218101500'

# 删除表
disable 'toll_stats'
drop 'toll_stats'
```

3. **数据导出**
```bash
# 导出到CSV
hbase org.apache.hadoop.hbase.mapreduce.Export toll_stats /output/path

# 使用自定义脚本导出
echo "scan 'toll_stats'" | hbase shell > toll_stats_export.txt
```

### 3. 故障排查

1. **检查连接**
```bash
# 检查ZooKeeper状态
echo stat | nc localhost 2181

# 检查HBase进程
jps | grep HBase

# 查看HBase日志
tail -f $HBASE_HOME/logs/hbase-*-master-*.log
```

2. **常见问题解决**
```bash
# 清理缓存
rm -rf /tmp/hbase-*

# 重启服务
stop-hbase.sh
start-hbase.sh

# 检查防火墙
systemctl status firewalld
```

## 四、数据查询示例

### 1. 基本查询
```sql
# 查询特定卡口的数据
scan 'toll_stats', {FILTER => "PrefixFilter('西湖卡口')"}

# 查询时间范围
scan 'toll_stats', {TIMERANGE => [1703062500000, 1703062560000]}
```

### 2. 复杂查询
```sql
# 组合条件查询
scan 'toll_stats', {
  FILTER => "SingleColumnValueFilter('stats', 'vehicle_count', >, 'binary:100')"
}
```

### 3. 导出查询结果
```bash
# 导出到文件
echo "scan 'toll_stats', {COLUMNS => ['stats:vehicle_count', 'stats:fxlx']}" | \
hbase shell > traffic_stats.txt
```

## 五、性能优化建议

1. **客户端优化**
   - 使用连接池
   - 批量写入
   - 适当的Scanner缓存

2. **表结构优化**
   - 合理的RowKey设计
   - 适当的列族数量
   - 压缩算法选择

3. **服务端优化**
   - 内存配置
   - Region大小
   - 预分区设计 