# 12306 环境检查脚本
# PowerShell 脚本，用于检查所有必需的中间件是否正常运行

Write-Host "==================================" -ForegroundColor Cyan
Write-Host "12306 环境检查工具" -ForegroundColor Cyan
Write-Host "==================================" -ForegroundColor Cyan
Write-Host ""

$allGood = $true

# 检查 Java 版本
Write-Host "[1/6] 检查 Java 环境..." -ForegroundColor Yellow
try {
    $javaVersionOutput = java -version 2>&1 | Out-String
    if ($javaVersionOutput -match "version") {
        # 提取版本号
        if ($javaVersionOutput -match 'version "(\d+)') {
            $javaVersion = [int]$matches[1]
            if ($javaVersion -ge 17) {
                Write-Host "  ✓ Java $javaVersion 已安装" -ForegroundColor Green
            } else {
                Write-Host "  ✗ 需要 Java 17+，当前版本: Java $javaVersion" -ForegroundColor Red
                $allGood = $false
            }
        } else {
            Write-Host "  ✓ Java 已安装" -ForegroundColor Green
        }
    } else {
        Write-Host "  ✗ 未检测到 Java，请安装 JDK 17+" -ForegroundColor Red
        $allGood = $false
    }
} catch {
    Write-Host "  ✗ 未检测到 Java，请安装 JDK 17+" -ForegroundColor Red
    $allGood = $false
}

# 检查 Maven
Write-Host "`n[2/6] 检查 Maven..." -ForegroundColor Yellow
try {
    $mavenVersion = mvn -version 2>&1 | Select-String "Apache Maven" | Select-Object -First 1
    Write-Host "  ✓ $mavenVersion" -ForegroundColor Green
} catch {
    Write-Host "  ✗ 未检测到 Maven" -ForegroundColor Red
    $allGood = $false
}

# 检查 MySQL
Write-Host "`n[3/6] 检查 MySQL (端口 3306)..." -ForegroundColor Yellow
$mysqlPort = Test-NetConnection -ComputerName localhost -Port 3306 -WarningAction SilentlyContinue
if ($mysqlPort.TcpTestSucceeded) {
    Write-Host "  ✓ MySQL 正在运行 (端口 3306)" -ForegroundColor Green
} else {
    Write-Host "  ✗ MySQL 未运行或端口 3306 未开放" -ForegroundColor Red
    $allGood = $false
}

# 检查 Redis
Write-Host "`n[4/6] 检查 Redis (端口 6379)..." -ForegroundColor Yellow
$redisPort = Test-NetConnection -ComputerName localhost -Port 6379 -WarningAction SilentlyContinue
if ($redisPort.TcpTestSucceeded) {
    Write-Host "  ✓ Redis 正在运行 (端口 6379)" -ForegroundColor Green
} else {
    Write-Host "  ✗ Redis 未运行或端口 6379 未开放" -ForegroundColor Red
    $allGood = $false
}

# 检查 Nacos
Write-Host "`n[5/6] 检查 Nacos (端口 8848)..." -ForegroundColor Yellow
$nacosPort = Test-NetConnection -ComputerName localhost -Port 8848 -WarningAction SilentlyContinue
if ($nacosPort.TcpTestSucceeded) {
    Write-Host "  ✓ Nacos 正在运行 (端口 8848)" -ForegroundColor Green
    Write-Host "    访问地址: http://localhost:8848/nacos" -ForegroundColor Gray
} else {
    Write-Host "  ✗ Nacos 未运行或端口 8848 未开放" -ForegroundColor Red
    $allGood = $false
}

# 检查 RocketMQ
Write-Host "`n[6/6] 检查 RocketMQ (端口 9876)..." -ForegroundColor Yellow
$rocketmqPort = Test-NetConnection -ComputerName localhost -Port 9876 -WarningAction SilentlyContinue
if ($rocketmqPort.TcpTestSucceeded) {
    Write-Host "  ✓ RocketMQ NameServer 正在运行 (端口 9876)" -ForegroundColor Green
} else {
    Write-Host "  ✗ RocketMQ NameServer 未运行或端口 9876 未开放" -ForegroundColor Red
    $allGood = $false
}

# 汇总结果
Write-Host "`n==================================" -ForegroundColor Cyan
if ($allGood) {
    Write-Host "✓ 所有环境检查通过！可以启动服务" -ForegroundColor Green
} else {
    Write-Host "✗ 部分环境检查失败，请按照提示修复" -ForegroundColor Red
    Write-Host "`n详细部署教程请查看: DEPLOYMENT_GUIDE.md" -ForegroundColor Yellow
}
Write-Host "==================================" -ForegroundColor Cyan

Write-Host "`n按任意键退出..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")

