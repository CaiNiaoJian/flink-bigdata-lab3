# ZooKeeper 配置与操作指南

## 一、ZooKeeper简介

### 1. 功能定位
- 分布式协调服务
- 配置管理中心
- 集群管理
- 分布式锁服务
- 命名服务

### 2. 在本项目中的作用
1. **Kafka集群管理**
   - Broker注册与发现
   - Topic配置管理
   - 消费者组协调

2. **HBase集群管理**
   - Region服务器注册
   - Master选举
   - 元数据管理

## 二、安装与配置

### 1. 单机模式安装

1. **下载和解压**
```bash
# 下载ZooKeeper
wget https://downloads.apache.org/zookeeper/zookeeper-3.7.1/apache-zookeeper-3.7.1-bin.tar.gz

# 解压
tar -xzvf apache-zookeeper-3.7.1-bin.tar.gz

# 移动到指定目录
mv apache-zookeeper-3.7.1-bin /usr/local/zookeeper
```

2. **配置环境变量**
```bash
vim ~/.bashrc

# 添加以下内容
export ZOOKEEPER_HOME=/usr/local/zookeeper
export PATH=$PATH:$ZOOKEEPER_HOME/bin

# 使配置生效
source ~/.bashrc
```

3. **创建数据和日志目录**
```bash
# 创建数据目录
mkdir -p /data/zookeeper/data

# 创建日志目录
mkdir -p /data/zookeeper/logs
```

4. **配置文件设置**
```bash
# 复制配置文件模板
cp $ZOOKEEPER_HOME/conf/zoo_sample.cfg $ZOOKEEPER_HOME/conf/zoo.cfg

# 编辑配置文件
vim $ZOOKEEPER_HOME/conf/zoo.cfg
```

基本配置内容：
```properties
# 基本配置
tickTime=2000
initLimit=10
syncLimit=5
dataDir=/data/zookeeper/data
dataLogDir=/data/zookeeper/logs
clientPort=2181

# 最大客户端连接数
maxClientCnxns=60

# 自动清理快照和事务日志
autopurge.snapRetainCount=3
autopurge.purgeInterval=1

# 管理端口
admin.serverPort=8080
```

### 2. 集群模式配置

1. **修改zoo.cfg添加集群配置**
```properties
# 集群配置
server.1=zoo1:2888:3888
server.2=zoo2:2888:3888
server.3=zoo3:2888:3888
```

2. **创建myid文件**
```bash
# 在每个节点的数据目录下创建myid文件
echo "1" > /data/zookeeper/data/myid  # 第一个节点
echo "2" > /data/zookeeper/data/myid  # 第二个节点
echo "3" > /data/zookeeper/data/myid  # 第三个节点
```

3. **配置hosts文件**
```bash
vim /etc/hosts

# 添加以下内容（请使用实际操作节点）具体使用学校给的节点还是自己搭建的节点
192.168.1.101 zoo1
192.168.1.102 zoo2
192.168.1.103 zoo3
```

### 3. 安全配置

1. **启用访问控制**
```properties
# 在zoo.cfg中添加
authProvider.1=org.apache.zookeeper.server.auth.SASLAuthenticationProvider
requireClientAuthScheme=sasl
```

2. **配置JAAS文件**
```bash
vim $ZOOKEEPER_HOME/conf/jaas.conf
```

```
Server {
    org.apache.zookeeper.server.auth.DigestLoginModule required
    user_super="admin"
    user_kafka="kafka-password"
    user_hbase="hbase-password";
};

Client {
    org.apache.zookeeper.server.auth.DigestLoginModule required
    username="admin"
    password="admin-secret";
};
```

## 三、运维操作

### 1. 服务管理命令

1. **启动服务**
```bash
# 前台启动
zkServer.sh start-foreground

# 后台启动
zkServer.sh start

# 指定配置文件启动
zkServer.sh start /path/to/zoo.cfg
```

2. **停止服务**
```bash
zkServer.sh stop
```

3. **状态查看**
```bash
zkServer.sh status
```

4. **重启服务**
```bash
zkServer.sh restart
```

### 2. 客户端操作

1. **连接服务器**
```bash
zkCli.sh -server localhost:2181
```

