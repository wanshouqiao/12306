# 12306 部署脚本说明

本目录包含了帮助快速部署 12306 项目的脚本文件。

## 📁 脚本清单

### 1. `check-environment.ps1` - 环境检查脚本
检查所有必需的中间件是否已正确安装和启动。

**使用方法：**
```powershell
# 在项目根目录执行
.\scripts\check-environment.ps1
```

**检查项：**
- ✅ Java 17+
- ✅ Maven 3.6+
- ✅ MySQL (3306)
- ✅ Redis (6379)
- ✅ Nacos (8848)
- ✅ RocketMQ (9876)

---

### 2. `start-aggregation.ps1` - 启动聚合服务
一键编译并启动聚合服务。

**使用方法：**
```powershell
# 在项目根目录执行
.\scripts\start-aggregation.ps1
```

**功能：**
- 自动检查 JAR 文件是否存在
- 如果不存在，自动编译项目
- 启动聚合服务（端口 9005）

**停止服务：** 按 `Ctrl+C`

---

### 3. `start-gateway.ps1` - 启动网关服务
一键编译并启动网关服务。

**使用方法：**
```powershell
# 在新的命令行窗口执行
.\scripts\start-gateway.ps1
```

**功能：**
- 自动检查 JAR 文件是否存在
- 如果不存在，自动编译项目
- 启动网关服务（端口 9000）

**停止服务：** 按 `Ctrl+C`

---

### 4. `init-database.sql` - 数据库初始化脚本
快速创建 12306 数据库。

**使用方法：**
```powershell
# 在 MySQL 客户端执行
mysql -u root -p < scripts\init-database.sql

# 然后导入数据表
mysql -u root -p 12306 < resources\db\12306-springboot.sql
```

---

## 🚀 快速部署流程

### 步骤 1: 检查环境
```powershell
.\scripts\check-environment.ps1
```

如果有任何检查失败，请参考 `DEPLOYMENT_GUIDE.md` 安装对应的中间件。

### 步骤 2: 初始化数据库
```powershell
# 创建数据库
mysql -u root -p < scripts\init-database.sql

# 导入数据表
mysql -u root -p 12306 < resources\db\12306-springboot.sql
```

### 步骤 3: 启动聚合服务
在**第一个**命令行窗口：
```powershell
.\scripts\start-aggregation.ps1
```

等待服务启动完成，看到 "Started AggregationServiceApplication" 提示。

### 步骤 4: 启动网关服务
在**第二个**命令行窗口：
```powershell
.\scripts\start-gateway.ps1
```

等待服务启动完成，看到 "Started GatewayServiceApplication" 提示。

### 步骤 5: 验证部署
访问以下地址验证：
- Nacos 控制台: http://localhost:8848/nacos
- 健康检查: http://localhost:9005/actuator/health
- 网关健康检查: http://localhost:9000/actuator/health

---

## ⚠️ 注意事项

1. **执行脚本前确保已安装 PowerShell**（Windows 10/11 自带）

2. **脚本执行策略**
   如果提示 "无法加载脚本"，需要修改执行策略：
   ```powershell
   # 以管理员身份运行 PowerShell
   Set-ExecutionPolicy RemoteSigned -Scope CurrentUser
   ```

3. **中间件启动顺序**
   建议按以下顺序启动中间件：
   1. MySQL
   2. Redis
   3. Nacos
   4. RocketMQ NameServer
   5. RocketMQ Broker

4. **端口占用**
   如果端口被占用，可以：
   - 关闭占用端口的程序
   - 或修改配置文件中的端口号

5. **内存要求**
   - 聚合服务: 至少 512MB，推荐 2GB
   - 网关服务: 至少 256MB，推荐 1GB
   - 整体系统: 建议至少 8GB RAM

---

## 🐛 故障排查

### 问题：脚本无法执行
**解决方案：**
```powershell
# 检查执行策略
Get-ExecutionPolicy

# 如果是 Restricted，修改为 RemoteSigned
Set-ExecutionPolicy RemoteSigned -Scope CurrentUser
```

### 问题：编译失败
**解决方案：**
```powershell
# 清理并重新编译
mvn clean install -DskipTests

# 如果还是失败，尝试分步编译
mvn clean install -pl dependencies -am -DskipTests
mvn clean install -pl frameworks -am -DskipTests
mvn clean package -DskipTests
```

### 问题：服务启动失败
**解决方案：**
1. 查看启动日志，定位具体错误
2. 运行 `check-environment.ps1` 检查中间件状态
3. 检查配置文件中的连接信息是否正确

---

## 📚 更多帮助

- **完整部署教程**: 查看项目根目录的 `DEPLOYMENT_GUIDE.md`
- **官方文档**: https://nageoffer.com/12306
- **常见问题**: https://nageoffer.com/12306/question/

如有问题，欢迎提 Issue！

