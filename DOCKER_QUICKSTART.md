# Docker 快速开始指南

## 🚀 一键启动

### Windows (PowerShell)
```powershell
.\deploy.ps1 start
```

### Linux/Mac
```bash
chmod +x deploy.sh
./deploy.sh start
```

## 🌐 访问地址

- **管理端**: http://localhost:8080
- **用户端**: http://localhost:8081  
- **API文档**: http://localhost:9171/swagger-ui.html

## 📋 常用命令

| 命令 | 说明 |
|------|------|
| `./deploy.sh start` | 启动所有服务 |
| `./deploy.sh stop` | 停止所有服务 |
| `./deploy.sh restart` | 重启所有服务 |
| `./deploy.sh logs` | 查看日志 |
| `./deploy.sh status` | 查看状态 |
| `./deploy.sh clean` | 清理数据 |

## 🔧 故障排查

**服务启动失败?**
```bash
docker-compose ps          # 查看服务状态
docker-compose logs        # 查看详细日志
```

**端口被占用?**
修改 `docker-compose.yml` 中的端口映射

**数据库连接失败?**
```bash
docker exec -it music-mysql mysql -uroot -proot
```

## 📚 详细文档

查看 [DOCKER_DEPLOYMENT.md](DOCKER_DEPLOYMENT.md) 获取完整部署指南
