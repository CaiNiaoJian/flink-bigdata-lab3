# Hadoop 与 HBase 集群配置指南（一主两从）

## 一、环境准备

### 1. 系统要求
- 操作系统：CentOS 7+ / Ubuntu 18.04+
- JDK版本：Oracle JDK 1.8+
- 内存要求：每个节点最小8GB，推荐16GB以上
- 磁盘空间：系统盘50GB以上，数据盘100GB以上
- 网络：所有节点之间网络互通，延迟低于1ms

### 2. 节点规划
| 节点角色 | 主机名 | IP地址 | 服务 |
|---------|--------|--------|------|
| Master  | master | 192.168.1.100 | NameNode, ResourceManager, HMaster, ZooKeeper |
| Slave1  | slave1 | 192.168.1.101 | DataNode, NodeManager, RegionServer, ZooKeeper |
| Slave2  | slave2 | 192.168.1.102 | DataNode, NodeManager, RegionServer, ZooKeeper |

### 3. 基础环境配置

1. **在所有节点配置hosts**
```bash
vim /etc/hosts

# 添加以下内容
192.168.1.100 master
192.168.1.101 slave1
192.168.1.102 slave2
```

2. **在所有节点配置SSH免密登录**
```bash
# 在master节点执行
ssh-keygen -t rsa -P ""
ssh-copy-id -i ~/.ssh/id_rsa.pub hadoop@master
ssh-copy-id -i ~/.ssh/id_rsa.pub hadoop@slave1
ssh-copy-id -i ~/.ssh/id_rsa.pub hadoop@slave2

# ��试连接
ssh hadoop@slave1
ssh hadoop@slave2
```

## 二、Hadoop集群配置

### 1. 核心配置文件（master节点）

1. **core-site.xml**
```xml
<configuration>
    <property>
        <name>fs.defaultFS</name>
        <value>hdfs://master:9000</value>
    </property>
    <property>
        <name>hadoop.tmp.dir</name>
        <value>/data/hadoop/tmp</value>
    </property>
</configuration>
```

2. **hdfs-site.xml**
```xml
<configuration>
    <property>
        <name>dfs.replication</name>
        <value>2</value>
    </property>
    <property>
        <name>dfs.namenode.name.dir</name>
        <value>/data/hadoop/namenode</value>
    </property>
    <property>
        <name>dfs.datanode.data.dir</name>
        <value>/data/hadoop/datanode</value>
    </property>
    <property>
        <name>dfs.namenode.secondary.http-address</name>
        <value>master:50090</value>
    </property>
</configuration>
```

3. **yarn-site.xml**
```xml
<configuration>
    <property>
        <name>yarn.resourcemanager.hostname</name>
        <value>master</value>
    </property>
    <property>
        <name>yarn.nodemanager.aux-services</name>
        <value>mapreduce_shuffle</value>
    </property>
    <property>
        <name>yarn.resourcemanager.webapp.address</name>
        <value>master:8088</value>
    </property>
</configuration>
```

4. **mapred-site.xml**
```xml
<configuration>
    <property>
        <name>mapreduce.framework.name</name>
        <value>yarn</value>
    </property>
    <property>
        <name>mapreduce.jobhistory.address</name>
        <value>master:10020</value>
    </property>
    <property>
        <name>mapreduce.jobhistory.webapp.address</name>
        <value>master:19888</value>
    </property>
</configuration>
```

5. **workers文件**
```bash
# 编辑workers文件
vim $HADOOP_HOME/etc/hadoop/workers

# 添加以下内容
slave1
slave2
```

### 2. 配置文件分发
```bash
# 在master节点执行，将配置文件分发到从节点
for node in slave1 slave2; do
    scp -r $HADOOP_HOME/etc/hadoop/* $node:$HADOOP_HOME/etc/hadoop/
done
```

## 三、HBase集群配置

### 1. HBase配置文件（master节点）

