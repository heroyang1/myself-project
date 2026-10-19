# OSH Backend 本地开发环境搭建指南

> **⚠️ 本文档仅适用于本地开发调试。生产/测试环境部署必须通过 CI/CD 流水线。**

---

## 一、环境总览

| 组件 | 要求 | 本地状态 | 必需 |
|------|------|---------|------|
| JDK | 1.8 (Corretto 推荐) | ✅ `C:\Users\Admin1\.jdks\corretto-1.8.0_492` | ✅ |
| JDK 17 | Kafka 4.x 运行时需要 | ✅ `C:\middleware\jdk17\jdk17.0.20_12` | ⚡ Kafka 依赖 |
| Maven | 3.8.4+ | ✅ IDEA 内置 `mvn.cmd` | ✅ |
| MySQL | 8.0+ | ✅ `localhost:3306` | ✅ |
| Redis | 3.0+ | ✅ `localhost:6379` | ✅ |
| Elasticsearch | 7.17.x | ✅ `localhost:9200`（单节点，已安装） | ⚡ 可降级 |
| Kafka | 4.x KRaft | ✅ `localhost:9092`（已安装） | ⚡ 可禁用 |
| XXL-Job Admin | 2.4.0 | ✅ `localhost:8888`（已安装） | ⚡ 可降级 |
| Nacos | 2.0.x~2.1.x | ❌ 远程不可达 / 本地未安装 | ⚡ 可绕过 |
| Docker | 任意 | ❌ 未安装 | 可选 |

---

## 二、快速启动方案（绕过 Nacos）

本方案无需安装 Nacos，直接使用本地配置文件启动，适合快速验证核心功能。

### 2.1 已创建的配置文件

```
backstage-admin/src/main/resources/
├── bootstrap.yml          # 原 dev 环境（连接远程 Nacos）
├── bootstrap-local.yml    # ✨ 本地环境（Nacos config/discovery 已禁用）
├── application-local.yml  # ✨ 本地最小化完整配置
```

### 2.2 启动前必须确认的配置

编辑 `application-local.yml`，**至少修改以下几项**：

```yaml
# 1. MySQL 密码（当前为占位值）
spring.datasource.druid.master.password: <你的本地MySQL密码>

# 2. MySQL 数据库名（确认本地库名，默认假设 ry-vue）
spring.datasource.druid.master.url: jdbc:mysql://localhost:3306/<你的数据库名>?...

# 3. Redis 密码（如本地 Redis 设置了密码）
spring.redis.password: <你的Redis密码>

# 4. 文件上传路径（确保目录存在或改为本地路径）
ruoyi.profile: C:/osh/upload
```

### 2.3 启动命令（PowerShell）

```powershell
# 设置 JDK 1.8
$env:JAVA_HOME = 'C:\Users\Admin1\.jdks\corretto-1.8.0_492'
$env:Path = $env:JAVA_HOME + '\bin;' + $env:Path

# Maven 路径（IDEA 内置）
$mvn = 'C:\Users\Admin1\code\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd'

# 编译（首次或代码变更后执行）
& $mvn clean install -DskipTests

# 启动（JVM 参数参考 ry.sh）
java '-Dname=backstage-admin.jar' `
     '-Duser.timezone=Asia/Shanghai' `
     '-Xms512m' '-Xmx1024m' `
     '-XX:MetaspaceSize=128m' '-XX:MaxMetaspaceSize=512m' `
     '-XX:+HeapDumpOnOutOfMemoryError' `
     '-XX:+PrintGCDateStamps' '-XX:+PrintGCDetails' `
     '-XX:NewRatio=1' '-XX:SurvivorRatio=30' `
     '-XX:+UseParallelGC' '-XX:+UseParallelOldGC' `
     -jar backstage-admin\target\backstage-admin.jar `
     '--spring.profiles.active=local'
