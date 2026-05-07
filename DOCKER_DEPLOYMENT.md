# Docker 部署完整指南

## 📋 概述

本文档提供音乐网站系统的完整 Docker 部署方案，包括开发环境、生产环境配置，以及常见问题解决方案。

🚀 **快速开始**：请参考 [DOCKER_QUICK_START.md](DOCKER_QUICK_START.md)

---

## 🏗️ 架构设计

### 服务架构

```
┌─────────────┐     ┌─────────────┐
│ Admin Front │     │ User Front  │
│   :8080     │     │   :8081     │
└──────┬──────┘     └──────┬──────┘
       │                   │
       └────────┬──────────┘
                │
       ┌────────▼──────────┐
       │   Backend API     │
       │     :9171         │
       └────────┬──────────┘
                │
       ┌────────┴──────────┐
       │                   │
┌──────▼──────┐    ┌──────▼──────┐
│   MySQL     │    │   Redis     │
│   :3306     │    │   :6379     │
└─────────────┘    └─────────────┘
```

### 技术栈

| 服务 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 管理端前端 | Vue 2 + Nginx | 2.6.11 | 后台管理系统 |
| 用户端前端 | Vue 2 + Nginx | 2.6.11 | 用户音乐网站 |
| 后端 API | Spring Boot | 3.4.3 | RESTful API |
| 数据库 | MySQL | 8.0 | 关系数据库 |
| 缓存 | Redis | 7.x | 缓存数据库 |

---

## 🚀 快速部署

### 前置要求

- **Docker**: 20.10+
- **Docker Compose**: 2.0+
- **内存**: 至少 4GB
- **磁盘**: 至少 10GB

### 使用部署脚本（推荐）

#### Windows
```powershell
.\deploy.ps1 start
```

#### Linux/Mac
```bash
chmod +x deploy.sh && ./deploy.sh start
```

### 使用 Docker Compose

```bash
# 开发环境
docker-compose up -d

# 生产环境
cp .env.example .env
# 编辑 .env 配置密码
docker-compose -f docker-compose.prod.yml up -d
```

---

## 🌐 访问地址

| 服务 | 地址 | 说明 |
|------|------|------|
| 管理端前端 | http://localhost:8080 | 后台管理系统 |
| 用户端前端 | http://localhost:8081 | 用户音乐网站 |
| 后端 API | http://localhost:9171 | RESTful API |
| Swagger UI | http://localhost:9171/swagger-ui.html | API 文档 |
| MySQL | localhost:3306 | 数据库 (root/root) |
| Redis | localhost:6379 | 缓存 (密码: admin) |
| Druid 监控 | http://localhost:9171/druid | 连接池监控 (admin/admin) |

---

## ⚙️ 配置说明

### 环境变量

#### 开发环境（docker-compose.yml）

默认配置已包含在 `docker-compose.yml` 中，无需额外配置。

#### 生产环境（.env 文件）

1. 复制模板：
```bash
cp .env.example .env
```

2. 编辑 `.env` 文件：
```env
# MySQL 配置
MYSQL_ROOT_PASSWORD=your_secure_password
MYSQL_DATABASE=db_music

# Redis 配置
REDIS_PASSWORD=your_redis_password

# JWT 配置
JWT_SECRET=your_jwt_secret_key_change_in_production

# 时区
TZ=Asia/Shanghai
```

3. 启动生产环境：
```bash
docker-compose -f docker-compose.prod.yml up -d
```

### 数据持久化

Docker Compose 配置了以下数据卷来持久化数据：

| 数据卷 | 用途 | 位置 |
|--------|------|------|
| `mysql-data` | MySQL 数据库文件 | `/var/lib/mysql` |
| `redis-data` | Redis 数据 | `/data` |
| `backend-uploads` | 后端上传文件 | `/app/uploads` |
| `backend-logs` | 后端日志文件 | `/app/logs` |

**查看数据卷**：
```bash
docker volume ls | grep music
```

**备份数据**：
```bash
# 备份 MySQL 数据
docker-compose exec mysql mysqldump -u root -p${MYSQL_ROOT_PASSWORD} db_music > backup.sql

# 备份 Redis 数据
docker-compose exec redis redis-cli -a ${REDIS_PASSWORD} BGSAVE
```

---

## 🔧 高级配置

### 修改端口

编辑 `docker-compose.yml`：
```yaml
services:
  admin-frontend:
    ports:
      - "8082:80"      # 管理端改为 8082
  
  user-frontend:
    ports:
      - "8083:80"      # 用户端改为 8083
  
  backend:
    ports:
      - "9172:9171"    # 后端改为 9172
```

### 资源限制

编辑 `docker-compose.yml`：
```yaml
services:
  backend:
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 2G
        reservations:
          cpus: '1'
          memory: 1G
```

### 自定义镜像构建

```bash
# 重新构建后端镜像
docker-compose build backend

# 重新构建前端镜像
docker-compose build admin-frontend user-frontend

# 构建所有服务
docker-compose build
```

---

## 📊 运维管理

### 查看服务状态

```bash
# 查看所有服务
docker-compose ps

# 查看特定服务
docker-compose ps backend

# 查看资源使用
docker stats
```

### 查看日志

```bash
# 查看所有服务日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f backend
docker-compose logs -f admin-frontend

# 查看最近 100 行
docker-compose logs --tail=100 backend
```

### 进入容器