1. **hbase-site.xml**
```xml
<configuration>
    <property>
        <name>hbase.rootdir</name>
        <value>hdfs://master:9000/hbase</value>
    </property>
    <property>
        <name>hbase.cluster.distributed</name>
        <value>true</value>
    </property>
    <property>
        <name>hbase.zookeeper.quorum</name>
        <value>master,slave1,slave2</value>
    </property>
    <property>
        <name>hbase.zookeeper.property.dataDir</name>
        <value>/data/zookeeper</value>
    </property>
    <property>
        <name>hbase.master.hostname</name>
        <value>master</value>
    </property>
    <property>
        <name>hbase.regionserver.hostname</name>
        <value>master</value>
    </property>
</configuration>
```

2. **regionservers文件**
```bash
# 编辑regionservers文件
vim $HBASE_HOME/conf/regionservers

# 添加以下内容
slave1
slave2
```

### 2. 配置文件分发
```bash
# 在master节点执行，将配置文件分发到从节点
for node in slave1 slave2; do
    scp -r $HBASE_HOME/conf/* $node:$HBASE_HOME/conf/
done
```

## 四、启动顺序与验证

### 1. 启动集群
```bash
# 1. 在所有节点启动ZooKeeper
zkServer.sh start

# 2. 在master节点格式化HDFS（仅第一次）
hdfs namenode -format

# 3. 在master节点启动HDFS
start-dfs.sh

# 4. 在master节点启动YARN
start-yarn.sh

# 5. 在master节点启动HBase
start-hbase.sh
```

### 2. 验证集群状态

1. **验证HDFS**
```bash
# 查看HDFS节点状态
hdfs dfsadmin -report

# 预期输出应显示：
# - 1个活跃的NameNode（master）
# - 2个活跃的DataNode（slave1, slave2）
```

2. **验证YARN**
```bash
# 查看YARN节点状态
yarn node -list -all

# 预期输出应显示：
# - ResourceManager在master节点
# - 2个活跃的NodeManager（slave1, slave2）
```

3. **验证HBase**
```bash
# 进入HBase Shell
hbase shell

# 检查节点状态
status 'detailed'

# 预期输出应显示：
# - HMaster在master节点
# - 2个活跃的RegionServer（slave1, slave2）
```

### 3. Web界面访问
- HDFS管理界面: http://master:9870
- YARN管理界面: http://master:8088
- HBase管理界面: http://master:16010

### 4. 常见配置问题与解决方案

1. **环境变量配置问题**
```bash
# 问题：找不到hadoop或hbase命令
# 解决：检查环境变量配置
echo $HADOOP_HOME
echo $HBASE_HOME
echo $PATH

# 正确配置方法
vim ~/.bashrc
export HADOOP_HOME=/usr/local/hadoop
export HBASE_HOME=/usr/local/hbase
export PATH=$PATH:$HADOOP_HOME/bin:$HADOOP_HOME/sbin:$HBASE_HOME/bin
source ~/.bashrc
```

2. **节点间通信问题**
```bash
# 问题：节点之间无法通信
# 解决：检查hosts配置和SSH设置
# 1. 检查hosts文件
cat /etc/hosts

# 2. 测试节点间连通性
ping slave1
ping slave2

# 3. 验证SSH免密登录
ssh slave1 'date'
ssh slave2 'date'
```

3. **权限问题**
```bash
# 问题：无法创建目录或写入文件
# 解决：检查目录权限
# 1. 检查HDFS目录权限
hdfs dfs -ls /
hdfs dfs -chmod -R 755 /path/to/dir
hdfs dfs -chown -R hadoop:hadoop /path/to/dir

# 2. 检查本地目录权限
ls -l /data/hadoop
ls -l /data/hbase
sudo chown -R hadoop:hadoop /data/hadoop
sudo chown -R hadoop:hadoop /data/hbase
```

4. **端口占用问题**
```bash
# 问题：服务无法启动，端口被占用
# 解决：检查端口占用情况
# 1. 检查端口占用
netstat -tulpn | grep 9000
netstat -tulpn | grep 16000

# 2. 结束占用进程
kill -9 <pid>

# 3. 验证端口是否释放
netstat -tulpn | grep <port>
```