```

### 2.4 IDEA 中启动（推荐）

1. **Run/Debug Configurations** → **Spring Boot** → 新建配置
2. **Main class**: `com.backstage.RuoYiApplication`
3. **Active profiles**: `local`
4. **VM options**:
   ```
   -Xms512m -Xmx1024m
   -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=512m
   -XX:+HeapDumpOnOutOfMemoryError
   -Duser.timezone=Asia/Shanghai
   ```
5. **JRE**: 选择 `corretto-1.8.0_492`
6. 点击 **Run** 或 **Debug**

---

## 三、本地中间件安装（ES / Kafka / XXL-Job）

> 以下中间件均已在本机安装并验证通过，安装路径统一在 `C:\middleware\` 下。

### 3.1 Elasticsearch 7.17.25

```
安装路径：C:\middleware\elasticsearch\elasticsearch-7.17.25\
数据目录：C:\middleware\elasticsearch\elasticsearch-7.17.25\data\
端口：9200（HTTP）
```

**关键配置**（`config/elasticsearch.yml`）：

```yaml
cluster.name: osh-local-dev
node.name: local-node-1
path.data: C:/middleware/elasticsearch/elasticsearch-7.17.25/data
path.logs: C:/middleware/elasticsearch/elasticsearch-7.17.25/logs
network.host: 0.0.0.0
discovery.type: single-node
xpack.security.enabled: false
```

**JVM 堆内存**（`config/jvm.options.d/heap.options`）：

```
-Xms2g
-Xmx2g
```

> ⚠️ **不要** 设置 `xpack.security.enrollment.enabled`，这是 ES 8.x 配置项，7.17.x 不识别会报错。

**启动**：

```bat
C:\middleware\elasticsearch\elasticsearch-7.17.25\bin\elasticsearch.bat
```

**验证**：

```powershell
Invoke-WebRequest -Uri http://localhost:9200/_cluster/health?pretty -UseBasicParsing
# 期望返回："status" : "green", "cluster_name" : "osh-local-dev"
```

### 3.2 Kafka 4.3.1（KRaft 模式，免 Zookeeper）

```
安装路径：C:\middleware\kafka\kafka_2.13-4.3.1\
JDK 17：C:\middleware\jdk17\jdk17.0.20_12\（Kafka 4.x 强制要求 JDK 17+）
数据目录：C:\middleware\kafka\data\
端口：9092（PLAINTEXT）、9093（CONTROLLER）
```

**关键配置**（`config/server.properties`）：

```properties
process.roles=broker,controller
node.id=1
controller.quorum.voters=1@localhost:9093
listeners=PLAINTEXT://:9092,CONTROLLER://:9093
log.dirs=C:/middleware/kafka/data
```

> ⚠️ **Kafka 4.x 在 Windows 上的已知 bug**：`kafka-run-class.bat` 中 `KAFKA_LOG4J_OPTS` 变量引号嵌套错误，导致 `kafka-storage.bat format` 和 `kafka-server-start.bat` 均报 "命令语法不正确"。

**解决方案**：使用自定义启动脚本 `C:\middleware\kafka\run-kafka.bat`，直接调用 Java 主类绕过 bat 文件：

```bat
@echo off
"C:\middleware\jdk17\jdk17.0.20_12\bin\java.exe" -Xmx512M -cp "...完整classpath..." ^
  -Dlog4j2.configurationFile="file:.../log4j2.yaml" ^
  -Dkafka.logs.dir=".../logs" ^
  kafka.Kafka ".../config/server.properties"
