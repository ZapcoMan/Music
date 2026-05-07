# Docker 部署指南

## 📋 目录结构

```
Music/
├── docker-compose.yml          # Docker Compose 配置文件
├── deploy.sh                   # Linux/Mac 部署脚本
├── deploy.ps1                  # Windows PowerShell 部署脚本
├── .dockerignore               # 根目录 Docker 忽略文件
│
├── Music-backend/              # 后端服务
│   ├── Dockerfile              # 后端 Docker 镜像配置
│   └── .dockerignore           # 后端 Docker 忽略文件
│
├── MusicAdmin-frontend/        # 管理端前端
│   ├── Dockerfile              # 管理端 Docker 镜像配置
│   ├── nginx.conf              # Nginx 配置文件
│   └── .dockerignore           # 管理端 Docker 忽略文件
│
└── MusicUser-frontend/         # 用户端前端
    ├── Dockerfile              # 用户端 Docker 镜像配置
    ├── nginx.conf              # Nginx 配置文件
    └── .dockerignore           # 用户端 Docker 忽略文件
```

## 🚀 快速开始

### 前置要求

- Docker 20.10+ 
- Docker Compose 2.0+ (或 docker-compose 1.29+)
- 至少 4GB 可用内存
- 至少 10GB 可用磁盘空间

### 方式一: 使用部署脚本 (推荐)

#### Windows (PowerShell)

```powershell
# 启动所有服务
.\deploy.ps1 start

# 查看服务状态
.\deploy.ps1 status

# 查看日志
.\deploy.ps1 logs

# 停止服务
.\deploy.ps1 stop

# 重启服务
.\deploy.ps1 restart

# 重新构建镜像
.\deploy.ps1 build

# 清理所有数据
.\deploy.ps1 clean
```

#### Linux/Mac

```bash
# 添加执行权限
chmod +x deploy.sh

# 启动所有服务
./deploy.sh start

# 查看服务状态
./deploy.sh status

# 查看日志
./deploy.sh logs

# 停止服务
./deploy.sh stop

# 重启服务
./deploy.sh restart

# 重新构建镜像
./deploy.sh build

# 清理所有数据
./deploy.sh clean
```

### 方式二: 使用 Docker Compose 命令

```bash
# 启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down

# 重新构建并启动
docker-compose up -d --build

# 清理所有数据(包括卷)
docker-compose down -v
```

## 🌐 访问地址

服务启动成功后,可以通过以下地址访问:

| 服务 | 地址 | 说明 |
|------|------|------|
| 管理端前端 | http://localhost:8080 | 后台管理系统 |
| 用户端前端 | http://localhost:8081 | 用户音乐网站 |
| 后端 API | http://localhost:9171 | RESTful API |
| Swagger UI | http://localhost:9171/swagger-ui.html | API 文档 |
| MySQL | localhost:3306 | 数据库 (root/root) |
| Redis | localhost:6379 | 缓存 (密码: admin) |
| Druid 监控 | http://localhost:9171/druid | 数据库连接池监控 (admin/admin) |

## 📦 服务架构

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

## ⚙️ 配置说明

### 环境变量

可以在 `docker-compose.yml` 中修改以下配置:

#### 后端服务
```yaml
environment:
  SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/db_music
  SPRING_DATASOURCE_USERNAME: root
  SPRING_DATASOURCE_PASSWORD: root
  SPRING_DATA_REDIS_HOST: redis
  SPRING_DATA_REDIS_PORT: 6379
  SPRING_DATA_REDIS_PASSWORD: admin
  JWT_SECRET: YourSecretKey
  TZ: Asia/Shanghai
```

#### 前端服务
```yaml
build:
  args:
    VUE_APP_API_BASE_URL: http://localhost:9171  # 后端 API 地址
```

### 数据持久化

Docker Compose 配置了以下数据卷来持久化数据:

- `mysql-data`: MySQL 数据库文件
- `redis-data`: Redis 数据
- `backend-uploads`: 后端上传的文件(歌曲、图片等)
- `backend-logs`: 后端日志文件

## 🔧 常见问题

### 1. 端口冲突

如果端口被占用,可以修改 `docker-compose.yml` 中的端口映射:

```yaml
ports:
  - "8080:80"  # 改为 "8082:80"
```

### 2. 内存不足

可以增加 Docker 的内存限制,或在 `docker-compose.yml` 中添加资源限制:

