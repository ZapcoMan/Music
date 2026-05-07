# Docker 快速开始

## 🚀 30 秒快速启动

### Windows 用户
```powershell
.\deploy.ps1 start
```

### Linux/Mac 用户
```bash
chmod +x deploy.sh && ./deploy.sh start
```

✅ **自动完成**：
- 启动 MySQL、Redis、后端、前端（管理端+用户端）共 5 个服务
- 自动初始化数据库
- 自动配置网络连接

✅ **访问地址**：
- 管理端：http://localhost:8080
- 用户端：http://localhost:8081
- 后端 API：http://localhost:9171
- Swagger UI：http://localhost:9171/swagger-ui.html

---

## 📋 前置要求

- **Docker**: 20.10+
- **Docker Compose**: 2.0+

### 检查安装
```bash
docker --version
docker-compose --version
```

### 未安装？
- [Docker Desktop (Windows/Mac)](https://www.docker.com/products/docker-desktop)
- [Docker Engine (Linux)](https://docs.docker.com/engine/install/)

---

## 🔧 常用命令

### 启动服务
```bash
# Windows
.\deploy.ps1 start

# Linux/Mac
./deploy.sh start
```

### 停止服务
```bash
# Windows
.\deploy.ps1 stop

# Linux/Mac
./deploy.sh stop
```

### 重启服务
```bash
# Windows
.\deploy.ps1 restart

# Linux/Mac
./deploy.sh restart
```

### 查看日志
```bash
# 查看所有服务日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f backend
docker-compose logs -f admin-frontend
docker-compose logs -f user-frontend
```

### 查看服务状态
```bash
docker-compose ps
```

---

## ⚙️ 配置说明

### 开发环境（默认）
```bash
docker-compose up -d
```

使用 `docker-compose.yml`，适合本地开发。

### 生产环境
```bash
# 1. 复制环境变量模板
cp .env.example .env

# 2. 编辑 .env 文件，设置密码
vim .env

# 3. 启动生产环境
docker-compose -f docker-compose.prod.yml up -d
```

📖 详细配置：[DOCKER_DEPLOYMENT.md](DOCKER_DEPLOYMENT.md)

---

## 🗄️ 数据库初始化

### 自动初始化（推荐）
首次启动时，Flyway 会自动执行数据库迁移脚本。

### 手动导入
```bash
# 进入 MySQL 容器
docker-compose exec mysql mysql -u root -p db_music

# 或在宿主机执行
docker-compose exec -T mysql mysql -uroot -p${MYSQL_ROOT_PASSWORD} db_music < db_music.sql
```

---

## 🔍 故障排查

### 1. 端口被占用

**错误信息**：
```
Error starting userland proxy: Bind for 0.0.0.0:8080 failed: port is already allocated
```

**解决方案**：
修改 `docker-compose.yml` 中的端口映射：
```yaml
ports:
  - "8082:80"      # 将管理端改为 8082
  - "8083:80"      # 将用户端改为 8083
  - "9172:9171"    # 将后端改为 9172
```

### 2. 容器启动失败

**查看日志**：
```bash
docker-compose logs backend
```

**常见原因**：
- MySQL/Redis 未就绪：等待 30 秒后重试
- 内存不足：增加 Docker 内存限制
- 配置文件错误：检查 `.env` 文件

### 3. 无法访问服务

**检查容器状态**：
```bash
docker-compose ps
```

**检查网络连接**：
```bash
docker-compose exec backend ping mysql
docker-compose exec backend ping redis
```

**重启服务**：
```bash
docker-compose restart
```

### 4. 数据库连接失败

**检查 MySQL 是否启动**：
```bash
docker-compose ps mysql
```

**查看 MySQL 日志**：
```bash
docker-compose logs mysql
```

**测试连接**：
```bash
docker-compose exec mysql mysql -u root -p${MYSQL_ROOT_PASSWORD} -e "SHOW DATABASES;"
```

---

## 🧹 清理与维护

### 停止并删除容器
```bash
docker-compose down
```

### 删除数据卷（⚠️ 谨慎操作）
```bash
# 删除所有数据卷（包括数据库数据）
docker-compose down -v

# 仅删除指定卷
docker volume rm music_mysql-data
docker volume rm music_redis-data
```

### 清理无用镜像
```bash
docker image prune -a
```

### 查看磁盘占用
```bash
docker system df
```

---

## 📊 服务端口

| 服务 | 容器端口 | 宿主机端口 | 说明 |
|------|---------|-----------|------|
| 管理端前端 | 80 | 8080 | Vue 2 + Nginx |
| 用户端前端 | 80 | 8081 | Vue 2 + Nginx |
| 后端 API | 9171 | 9171 | Spring Boot |
| MySQL | 3306 | 3306 | 数据库 |
| Redis | 6379 | 6379 | 缓存 |

---

## 💡 高级用法

### 单服务操作
```bash
# 只启动后端
docker-compose up -d backend

# 只重启前端
docker-compose restart admin-frontend user-frontend

# 进入容器内部
docker-compose exec backend bash
docker-compose exec mysql mysql -u root -p
```

### 构建自定义镜像
```bash
# 重新构建后端镜像
docker-compose build backend

# 重新构建前端镜像
docker-compose build admin-frontend user-frontend

# 构建所有服务
docker-compose build
```

### 环境变量覆盖
```bash
# 临时修改端口
MYSQL_PORT=3307 docker-compose up -d

# 或使用 .env 文件
echo "MYSQL_PORT=3307" >> .env
docker-compose up -d
```

---

## 📖 相关文档

- [DOCKER_DEPLOYMENT.md](DOCKER_DEPLOYMENT.md) - 完整 Docker 部署指南
- [README.md](README.md) - 项目总览
- [API 开发文档.md](API%20开发文档.md) - API 接口文档

---

## ❓ 常见问题

### Q: 首次启动很慢？
A: 首次需要下载镜像和初始化数据库，请耐心等待 2-5 分钟。

### Q: 如何修改数据库密码？
A: 编辑 `.env` 文件，修改 `MYSQL_ROOT_PASSWORD`，然后重启服务。

### Q: 数据会丢失吗？
A: 不会。数据存储在 Docker Volume 中，除非手动删除卷，否则数据会持久化。

### Q: 如何在服务器上部署？
A: 参考 [DOCKER_DEPLOYMENT.md](DOCKER_DEPLOYMENT.md) 的生产环境部署章节。

---

**提示**：遇到问题？查看完整文档 [DOCKER_DEPLOYMENT.md](DOCKER_DEPLOYMENT.md) 或提交 [Issue](https://github.com/ZapcoMan/Music/issues)
