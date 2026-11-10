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
                │  （以下两个服务只在
                │   compose 内部网络可达，
                │   不再映射到宿主机端口）
       ┌────────┴──────────┐
       │                   │
┌──────▼──────┐    ┌──────▼──────┐
│   MySQL     │    │   Redis     │
│ mysql:3306  │    │  redis:6379 │
└─────────────┘    └─────────────┘
```

### 技术栈

| 服务 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 管理端前端 | Vue 2 + Nginx（unprivileged） | Vue ^2.5.2 | 后台管理系统，容器内监听 8080 |
| 用户端前端 | Vue 2 + Nginx（unprivileged） | Vue ^2.6.11 | 用户音乐网站，容器内监听 8080 |
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
# 开发/本地环境（compose 的必填变量同样生效，先准备 .env，见下方"环境变量"）
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

> ⚠️ 口径变化（v3.0.0 安全收紧）：**MySQL 与 Redis 不再暴露宿主端口**（3306/6379 仅在
> compose 内部网络可达，本机调试可按 `docker-compose.yml` 内注释临时绑 `127.0.0.1`）；
> **Druid 监控台在 prod 档位关闭**，不再有 `localhost:9171/druid` 入口；数据库/Redis 口令
> 一律来自 `.env` 的必填变量，不存在 root/root、admin 之类的默认值。

---

## ⚙️ 配置说明

### 环境变量

#### 开发环境（docker-compose.yml）

`docker-compose.yml` 中的敏感变量同样使用 `${VAR:?}` 强制语法：**没有 `.env`（或环境变量）时
compose 会直接拒绝启动**，而不是回落到示例弱口令。本地开发同样需要先：

```bash
cp .env.example .env
```

如只做本地联调，可在 `.env` 中显式设置 `SPRING_PROFILES_ACTIVE=dev` 覆盖默认的 prod 档位。

#### 生产环境（.env 文件）

1. 复制模板：
```bash
cp .env.example .env
```

2. 编辑 `.env` 文件（变量名与 `application-prod.yml` 占位一致，均无默认值，缺失即启动失败）：
```env
# MySQL 配置（MYSQL_ROOT_PASSWORD 需与 DB_PASSWORD 一致）
DB_USERNAME=your_db_user
DB_PASSWORD=your_secure_mysql_password
MYSQL_ROOT_PASSWORD=your_secure_mysql_password

# Redis 配置
REDIS_PASSWORD=your_secure_redis_password

# JWT 配置：HMAC-SHA384 签名要求密钥至少 448 bit（约 56 个 ASCII 字符），禁止沿用示例值
JWT_SECRET=your_at_least_56_chars_random_secret_key_change_in_production

# 允许的跨域来源（逗号分隔，禁止通配符）
CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://admin.yourdomain.com

# 时区 / 档位
TZ=Asia/Shanghai
SPRING_PROFILES_ACTIVE=prod
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

编辑 `docker-compose.yml`（注意：前端**容器内端口是 8080**——nginx-unprivileged 非 root 不能绑定 1024 以下端口，改宿主侧即可）：
```yaml
services:
  admin-frontend:
    ports:
      - "8082:8080"  # 管理端改为 8082
  
  user-frontend:
    ports:
      - "8083:8080"  # 用户端改为 8083
  
  backend:
    ports:
      - "9172:9171"  # 后端改为 9172
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

# 进入 Redis（容器内不设口令明文，用环境变量取）
docker-compose exec redis sh -c 'redis-cli -a "$REDIS_PASSWORD"'
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

库结构的唯一出处是 `Music-backend/src/main/resources/db/migration` 下的 Flyway 迁移脚本（V1–V13），
后端启动时自动建库建表，**无需也不应手工导入任何 SQL**；根目录 `db_music.sql` 已停用（它是 V1 时代
的旧快照，与 Flyway 脚本不同源，导入只会造成双出处冲突）。

### 4. 前端无法访问后端

**检查网络**：
```bash
docker-compose exec backend ping mysql
docker-compose exec backend ping redis
```

**检查配置**：
前端镜像构建时通过 build arg 注入 API 地址，指向**宿主机上的后端**（浏览器从宿主访问）：
```yaml
# docker-compose.yml
build:
  args:
    VUE_APP_API_BASE_URL: http://localhost:9171
```
若后端换了宿主端口（如 9172），需同步修改此地址并重新构建前端镜像。

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

- ✅ 修改所有默认密码（MySQL、Redis——compose 已用 `${VAR:?}` 强制必填，缺失即拒绝启动）
- ✅ 使用强 JWT Secret（HMAC-SHA384 签名，至少 448 bit，约 56 个 ASCII 字符）
- ✅ 启用 HTTPS（使用 Nginx 反向代理 + Let's Encrypt）
- ✅ 限制外部访问（防火墙规则；MySQL/Redis 默认已不暴露宿主端口）
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
FROM node:20-alpine AS builder
RUN npm ci && npm run build

# 运行阶段
FROM nginxinc/nginx-unprivileged:alpine   # 非 root 运行，容器内监听 8080
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
A: 首次需要下载镜像，后端启动时 Flyway 还会自动执行 V1–V13 迁移建库，请耐心等待 2-5 分钟。后续启动会快很多。

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

**最后更新**: 2026-11-10（按 v3.0.0 现状口径刷新：端口收敛、必填环境变量、Flyway 唯一库结构出处）
**维护者**: 开发团队  
**问题反馈**: [GitHub Issues](https://github.com/ZapcoMan/Music/issues)