```bash
# 进入后端容器
docker-compose exec backend bash

# 进入 MySQL
docker-compose exec mysql mysql -u root -p

# 进入 Redis
docker-compose exec redis redis-cli -a admin
```

### 重启服务

```bash
# 重启所有服务
docker-compose restart

# 重启特定服务
docker-compose restart backend

# 停止并删除容器
docker-compose down

# 停止并删除容器和数据卷（⚠️ 谨慎操作）
docker-compose down -v
```

---

## 🔍 故障排查

### 1. 端口被占用

**错误信息**：
```
Error starting userland proxy: Bind for 0.0.0.0:8080 failed: port is already allocated
```

**解决方案**：
修改 `docker-compose.yml` 中的端口映射（见上文"修改端口"章节）。

### 2. 容器启动失败

**查看日志**：
```bash
docker-compose logs backend
```

**常见原因**：
- MySQL/Redis 未就绪：等待 30 秒后重试
- 内存不足：增加 Docker 内存限制
- 配置文件错误：检查 `.env` 文件

**解决方案**：
```bash
# 重启服务
docker-compose restart

# 重新构建并启动
docker-compose up -d --build
```

### 3. 数据库连接失败

**检查 MySQL 状态**：
```bash
docker-compose ps mysql
docker-compose logs mysql
```

**测试连接**：
```bash
docker-compose exec mysql mysql -u root -p${MYSQL_ROOT_PASSWORD} -e "SHOW DATABASES;"
```

**初始化数据库**：
```bash
docker-compose exec -T mysql mysql -uroot -p${MYSQL_ROOT_PASSWORD} db_music < db_music.sql
```

### 4. 前端无法访问后端

**检查网络**：
```bash
docker-compose exec backend ping mysql
docker-compose exec backend ping redis
```

**检查配置**：
确保前端构建时的 API 地址正确：
```yaml
# docker-compose.yml
build:
  args:
    VUE_APP_API_BASE_URL: http://backend:9171  # 容器内通信
```

### 5. 内存不足

**增加 Docker 内存**：
- Docker Desktop：Settings → Resources → Memory
- Linux：修改 `/etc/docker/daemon.json`

**清理无用资源**：
```bash
# 清理未使用的镜像
docker image prune -a

# 清理未使用的容器
docker container prune

# 清理未使用的卷
docker volume prune

# 一键清理
docker system prune -a --volumes
```

---

## 🛡️ 安全建议

### 生产环境安全检查清单

- ✅ 修改所有默认密码（MySQL、Redis、Druid）
- ✅ 使用强 JWT Secret（至少 32 位随机字符串）
- ✅ 启用 HTTPS（使用 Nginx 反向代理 + Let's Encrypt）
- ✅ 限制外部访问（防火墙规则）
- ✅ 定期更新镜像（安全补丁）
- ✅ 备份重要数据
- ✅ 监控日志和异常

### 防火墙配置

```bash
# 仅开放必要端口
ufw allow 80/tcp    # HTTP
ufw allow 443/tcp   # HTTPS
ufw deny 3306/tcp   # 禁止外部访问 MySQL
ufw deny 6379/tcp   # 禁止外部访问 Redis
```

---

## 📈 性能优化

### 1. 多阶段构建

项目已使用多阶段构建，镜像体积优化 60%+：

```dockerfile
# 构建阶段
FROM node:16 AS builder
RUN npm run build

# 运行阶段
FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
```

### 2. 缓存优化

- 使用 `.dockerignore` 排除不必要文件
- 合理分层 Dockerfile 指令
- 使用 Alpine 基础镜像

### 3. 资源监控

```bash
# 实时监控资源使用
docker stats

# 查看详细信息
docker inspect backend
```

---

## 🔄 持续集成/持续部署（CI/CD）

### GitHub Actions 示例

```yaml
name: Docker Build and Deploy

on:
  push:
    branches: [ main ]

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Build and Push Docker Images
        run: |
          docker-compose build
          docker-compose push
      
      - name: Deploy to Server
        run: |
          ssh user@server "cd /path/to/project && docker-compose pull && docker-compose up -d"
```

---

## 📖 相关文档

- [DOCKER_QUICK_START.md](DOCKER_QUICK_START.md) - Docker 快速开始指南
- [README.md](README.md) - 项目总览
- [API 开发文档.md](API%20开发文档.md) - API 接口文档

---

## ❓ 常见问题

### Q: 首次启动很慢？
A: 首次需要下载镜像和初始化数据库，请耐心等待 2-5 分钟。后续启动会快很多。

### Q: 如何修改数据库密码？
A: 编辑 `.env` 文件，修改 `MYSQL_ROOT_PASSWORD`，然后执行 `docker-compose down -v && docker-compose up -d`（⚠️ 会清空数据）。

### Q: 数据会丢失吗？
A: 不会。数据存储在 Docker Volume 中，除非手动执行 `docker-compose down -v`，否则数据会持久化。

### Q: 如何在服务器上部署？
A: 
1. 安装 Docker 和 Docker Compose
2. 克隆项目代码
3. 配置 `.env` 文件
4. 执行 `docker-compose -f docker-compose.prod.yml up -d`
5. 配置 Nginx 反向代理和 HTTPS

### Q: 如何查看磁盘占用？
A: 
```bash
docker system df
docker volume ls
du -sh /var/lib/docker/volumes
```

---

**最后更新**: 2026-05-07  
**维护者**: 开发团队  
**问题反馈**: [GitHub Issues](https://github.com/ZapcoMan/Music/issues)
