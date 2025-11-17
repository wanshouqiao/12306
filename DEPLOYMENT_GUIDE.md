# 12306 聚合服务部署详细教程

## 📋 目录
- [环境要求](#环境要求)
- [第一步：安装中间件](#第一步安装中间件)
- [第二步：初始化数据库](#第二步初始化数据库)
- [第三步：配置项目](#第三步配置项目)
- [第四步：编译打包](#第四步编译打包)
- [第五步：启动服务](#第五步启动服务)
- [第六步：验证部署](#第六步验证部署)
- [常见问题](#常见问题)

---

## 环境要求

### 必需软件
- **JDK 17** （项目使用Java 17）
- **Maven 3.6+**
- **MySQL 8.0+**
- **Redis 6.0+**
- **Nacos 2.0+** （服务注册与配置中心）
- **RocketMQ 4.9+** （消息队列）
- **Sentinel 1.8+** （流量控制，可选）

### 服务端口规划
| 服务 | 端口 | 说明 |
|------|------|------|
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存 |
| Nacos | 8848 | 服务注册中心 |
| RocketMQ NameServer | 9876 | 消息队列 |
| Sentinel | 8686 | 流量控制（可选）|
| Gateway | 9000 | 网关服务 |
| Aggregation | 9005 | 聚合服务 |

---

## 第一步：安装中间件

### 1.1 安装 MySQL 8.0

**Windows:**
1. 下载 MySQL 8.0：https://dev.mysql.com/downloads/mysql/
2. 安装并设置 root 密码为 `root`
3. 启动 MySQL 服务

**验证安装：**
```powershell
mysql -u root -p
# 输入密码 root
```

### 1.2 安装 Redis

**Windows:**
1. 下载 Redis for Windows：https://github.com/tporadowski/redis/releases
2. 解压到目录（如 `C:\Redis`）
3. 修改 `redis.windows.conf`：
   ```
   requirepass 123456
   ```
4. 启动 Redis：
   ```powershell
   cd C:\Redis
   redis-server.exe redis.windows.conf
   ```

**验证安装：**
```powershell
redis-cli -h 127.0.0.1 -p 6379 -a 123456
# 执行 ping，应返回 PONG
```

### 1.3 安装 Nacos 2.0+

**下载安装：**
1. 下载 Nacos：https://github.com/alibaba/nacos/releases
2. 解压到目录（如 `C:\nacos`）
3. 修改配置（单机模式）：
   编辑 `conf\application.properties`：
   ```properties
   # 使用内嵌数据库（开发测试用）
   spring.sql.init.platform=derby
   
   # 或者使用MySQL（推荐生产环境）
   # spring.sql.init.platform=mysql
   # db.url.0=jdbc:mysql://127.0.0.1:3306/nacos?characterEncoding=utf8&connectTimeout=1000&socketTimeout=3000&autoReconnect=true&useUnicode=true&useSSL=false&serverTimezone=UTC
   # db.user.0=root
   # db.password.0=root
   ```

4. 启动 Nacos（单机模式）：
   ```powershell
   cd C:\nacos\bin
   startup.cmd -m standalone
   secret。key=bmFjb3NfMjAyNDAxMTBfc2hpZ3poX25hY29zX3Rva2Vu
   其他两个值是123
   ```

5. 访问控制台：http://localhost:8848/nacos
   - 用户名：nacos
   - 密码：nacos

**验证安装：**
- 访问 http://localhost:8848/nacos 能看到登录页面

### 1.4 安装 RocketMQ

**Windows:**
1. 下载 RocketMQ：https://rocketmq.apache.org/download/
2. 解压到目录（如 `C:\rocketmq`）
3. 配置环境变量：
   ```
   ROCKETMQ_HOME=C:\rocketmq
   NAMESRV_ADDR=127.0.0.1:9876
   ```

4. 启动 NameServer：
   ```powershell
   cd C:\rocketmq\bin
   start mqnamesrv.cmd
   ```

5. 启动 Broker：
   ```powershell
   cd C:\rocketmq\bin
   start mqbroker.cmd -n 127.0.0.1:9876 autoCreateTopicEnable=true
   ```

**验证安装：**
检查进程是否启动：
```powershell
jps | findstr "NamesrvStartup"
jps | findstr "BrokerStartup"
```

### 1.5 安装 Sentinel（可选）

**下载安装：**
1. 下载 Sentinel Dashboard：https://github.com/alibaba/Sentinel/releases
2. 启动：
   ```powershell
   java -Dserver.port=8686 -jar sentinel-dashboard-1.8.6.jar
   ```

3. 访问控制台：http://localhost:8686
   - 用户名：sentinel
   - 密码：sentinel

---

## 第二步：初始化数据库

### 2.1 创建数据库

连接 MySQL，执行：
```sql
CREATE DATABASE IF NOT EXISTS db_12306 
  DEFAULT CHARACTER SET utf8mb4 
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

### 2.2 导入数据库脚本

在项目根目录执行：
```powershell
# 进入数据库脚本目录
cd resources\db

# 导入数据
use db_12306;
source 12306-springboot.sql
```

或者使用 Navicat、DBeaver 等工具导入 `resources/db/12306-springboot.sql`

### 2.3 验证数据库

```sql
USE 12306;
SHOW TABLES;
```

应该能看到以下分表结构：
- t_user_0 ~ t_user_15（用户表）
- t_passenger_0 ~ t_passenger_15（乘客表）
- t_order_0 ~ t_order_15（订单表）
- t_order_item_0 ~ t_order_item_15（订单明细表）
- t_pay_0 ~ t_pay_15（支付表）
- t_train（列车表）
- t_station（车站表）
- 等等...

---

## 第三步：配置项目

### 3.1 修改聚合服务配置

检查配置文件：`services/aggregation-service/src/main/resources/application.yaml`

**主要配置项：**

```yaml
server:
  port: 9005  # 聚合服务端口

spring:
  datasource:
    # ShardingSphere 数据源（已配置）
    driver-class-name: org.apache.shardingsphere.driver.ShardingSphereDriver
    url: jdbc:shardingsphere:classpath:shardingsphere-config-dev.yaml
    
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      password: 123456  # ⚠️ 确保与你的 Redis 密码一致
      
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848  # Nacos 地址
      username: nacos
      password: nacos
      
rocketmq:
  name-server: 127.0.0.1:9876  # RocketMQ NameServer 地址
```

### 3.2 修改分库分表配置

检查配置文件：`services/aggregation-service/src/main/resources/shardingsphere-config-dev.yaml`

**主要配置项：**

```yaml
dataSources:
  ds_0:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.cj.jdbc.Driver
    jdbcUrl: jdbc:mysql://127.0.0.1:3306/12306?useUnicode=true&characterEncoding=UTF-8&rewriteBatchedStatements=true&allowMultiQueries=true&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root  # ⚠️ 确保与你的 MySQL 用户名一致
    password: root  # ⚠️ 确保与你的 MySQL 密码一致
```

### 3.3 修改网关服务配置

检查配置文件：`services/gateway-service/src/main/resources/application.yaml`

```yaml
server:
  port: 9000  # 网关端口

spring:
  profiles:
    active: aggregation  # ⚠️ 使用聚合服务模式
```

### 3.4 配置 Nacos 动态线程池（可选）

如果需要动态线程池功能，需在 Nacos 中添加配置：

1. 登录 Nacos 控制台：http://localhost:8848/nacos
2. 进入 **配置管理** -> **配置列表**
3. 点击 **+** 创建配置：
   - Data ID: `hippo4j-nacos.yaml`
   - Group: `DEFAULT_GROUP`
   - 配置格式: `YAML`
   - 配置内容：
     ```yaml
     spring:
       dynamic:
         thread-pool:
           executors:
             - thread-pool-id: 'select-seat-thread-pool-executor'
               core-pool-size: 20
               maximum-pool-size: 40
     ```

---

## 第四步：编译打包

### 4.1 编译整个项目

在项目根目录执行：
```powershell
# 清理并打包（跳过测试）
mvn clean package -DskipTests
```

如果遇到编译错误，可以尝试：
```powershell
# 先安装依赖到本地仓库
mvn clean install -DskipTests
```

### 4.2 检查打包结果

编译成功后，应该能在以下位置找到 JAR 文件：
```
services/aggregation-service/target/index12306-aggregation-service.jar
services/gateway-service/target/index12306-gateway-service.jar
```

---

## 第五步：启动服务

### 5.1 确认中间件已启动

启动服务前，确保以下中间件都已启动：
- ✅ MySQL (3306)
- ✅ Redis (6379)
- ✅ Nacos (8848)
- ✅ RocketMQ NameServer (9876)
- ✅ RocketMQ Broker
- ⭕ Sentinel (8686) - 可选

### 5.2 启动聚合服务

**方式一：使用 Maven 启动（开发模式）**
```powershell
cd services\aggregation-service
mvn spring-boot:run
```

**方式二：使用 JAR 包启动（生产模式）**
```powershell
cd services\aggregation-service\target
java -jar index12306-aggregation-service.jar
```

**启动参数（可选）：**
```powershell
# 指定 JVM 参数
java -Xms512m -Xmx2g -jar index12306-aggregation-service.jar

# 指定配置文件
java -jar index12306-aggregation-service.jar --spring.profiles.active=dev
```

### 5.3 启动网关服务

在新的命令行窗口执行：
```powershell
cd services\gateway-service\target
java -jar index12306-gateway-service.jar
```

### 5.4 查看启动日志

**正常启动日志应包含：**
```
Started AggregationServiceApplication in X.XXX seconds
Nacos naming client initialized
Tomcat started on port(s): 9005
```

**常见启动问题：**
- 如果提示连接 MySQL 失败，检查数据库配置和服务是否启动
- 如果提示连接 Redis 失败，检查 Redis 密码是否正确
- 如果提示连接 Nacos 失败，检查 Nacos 服务是否启动

---

## 第六步：验证部署

### 6.1 检查服务注册

访问 Nacos 控制台：http://localhost:8848/nacos

在 **服务管理** -> **服务列表** 中，应该能看到：
- ✅ index12306-aggregation-service
- ✅ index12306-gateway-service

### 6.2 测试接口

使用 Postman 或浏览器测试：

**1. 健康检查：**
```
GET http://localhost:9005/actuator/health
```

**2. 查询车站列表：**
```
GET http://localhost:9000/api/ticket-service/station/all
```

**3. 用户注册：**
```
POST http://localhost:9000/api/user-service/register
Content-Type: application/json

{
  "username": "testuser",
  "password": "123456",
  "realName": "测试用户",
  "idType": 0,
  "idCard": "110101199001011234",
  "phone": "13800138000",
  "mail": "test@example.com"
}
```

### 6.3 查看监控指标

访问 Actuator 端点：
```
http://localhost:9005/actuator
http://localhost:9005/actuator/metrics
http://localhost:9005/actuator/health
```

---

## 常见问题

### Q1: 编译时提示找不到依赖？
**解决方案：**
```powershell
# 先编译 frameworks 和 dependencies
mvn clean install -pl dependencies -am -DskipTests
mvn clean install -pl frameworks -am -DskipTests
# 再编译整个项目
mvn clean package -DskipTests
```

### Q2: 启动时提示端口被占用？
**解决方案：**
```powershell
# 查看端口占用
netstat -ano | findstr "9005"
# 修改 application.yaml 中的端口
```

### Q3: 连接 MySQL 失败？
**解决方案：**
1. 检查 MySQL 服务是否启动
2. 检查用户名密码是否正确
3. 检查数据库 `12306` 是否存在
4. 尝试手动连接：`mysql -u root -p`

### Q4: Redis 连接失败？
**解决方案：**
1. 检查 Redis 服务是否启动
2. 检查密码是否为 `123456`
3. 测试连接：`redis-cli -h 127.0.0.1 -p 6379 -a 123456`

### Q5: Nacos 注册失败？
**解决方案：**
1. 检查 Nacos 是否启动：http://localhost:8848/nacos
2. 检查用户名密码是否为 nacos/nacos
3. 查看 Nacos 日志：`nacos/logs/start.out`

### Q6: RocketMQ 连接失败？
**解决方案：**
1. 检查 NameServer 是否启动：`jps | findstr NamesrvStartup`
2. 检查 Broker 是否启动：`jps | findstr BrokerStartup`
3. 检查配置中的地址是否为 `127.0.0.1:9876`

### Q7: 启动成功但接口报错？
**解决方案：**
1. 检查数据库表是否正确导入
2. 查看服务日志，定位具体错误
3. 检查所有配置项是否正确

### Q8: 分库分表相关错误？
**解决方案：**
1. 确认数据库中有 t_user_0 到 t_user_15 等分表
2. 检查 `shardingsphere-config-dev.yaml` 配置
3. 确认 ShardingSphere 版本兼容

---

## 🎉 部署完成！

如果所有步骤都成功，你现在应该有：
- ✅ 聚合服务运行在 9005 端口
- ✅ 网关服务运行在 9000 端口
- ✅ 所有服务已注册到 Nacos
- ✅ 可以通过网关访问各个服务接口

下一步：
1. **启动前端项目**：参考 `console-vue/README.md`
2. **导入测试数据**：车次、车站等基础数据
3. **功能测试**：注册、登录、购票等完整流程

---

## 📚 相关文档

- [项目官方文档](https://nageoffer.com/12306)
- [接口文档](https://nageoffer.com/12306/interface/)
- [常见问题](https://nageoffer.com/12306/question/)

祝你学习顺利！🚀