```yaml
services:
  backend:
    deploy:
      resources:
        limits:
          memory: 1G
```

### 3. 数据库初始化失败

确保 `db_music.sql` 文件在项目根目录,Docker 会在首次启动时自动导入。

### 4. 前端无法连接后端

检查前端构建时的 `VUE_APP_API_BASE_URL` 是否正确指向后端地址。

### 5. 查看特定服务日志

```bash
# 查看后端日志
docker-compose logs -f backend

# 查看管理端日志
docker-compose logs -f admin-frontend

# 查看数据库日志
docker-compose logs -f mysql
```

### 6. 进入容器内部

```bash
# 进入后端容器
docker exec -it music-backend sh

# 进入数据库容器
docker exec -it music-mysql mysql -uroot -proot

# 进入 Redis 容器
docker exec -it music-redis redis-cli -a admin
```

## 🔄 更新部署

### 更新后端代码

```bash
# 重新构建后端镜像
docker-compose build backend

# 重启后端服务
docker-compose up -d backend
```

### 更新前端代码

```bash
# 重新构建前端镜像
docker-compose build admin-frontend  # 或 user-frontend

# 重启前端服务
docker-compose up -d admin-frontend
```

### 更新所有服务

```bash
# 重新构建所有镜像
docker-compose build --no-cache

# 重启所有服务
docker-compose up -d
```

## 🛡️ 生产环境建议

1. **修改默认密码**
   - MySQL root 密码
   - Redis 密码
   - JWT Secret

2. **启用 HTTPS**
   - 在 Nginx 中配置 SSL 证书
   - 使用 Let's Encrypt 免费证书

3. **限制资源使用**
   ```yaml
   services:
     backend:
       deploy:
         resources:
           limits:
             cpus: '1.0'
             memory: 1G
   ```

4. **日志管理**
   - 配置日志轮转
   - 集成 ELK 或类似日志系统

5. **备份策略**
   ```bash
   # 备份数据库
   docker exec music-mysql mysqldump -uroot -proot db_music > backup.sql
   
   # 恢复数据库
   docker exec -i music-mysql mysql -uroot -proot db_music < backup.sql
   ```

6. **监控告警**
   - 使用 Prometheus + Grafana
   - 配置健康检查告警

## 📊 性能优化

### 后端优化

已在 Dockerfile 中配置:
- JVM 参数: `-Xms256m -Xmx512m`
- 使用 JRE 而非 JDK 减小镜像体积
- 多阶段构建减小最终镜像大小

### 前端优化

Nginx 配置已包含:
- Gzip 压缩
- 静态资源缓存 (30天)
- HTTP/2 支持

### 数据库优化

MySQL 配置:
- UTF8MB4 字符集
- 时区设置为 Asia/Shanghai
- 数据持久化到卷

## 🆘 故障排查

### 服务启动失败

```bash
# 查看所有服务状态
docker-compose ps

# 查看详细日志
docker-compose logs

# 检查特定服务
docker-compose logs backend
```

### 数据库连接失败

```bash
# 检查 MySQL 是否运行
docker-compose ps mysql

# 测试数据库连接
docker exec -it music-mysql mysql -uroot -proot -e "SHOW DATABASES;"
```

### 前端白屏

```bash
# 检查 Nginx 配置
docker exec -it music-admin-frontend nginx -t

# 查看 Nginx 错误日志
docker exec -it music-admin-frontend cat /var/log/nginx/error.log
```

## 📝 附录

### Docker 常用命令

```bash
# 查看所有容器
docker ps -a

# 查看所有镜像
docker images

# 删除未使用的镜像
docker image prune -a

# 删除未使用的卷
docker volume prune

# 查看磁盘使用情况
docker system df
```

### Docker Compose 常用命令

```bash
# 启动服务
docker-compose up -d [service_name]

# 停止服务
docker-compose down [service_name]

# 重启服务
docker-compose restart [service_name]

# 查看日志
docker-compose logs -f [service_name]

# 进入容器
docker-compose exec [service_name] sh

# 查看服务状态
docker-compose ps
```

## 📞 技术支持

如遇到问题,请检查:
1. Docker 和 Docker Compose 版本是否符合要求
2. 端口是否被占用
3. 磁盘空间是否充足
4. 查看相关服务的日志

---

**祝部署顺利! 🎉**
