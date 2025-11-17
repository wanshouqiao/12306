# 12306 网关服务启动脚本
# PowerShell 脚本

Write-Host "==================================" -ForegroundColor Cyan
Write-Host "启动 12306 网关服务" -ForegroundColor Cyan
Write-Host "==================================" -ForegroundColor Cyan
Write-Host ""

# 检查是否在项目根目录
if (-not (Test-Path "pom.xml")) {
    Write-Host "错误: 请在项目根目录下执行此脚本！" -ForegroundColor Red
    exit 1
}

# 检查 JAR 文件是否存在
$jarPath = "services\gateway-service\target\index12306-gateway-service.jar"
if (-not (Test-Path $jarPath)) {
    Write-Host "未找到 JAR 文件，开始编译项目..." -ForegroundColor Yellow
    Write-Host "执行: mvn clean package -DskipTests" -ForegroundColor Gray
    Write-Host ""
    
    mvn clean package -DskipTests
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "`n编译失败！请检查错误信息。" -ForegroundColor Red
        exit 1
    }
    
    Write-Host "`n编译成功！" -ForegroundColor Green
}

# 启动服务
Write-Host "`n正在启动网关服务..." -ForegroundColor Yellow
Write-Host "端口: 9000" -ForegroundColor Gray
Write-Host "日志: 将在下方显示" -ForegroundColor Gray
Write-Host ""
Write-Host "提示: 按 Ctrl+C 停止服务" -ForegroundColor Cyan
Write-Host "==================================" -ForegroundColor Cyan
Write-Host ""

# 启动 JAR
java -Xms256m -Xmx1g -jar $jarPath

Write-Host "`n服务已停止" -ForegroundColor Yellow

