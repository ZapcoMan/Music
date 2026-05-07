# 音乐网站 Docker 部署脚本 (Windows PowerShell)
# 用法: .\deploy.ps1 [start|stop|restart|build|logs|clean|status]

param(
    [Parameter(Position=0)]
    [ValidateSet("start", "stop", "restart", "build", "logs", "clean", "status")]
    [string]$Command = "start",
    
    [Parameter(Position=1)]
    [string]$ServiceName = ""
)

# 项目根目录
$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "================================" -ForegroundColor Green
Write-Host "  音乐网站 Docker 部署脚本" -ForegroundColor Green
Write-Host "================================" -ForegroundColor Green

# 检查 Docker 是否安装
try {
    docker --version | Out-Null
} catch {
    Write-Host "错误: Docker 未安装" -ForegroundColor Red
    exit 1
}

# 检查 Docker Compose 是否安装
try {
    docker compose version 2>$null
    if ($LASTEXITCODE -eq 0) {
        $DockerCompose = "docker compose"
    } else {
        docker-compose --version 2>$null
        if ($LASTEXITCODE -eq 0) {
            $DockerCompose = "docker-compose"
        } else {
            Write-Host "错误: Docker Compose 未安装" -ForegroundColor Red
            exit 1
        }
    }
} catch {
    Write-Host "错误: Docker Compose 未安装" -ForegroundColor Red
    exit 1
}

switch ($Command) {
    "start" {
        Write-Host "启动所有服务..." -ForegroundColor Yellow
        Set-Location $ProjectRoot
        Invoke-Expression "$DockerCompose up -d"
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✓ 服务启动成功" -ForegroundColor Green
            Write-Host "等待服务就绪..." -ForegroundColor Yellow
            Start-Sleep -Seconds 10
            
            Write-Host "================================" -ForegroundColor Green
            Write-Host "访问地址:" -ForegroundColor Green
            Write-Host "  管理端: http://localhost:8080"
            Write-Host "  用户端: http://localhost:8081"
            Write-Host "  后端API: http://localhost:9171"
            Write-Host "  Swagger UI: http://localhost:9171/swagger-ui.html"
            Write-Host "================================" -ForegroundColor Green
        } else {
            Write-Host "✗ 服务启动失败" -ForegroundColor Red
        }
    }
    
    "stop" {
        Write-Host "停止所有服务..." -ForegroundColor Yellow
        Set-Location $ProjectRoot
        Invoke-Expression "$DockerCompose down"
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✓ 服务已停止" -ForegroundColor Green
        }
    }
    
    "restart" {
        Write-Host "重启所有服务..." -ForegroundColor Yellow
        Set-Location $ProjectRoot
        Invoke-Expression "$DockerCompose down"
        Invoke-Expression "$DockerCompose up -d"
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✓ 服务已重启" -ForegroundColor Green
        }
    }
    
    "build" {
        Write-Host "重新构建所有镜像..." -ForegroundColor Yellow
        Set-Location $ProjectRoot
        Invoke-Expression "$DockerCompose build --no-cache"
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✓ 镜像构建完成" -ForegroundColor Green
        }
    }
    
    "logs" {
        Write-Host "查看服务日志 (Ctrl+C 退出)..." -ForegroundColor Yellow
        Set-Location $ProjectRoot
        
        if ($ServiceName) {
            Invoke-Expression "$DockerCompose logs -f $ServiceName"
        } else {
            Invoke-Expression "$DockerCompose logs -f"
        }
    }
    
    "clean" {
        Write-Host "警告: 这将删除所有容器、卷和镜像!" -ForegroundColor Red
        $confirm = Read-Host "确认继续? (y/n)"
        
        if ($confirm -eq "y" -or $confirm -eq "Y") {
            Set-Location $ProjectRoot
            Invoke-Expression "$DockerCompose down -v --rmi all"
            
            if ($LASTEXITCODE -eq 0) {
                Write-Host "✓ 清理完成" -ForegroundColor Green
            }
        } else {
            Write-Host "操作已取消" -ForegroundColor Yellow
        }
    }
    
    "status" {
        Write-Host "服务状态:" -ForegroundColor Yellow
        Set-Location $ProjectRoot
        Invoke-Expression "$DockerCompose ps"
    }
}

exit 0