```

**首次初始化**（格式化 KRaft 存储）：

```bat
C:\middleware\jdk17\jdk17.0.20_12\bin\java.exe -cp libs/* ^
  kafka.tools.StorageTool format --standalone ^
  -t <随机UUID> -c config/server.properties
```

### 3.3 XXL-Job Admin 2.4.0

```
源码路径：C:\middleware\xxl-job\xxl-job-2.4.0\
JAR 路径：C:\middleware\xxl-job\xxl-job-2.4.0\xxl-job-admin\target\xxl-job-admin-2.4.0.jar
数据库：xxl_job（独立库，8 张表）
端口：8888
默认账号：admin / 123456
```

**安装步骤**：

1. **获取源码**（GitHub 不可达时使用 Gitee 镜像）：
   ```bash
   git clone --branch 2.4.0 --depth 1 https://gitee.com/xuxueli0323/xxl-job.git
   ```

2. **初始化数据库**：
   ```powershell
   # 执行 doc/db/tables_xxl_job.sql（自动创建 xxl_job 库和 8 张表）
   # 注意：PowerShell 管道传递 SQL 可能编码丢失，建议用 Process 对象写入原始字节
   ```

3. **修改配置**（`xxl-job-admin/src/main/resources/application.properties`）：
   ```properties
   server.port=8888
   spring.datasource.url=jdbc:mysql://127.0.0.1:3306/xxl_job?useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&serverTimezone=Asia/Shanghai
   spring.datasource.username=root
   spring.datasource.password=root
   xxl.job.accessToken=default_token
   ```

4. **Maven 构建**（使用 JDK 1.8）：
   ```powershell
   & $mvn -f C:\middleware\xxl-job\xxl-job-2.4.0\pom.xml clean package -DskipTests -pl xxl-job-core,xxl-job-admin -am
   ```

5. **启动**：
   ```powershell
   & 'C:\Users\Admin1\.jdks\corretto-1.8.0_492\bin\java.exe' -jar C:\middleware\xxl-job\xxl-job-2.4.0\xxl-job-admin\target\xxl-job-admin-2.4.0.jar
   ```

6. **验证**：浏览器访问 `http://localhost:8888/xxl-job-admin`，使用 admin / 123456 登录

7. **添加执行器**：在"执行器管理"中添加 AppName=`osh-backend`，地址类型选"自动注册"

### 3.4 一键启动脚本

项目根目录提供 `start-all-middleware.bat`，在新窗口中同时启动全部中间件：

```bat
:: 1. Elasticsearch
start "Elasticsearch 7.17.25" /min cmd /c "C:\middleware\elasticsearch\elasticsearch-7.17.25\bin\elasticsearch.bat"

:: 2. Kafka（需 JDK 17）
start "Kafka 4.3.1" /min cmd /c "C:\middleware\kafka\run-kafka.bat"

:: 3. XXL-Job Admin（需 JDK 1.8）
start "XXL-Job Admin 2.4.0" /min cmd /c "C:\Users\Admin1\.jdks\corretto-1.8.0_492\bin\java.exe -jar C:\middleware\xxl-job\xxl-job-2.4.0\xxl-job-admin\target\xxl-job-admin-2.4.0.jar"
```

> **使用方法**：双击 `start-all-middleware.bat` 或在 PowerShell 中运行 `Start-Process .\start-all-middleware.bat`

---

## 四、本地 Nacos 安装方案（可选，完整本地环境）

如需完整复刻生产环境（包括配置中心热更新），需安装本地 Nacos。**大多数场景下，使用 `--spring.profiles.active=local` 即可，无需 Nacos。**

### 4.1 版本选择

项目使用 `spring-cloud-alibaba 2021.1`，兼容的 Nacos Server 版本：

| Nacos Server | 兼容性 | 推荐 |
|-------------|--------|------|
| 2.0.4 | ✅ 稳定 | ⭐ 推荐 |
| 2.1.2 | ✅ 稳定 | ⭐ 推荐 |
| 2.2.x+ | ⚠️ 部分 API 变更 | 不推荐 |
| 1.x | ❌ gRPC 协议不兼容 | 禁止 |

### 4.2 独立部署（Windows）

```powershell
# 1. 下载 Nacos Server 2.1.2
#    https://github.com/alibaba/nacos/releases/download/2.1.2/nacos-server-2.1.2.zip

# 2. 解压到本地目录（如 C:\nacos）
# 3. 启动（单机模式，使用内置 Derby 数据库）
cd C:\nacos\bin
.\startup.cmd -m standalone
```

如需自定义 JVM 参数，编辑 `C:\nacos\bin\startup.cmd`：

```bat
set "NACOS_JVM_OPTS=-Xms512m -Xmx1024m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m"
```

### 4.3 首次登录与初始化

1. 浏览器访问 `http://localhost:8848/nacos`
2. 默认账号：`nacos` / 密码：`nacos`
3. **首次登录后务必修改默认密码**
4. 创建命名空间（Namespace）：
   - 进入 **命名空间管理** → **新建命名空间**
   - 命名空间 ID：`local-dev`（自定义）
   - 命名空间名称：`本地开发`

### 4.4 从远程 Nacos 导出/导入配置

```powershell
# 导出远程 Nacos 配置（当远程可达时）
# 访问远程 Nacos 控制台 → 配置管理 → 配置列表
# 选择命名空间 d4208eb8-... → 导出配置（ZIP）

# 导入到本地 Nacos
# 访问 http://localhost:8848/nacos → 配置管理
# 选择 local-dev 命名空间 → 导入配置（上传 ZIP）
```

**若远程 Nacos 始终不可达**，需手动在本地 Nacos 创建以下 7 个配置文件：

| data-id | 内容摘要 |
|---------|---------|
| `osh-backend-mysql-dev.yaml` | MySQL 主从数据源、Druid 连接池 |
| `osh-backend-redis-dev.yaml` | Redis 连接、Lettuce 池 |
| `osh-backend-kafka-dev.yaml` | Kafka broker、consumer group |
| `osh-backend-mail-dev.yaml` | SMTP 邮件服务 |
| `osh-backend-elasticsearch-dev.yaml` | ES 集群地址、认证 |
| `osh-backend-jasypt-dev.yaml` | Jasypt 加密密钥 |
| `osh-backend-xxljob-dev.yaml` | XXL-Job admin 地址、executor |

### 4.5 切换到本地 Nacos

编辑 `bootstrap-local.yml`，取消注释并修改：

```yaml
spring:
  cloud:
    nacos:
      username: nacos
      password: nacos            # 修改为你设置的本地 Nacos 密码
      discovery:
        enabled: true
        server-addr: 127.0.0.1:8848
        namespace: local-dev
      config:
        enabled: true
        server-addr: 127.0.0.1:8848
        namespace: local-dev
        file-extension: yaml
        shared-configs:
          # ...取消注释所有 shared-configs...
```

---

## 五、中间件连通性测试

### 5.1 一键检测全部端口

```powershell
# 快速检测 6 个核心端口
@(3306, 6379, 9200, 9092, 8888, 8080) | ForEach-Object {
    $tcp = New-Object System.Net.Sockets.TcpClient
    try { $tcp.Connect('localhost', $_); Write-Host "OK $_"; $tcp.Close() }
    catch { Write-Host "DOWN $_" }
}
# 期望全部输出 OK
```

### 5.2 MySQL 数据库

```powershell
# 测试连接
Test-NetConnection -ComputerName localhost -Port 3306

# 命令行连接（需替换密码）
& 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe' -u root -p<密码> -e "SHOW DATABASES;"

# 常见问题排查
# ① Access denied → 检查密码，或使用 --skip-grant-tables 重置
# ② Connection refused → 检查 MySQL 服务：Get-Service MySQL80
# ③ Unknown database → 需初始化数据库，执行 sql/ 目录下的建表脚本
```

**推荐工具**：IntelliJ IDEA DataGrip、Navicat、DBeaver

### 5.3 Redis 缓存

```powershell
# 测试连接
Test-NetConnection -ComputerName localhost -Port 6379

# 命令行连接
redis-cli ping
# 期望返回：PONG

# 常见问题排查
# ① NOAUTH → Redis 设置了密码，在 application-local.yml 中配置 spring.redis.password
# ② Connection refused → 检查 Redis 服务是否运行
```

**推荐工具**：RedisInsight、Another Redis Desktop Manager

### 5.4 Elasticsearch（已安装）

```powershell
# 测试连接
Test-NetConnection -ComputerName localhost -Port 9200

# 集群健康状态
$wc = New-Object System.Net.WebClient
$wc.DownloadString('http://localhost:9200/_cluster/health?pretty')
# 期望："status" : "green", "cluster_name" : "osh-local-dev"

# 常见问题排查
# ① 端口未监听 → 运行 start-all-middleware.bat 或手动启动 elasticsearch.bat
# ② 启动报 unknown setting → 检查是否误加了 xpack.security.enrollment.enabled（8.x 配置项）
# ③ 内存占用过大 → 确认 config/jvm.options.d/heap.options 设置了 -Xms2g -Xmx2g
```

### 5.5 Kafka（已安装）

```powershell
# 测试连接
Test-NetConnection -ComputerName localhost -Port 9092

# application-local.yml 已配置：
#   spring.kafka.listener.auto-startup: true     → 自动启动监听器（Kafka 已本地安装）
#   spring.kafka.listener.missing-topics-fatal: false → topic 不存在不报错
# 效果：应用正常启动，Kafka 消费者自动连接本地 broker

# 常见问题排查
# ① "命令语法不正确" → 使用 C:\middleware\kafka\run-kafka.bat 替代官方 bat 文件
# ② 端口未监听 → 确认 JDK 17 路径正确，Kafka 4.x 强制要求 JDK 17+
```

### 5.6 XXL-Job Admin（已安装）

```powershell
# 测试 HTTP 访问
$wc = New-Object System.Net.WebClient
$wc.Headers.Add('Content-Type', 'application/x-www-form-urlencoded')
$wc.UploadString('http://localhost:8888/xxl-job-admin/login', 'userName=admin&password=123456')
# 期望返回：{"code":200,"msg":null,"content":null}

# 浏览器访问：http://localhost:8888/xxl-job-admin（admin / 123456）
# 在"执行器管理"中确认 osh-backend 执行器已自动注册
```

---

## 六、功能模块测试清单

服务启动成功后，按以下清单验证核心功能：

### 6.1 管理后台（/admin 路径）

| 功能 | 验证方式 | 预期结果 |
|------|---------|---------|
| 登录 | POST `/login` (admin/admin123) | 返回 JWT token |
| 用户列表 | GET `/system/user/list` | 返回用户分页数据 |
| 角色管理 | GET `/system/role/list` | 返回角色列表 |
| 菜单权限 | GET `/system/menu/list` | 返回菜单树 |
| 系统监控 | GET `/monitor/server` | 返回服务器信息 |
| Druid 监控 | 浏览器访问 `/druid` | 登录 ruoyi/123456 |
| Swagger | 浏览器访问 `/swagger-ui/` | API 文档页面 |

### 6.2 C 端接口（/osh 路径）

| 功能 | 依赖 | 本地可用 |
|------|------|----------|
| 课程列表 | MySQL | ✅ |
| 拼团活动 | MySQL + Redis | ✅ |
| 秒杀活动 | MySQL + Redis + Kafka | ✅（Kafka 已安装） |
| 文件上传 | 本地存储 | ✅（需建 profile 目录） |
| 搜索功能 | Elasticsearch | ✅（ES 已安装） |
| 定时任务 | XXL-Job Admin | ✅（Admin 已安装） |

### 6.3 功能受限时说明

> 以下功能需要额外配置或外部服务，本地中间件已覆盖大部分场景。

| 受限功能 | 原因 | 替代方案 |
|---------|------|----------|
| 邮件发送 | SMTP 未配置 | 填写真实 SMTP 配置 |
| Nacos 配置中心 | 本地未安装 | 使用 `application-local.yml` 即可 |
| XXL-Job 定时任务调度 | 需登录 Admin 手动创建任务 | 在 `http://localhost:8888/xxl-job-admin` 中添加 |

---

## 七、常见问题与排查

### Q1: 启动报 `NacosException: no available server`

**原因**：Nacos 连接超时  
**解决**：确认使用 `--spring.profiles.active=local`，且 `bootstrap-local.yml` 中 config.enabled=false

### Q1.5: 启动报 `ERR Client sent AUTH, but no password is set`（Redisson）

**原因**：本地 Redis 无密码，但 `application-local.yml` 中 `spring.redis.password` 字段存在（即使为空），导致 Redisson 发送 AUTH 命令  
**解决**：将 `spring.redis.password` 整行注释掉（已在当前版本修复）

### Q2: 启动报 `Access denied for user 'root'@'localhost'`

**原因**：`application-local.yml` 中的 MySQL 密码与实际不匹配  
**解决**：

**方法 A：修改配置文件密码**
```yaml
spring.datasource.druid.master.password: <你的真实密码>
```

**方法 B：重置本地 MySQL root 密码（MySQL 8.4）**
```powershell
# 1. 停止 MySQL 服务
Stop-Service MySQL84

# 2. 以跳过权限检查方式启动
& 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe' --skip-grant-tables --shared-memory

# 3. 另开终端连接
& 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe' -u root

# 4. 重置密码
FLUSH PRIVILEGES;
ALTER USER 'root'@'localhost' IDENTIFIED BY '新密码';

# 5. 重启 MySQL 服务
Start-Service MySQL84
```

### Q3: 启动报 `HikariPool - Connection is not available`

**原因**：MySQL 连接失败  
**解决**：
1. 确认 MySQL 服务运行中：`Get-Service MySQL80`
2. 确认 `application-local.yml` 中的 URL、用户名、密码正确
3. 确认数据库已创建且包含所需表

### Q4: 启动报 `KafkaListenerEndpointRegistry - Listener container failed to start`

**原因**：Kafka broker 不可用  
**解决**：
1. 确认 Kafka 已启动：`Test-NetConnection -ComputerName localhost -Port 9092`
2. 如未启动，运行 `start-all-middleware.bat` 或手动启动 `C:\middleware\kafka\run-kafka.bat`
3. 确认 `application-local.yml` 中 `spring.kafka.listener.missing-topics-fatal: false`

### Q5: 验证码/登录功能报 Redis 错误

**原因**：Redis 连接失败或需要密码  
**解决**：
1. 测试 Redis 连接：`redis-cli ping`
2. 如有密码，在 `spring.redis.password` 中填写

### Q6: 文件上传报 `profile directory not found`

**原因**：`ruoyi.profile` 指向的目录不存在  
**解决**：手动创建目录，如 `mkdir C:\osh\upload`

### Q7: 启动报 `Unknown column 'no' in 'field list'`

**原因**：`osh_member_plan` 表缺少 `no` 列（代码实体已添加但 DDL 未同步）  
**解决**：
```sql
ALTER TABLE osh_member_plan ADD COLUMN `no` VARCHAR(32) DEFAULT NULL COMMENT '编号' AFTER `id`;
```

### Q8: 启动报 `xxl-job registry fail, Connection refused`

**原因**：XXL-Job Admin 未启动或端口 8888 不可达  
**解决**：
1. 确认 XXL-Job Admin 已启动：访问 `http://localhost:8888/xxl-job-admin`
2. 如未启动，运行 `start-all-middleware.bat`
3. 确认 `application-local.yml` 中 `xxl.job.admin.addresses` 指向 `http://localhost:8888/xxl-job-admin`
4. 确认 `xxl.job.accessToken` 与 Admin 配置一致

---

## 八、安全提示

1. **`bootstrap-local.yml` 和 `application-local.yml` 已加入版本控制**，供团队共享使用模板
2. **请勿在本地配置文件中写入真实生产密码**
3. 本地 JWT secret 仅供开发使用，与生产环境不同
4. 所有本地调试操作 **严禁在远程服务器执行**
5. **`start-all-middleware.bat` 中不包含任何密码**，中间件默认凭据仅限本地使用