5. **内存配置问题**
```bash
# 问题：JVM内存不足或配置不当
# 解决：调整内存配置
# 1. 修改Hadoop内存配置
vim $HADOOP_HOME/etc/hadoop/hadoop-env.sh
export HADOOP_HEAPSIZE=4096

# 2. 修改HBase内存配置
vim $HBASE_HOME/conf/hbase-env.sh
export HBASE_HEAPSIZE=4096
```

6. **日志问题**
```bash
# 问题：无法查看或写入日志
# 解决：检查日志配置和权限
# 1. 检查日志目录权限
ls -l $HADOOP_HOME/logs
ls -l $HBASE_HOME/logs

# 2. 检查日志级别配置
vim $HADOOP_HOME/etc/hadoop/log4j.properties
vim $HBASE_HOME/conf/log4j.properties

# 3. 清理旧日志
find $HADOOP_HOME/logs -name "*.log" -mtime +7 -exec rm {} \;
find $HBASE_HOME/logs -name "*.log" -mtime +7 -exec rm {} \;
```

7. **ZooKeeper连接问题**
```bash
# 问题：HBase无法连接ZooKeeper
# 解决：检查ZooKeeper配置和状态
# 1. 检查ZooKeeper状态
zkServer.sh status

# 2. 检查ZooKeeper连接
echo stat | nc localhost 2181

# 3. 检查HBase中的ZooKeeper配置
cat $HBASE_HOME/conf/hbase-site.xml | grep zookeeper
```

8. **数据目录问题**
```bash
# 问题：数据目录不存在或权限错误
# 解决：创建并设置正确权限
# 1. 创建必要目录
mkdir -p /data/hadoop/namenode
mkdir -p /data/hadoop/datanode
mkdir -p /data/hbase
mkdir -p /data/zookeeper

# 2. 设置权限
chown -R hadoop:hadoop /data/hadoop
chown -R hadoop:hadoop /data/hbase
chown -R hadoop:hadoop /data/zookeeper

# 3. 验证目录权限
ls -l /data
```

9. **配置文件同步问题**
```bash
# 问题：集群节点配置不一致
# 解决：同步配置文件
# 1. 从主节点同步到从节点
for node in slave1 slave2; do
    rsync -avz $HADOOP_HOME/etc/hadoop/ $node:$HADOOP_HOME/etc/hadoop/
    rsync -avz $HBASE_HOME/conf/ $node:$HBASE_HOME/conf/
done

# 2. 验证配置一致性
for node in slave1 slave2; do
    ssh $node "md5sum $HADOOP_HOME/etc/hadoop/*"
    ssh $node "md5sum $HBASE_HOME/conf/*"
done
```

10. **防火墙问题**
```bash
# 问题：节点间通信被阻断
# 解决：检查防火墙配置
# 1. 检查防火墙状态
systemctl status firewalld

# 2. 添加所需端口
firewall-cmd --permanent --add-port=9000/tcp
firewall-cmd --permanent --add-port=16000/tcp
firewall-cmd --permanent --add-port=16020/tcp
firewall-cmd --permanent --add-port=16030/tcp
firewall-cmd --permanent --add-port=2181/tcp
firewall-cmd --reload

# 3. 验证端口开放状态
firewall-cmd --list-ports
```

## 五、性能优化

### 1. HDFS优化
```xml
<!-- hdfs-site.xml -->
<property>
    <name>dfs.namenode.handler.count</name>
    <value>100</value>
</property>
<property>
    <name>dfs.datanode.handler.count</name>
    <value>50</value>
</property>
```

### 2. YARN优化
```xml
<!-- yarn-site.xml -->
<property>
    <name>yarn.nodemanager.resource.memory-mb</name>
    <value>16384</value>
</property>
<property>
    <name>yarn.scheduler.maximum-allocation-mb</name>
    <value>16384</value>
</property>
```

### 3. HBase优化
```xml
<!-- hbase-site.xml -->
<property>
    <name>hbase.regionserver.handler.count</name>
    <value>30</value>
</property>
<property>
    <name>hbase.hregion.max.filesize</name>
    <value>10737418240</value>
</property>
```