2. **基本操作命令**
```bash
# 创建节点
create /mynode mydata

# 获取节点数据
get /mynode

# 设置节点数据
set /mynode newdata

# 删除节点
delete /mynode

# 列出子节点
ls /

# 查看节点状态
stat /mynode
```

3. **监控命令**
```bash
# 设置监控
get /mynode watch

# 查看配额
listquota /mynode

# 设置配额
setquota -n 10 /mynode
```

### 3. 日志管理

1. **日志配置**
```bash
# 编辑log4j配置
vim $ZOOKEEPER_HOME/conf/log4j.properties
```

```properties
# 日志级别和输出配置
zookeeper.root.logger=INFO, CONSOLE, ROLLINGFILE
zookeeper.console.threshold=INFO

# 日志文件配置
zookeeper.log.dir=/data/zookeeper/logs
zookeeper.log.file=zookeeper.log
zookeeper.log.threshold=INFO
zookeeper.log.maxfilesize=256MB
zookeeper.log.maxbackupindex=10
```

2. **日志查看命令**
```bash
# 查看最新日志
tail -f /data/zookeeper/logs/zookeeper.log

# 查看错误日志
grep ERROR /data/zookeeper/logs/zookeeper.log

# 统计连接数
grep "Established session" /data/zookeeper/logs/zookeeper.log | wc -l
```

## 四、监控与维护

### 1. 四字命令

```bash
# 查看服务器状态
echo stat | nc localhost 2181

# 查看性能统计
echo mntr | nc localhost 2181

# 查看连接详情
echo cons | nc localhost 2181

# 查看服务器配置
echo conf | nc localhost 2181

# 查看是否正在进行选举
echo isro | nc localhost 2181
```

### 2. JMX监控

1. **启用JMX**
```bash
# 在zkServer.sh中添加
export JMXPORT=9999
export JMXAUTH=false
export JMXSSL=false
```

2. **使用JConsole连接**
```bash
jconsole localhost:9999
```

### 3. 性能优化

1. **JVM配置**
```bash
# 在zkServer.sh中添加
export JVMFLAGS="-Xms2g -Xmx2g -XX:+UseG1GC"
```

2. **系统参数优化**
```bash
# 编辑系统限���
vim /etc/security/limits.conf

# 添加以下内容
zookeeper soft nofile 65535
zookeeper hard nofile 65535
```

### 4. 备份与恢复

1. **数据备份**
```bash
# 停止服务
zkServer.sh stop

# 备份数据目录
tar -czf zk-backup-$(date +%Y%m%d).tar.gz /data/zookeeper/data

# 启动服务
zkServer.sh start
```

2. **数据恢复**
```bash
# 停止服务
zkServer.sh stop

# 清理当前数据
rm -rf /data/zookeeper/data/*

# 恢复备份
tar -xzf zk-backup-20231218.tar.gz -C /

# 启动服务
zkServer.sh start
```

## 五、故障处理

### 1. 常见问题

1. **连接问题**
```bash
# 检查网络连接
telnet localhost 2181

# 检查防火墙
systemctl status firewalld
```

2. **内存问题**
```bash
# 检查内存使用
jmap -heap $(pgrep -f QuorumPeerMain)

# 查看GC日志
jstat -gcutil $(pgrep -f QuorumPeerMain) 1000
```

3. **集群问题**
```bash
# 检查集群状态
echo stat | nc localhost 2181 | grep Mode

# 检查选举状态
echo isro | nc localhost 2181
```

### 2. 恢复措施

1. **数据修复**
```bash
# 检查数据一致性
zkCli.sh -server localhost:2181 get /zookeeper/config

# 强制同步
zkServer.sh start-foreground
```

2. **重建节点**
```bash
# 清理节点
rm -rf /data/zookeeper/data/version-2

# 初始化新节点
zkServer.sh init
```

## 六、最佳实践

1. **配置建议**
   - 使用独立的数据和日志目录
   - 启用自动清理
   - 配置合适的JVM参数
   - 使用监控和告警

2. **安全建议**
   - 启用访问控制
   - 限制客户端连接数
   - 定期更换密码
   - 及时更新版本

3. **运维建议**
   - 定期备份数据
   - 监控系统资源
   - 保持日志轮转
   - 建立应急预案 