# Docker 快速开始

## 🚀 30 秒快速启动

> ⚠️ 启动前先准备 `.env`：`docker-compose.yml` 对 `MYSQL_ROOT_PASSWORD`、`DB_USERNAME`、
> `DB_PASSWORD`、`REDIS_PASSWORD`、`JWT_SECRET` 使用了 `${VAR:?}` 必填语法，缺失时 compose 直接拒绝启动。

```powershell
# Windows
cp .env.example .env   # 首次：编辑 .env 填入真实口令
.\deploy.ps1 start
```

```bash
# Linux/Mac
cp .env.example .env   # 首次：编辑 .env 填入真实口令
chmod +x deploy.sh && ./deploy.sh start
```

✅ **自动完成**：
- 启动 MySQL、Redis、后端、前端（管理端+用户端）共 5 个服务
- 后端启动时 Flyway 自动执行 V1–V13 迁移脚本建库建表（库结构唯一出处，无需手工导入 SQL）
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

使用 `docker-compose.yml`，适合本地开发。注意：敏感变量同样是 `${VAR:?}` 必填，需要先准备 `.env`；
档位默认 prod，本地联调可在 `.env` 里显式设 `SPRING_PROFILES_ACTIVE=dev`。

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

### 自动初始化（唯一方式）
首次启动时，后端 Flyway 会自动执行 `Music-backend/src/main/resources/db/migration` 下的迁移脚本（V1–V13）建库建表。

> ⚠️ 根目录 `db_music.sql` 已停用：它是 V1 时代的整库旧快照，与 Flyway 脚本不同源；
> compose 也已不再把它挂载到 `/docker-entrypoint-initdb.d`。**请勿手工导入**，否则同一份库
> 会出现两个结构出处，后续排障时无法确认真实结构是谁给的。

---

## 🔍 故障排查

### 1. 端口被占用

**错误信息**：
```
Error starting userland proxy: Bind for 0.0.0.0:8080 failed: port is already allocated
```

**解决方案**：
修改 `docker-compose.yml` 中的端口映射（前端容器内监听 8080，不是 80）：
```yaml
ports:
  - "8082:8080"   # 将管理端改为 8082
  - "8083:8080"   # 将用户端改为 8083
  - "9172:9171"   # 将后端改为 9172
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
| 管理端前端 | 8080 | 8080 | Vue 2 + Nginx（unprivileged） |
| 用户端前端 | 8080 | 8081 | Vue 2 + Nginx（unprivileged） |
| 后端 API | 9171 | 9171 | Spring Boot |
| MySQL | 3306 | 不暴露 | 仅 compose 内部网络可达 |
| Redis | 6379 | 不暴露 | 仅 compose 内部网络可达 |

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
# 临时切换运行档位（默认 prod，本地联调可用 dev）
SPRING_PROFILES_ACTIVE=dev docker-compose up -d

# 或写入 .env 文件

SPRING_PROFILES_ACTIVE=dev
docker-compose up -d
```

换端口请直接修改 `docker-compose.yml` 的 `ports:` 映射（见“修改端口”章节）；口令类变量只能来自 `.env`。

---

## 📖 相关文档

- [DOCKER_DEPLOYMENT.md](DOCKER_DEPLOYMENT.md) - 完整 Docker 部署指南
- [README.md](../README.md) - 项目总览
- [API 开发文档.md](../API%20开发文档.md) - API 接口文档

---

## ❓ 常见问题

### Q: 首次启动很慢？
A: 首次需要下载镜像，后端启动时 Flyway 还会自动执行 V1–V13 迁移建库，请耐心等待 2-5 分钟。

### Q: 如何修改数据库密码？
A: 编辑 `.env` 文件，同步修改 `MYSQL_ROOT_PASSWORD` 与 `DB_PASSWORD`（两者需一致），然后重建 MySQL 容器：`docker-compose down -v && docker-compose up -d`（⚠️ `-v` 会清空数据）。

### Q: 数据会丢失吗？
A: 不会。数据存储在 Docker Volume 中，除非手动删除卷，否则数据会持久化。

### Q: 如何在服务器上部署？
A: 参考 [DOCKER_DEPLOYMENT.md](DOCKER_DEPLOYMENT.md) 的生产环境部署章节。

---

**提示**：遇到问题？查看完整文档 [DOCKER_DEPLOYMENT.md](DOCKER_DEPLOYMENT.md) 或提交 [Issue](https://github.com/ZapcoMan/Music/issues)

**最后更新**: 2026-11-10（按 v3.0.0 现状口径刷新）