## 六、维护操作

### 1. 日常维护命令

1. **HDFS维护**
```bash
# 检查文件系统健康状态
hdfs fsck /

# 平衡数据块分布
start-balancer.sh

# 回收站清理
hdfs dfs -expunge
```

2. **HBase维护**
```bash
# 主要表压缩
major_compact 'tablename'

# 检查region分布
hbase hbck

# 备份数据
hbase org.apache.hadoop.hbase.backup.HBaseBackup
```

### 2. 日志管理
```bash
# Hadoop日志位置
$HADOOP_HOME/logs/

# HBase日志位置
$HBASE_HOME/logs/
```

### 3. 监控指标
1. **HDFS监控**
   - NameNode内存使用
   - DataNode磁盘使用
   - 数据块复制状态

2. **YARN监控**
   - 集群资源使用率
   - 应用运行状态
   - 容器分配情况

3. **HBase监控**
   - Region分布
   - 读写请求QPS
   - 压缩队列长度

## 七、故障处理

### 1. 常见问题处理

1. **HDFS问题**
```bash
# NameNode启动失败
# 检查日志
tail -f $HADOOP_HOME/logs/hadoop-hadoop-namenode-*.log

# 数据块丢失
hdfs fsck / | grep -v '^.'
```

2. **HBase问题**
```bash
# Region服务器无响应
hbase hbck -details

# 表不可用
hbase hbck -repair tablename
```

### 2. 恢复流程

1. **HDFS元数据恢复**
```bash
# 备份当前元数据
cp -r $dfs.namenode.name.dir/current /backup/namenode_metadata

# 使用备份恢复
rm -rf $dfs.namenode.name.dir/current/*
cp -r /backup/namenode_metadata/* $dfs.namenode.name.dir/current/
```

2. **HBase数据恢复**
```bash
# 导出表数据
hbase org.apache.hadoop.hbase.mapreduce.Export tablename /backup/tablename

# 导入表数据
hbase org.apache.hadoop.hbase.mapreduce.Import tablename /backup/tablename
```

## 八、安全配置

### 1. 权限配置

1. **HDFS权限**
```xml
<!-- core-site.xml -->
<property>
    <name>hadoop.security.authentication</name>
    <value>kerberos</value>
</property>
```

2. **HBase权限**
```xml
<!-- hbase-site.xml -->
<property>
    <name>hbase.security.authentication</name>
    <value>kerberos</value>
</property>
<property>
    <name>hbase.security.authorization</name>
    <value>true</value>
</property>
```

### 2. 防火墙配置
```bash
# 开放必要端口
firewall-cmd --permanent --add-port=9000/tcp  # HDFS
firewall-cmd --permanent --add-port=16000/tcp # HBase
firewall-cmd --permanent --add-port=16010/tcp # HBase Web
firewall-cmd --permanent --add-port=16020/tcp # HBase Region
firewall-cmd --permanent --add-port=16030/tcp # HBase Region Web
firewall-cmd --reload
```

## 九、备份策略

### 1. HDFS备份
```bash
# 创建快照
hdfs dfsadmin -allowSnapshot /path
hdfs dfs -createSnapshot /path snapshot_name

# 导出数据
hadoop distcp hdfs://source:9000/path hdfs://backup:9000/path
```

### 2. HBase备份
```bash
# 完整备份
hbase org.apache.hadoop.hbase.backup.HBaseBackup FULL /backup/path table1 table2

# 增量备份
hbase org.apache.hadoop.hbase.backup.HBaseBackup INCREMENTAL /backup/path table1 table2
```

## 十、最佳实践

1. **系统配置**
   - 使用SSD作为系统盘
   - 关闭系统交换分区
   - 调整文件描述符限制

2. **数据存储**
   - 使用RAID配置数据盘
   - 定期进行数据备份
   - 监控磁盘使用率

3. **性能优化**
   - 合理设置内存分配
   - 优化GC参数
   - 定期压缩表 

   