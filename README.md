# 🎵 音乐网站 (Music Website)

<div align="center">

![Version](https://img.shields.io/badge/Version-v1.1.0-informational)
![CI](https://github.com/ZapcoMan/Music/actions/workflows/ci.yml/badge.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-brightgreen)
![Vue](https://img.shields.io/badge/Vue-2.6.11-4FC08D)
![MySQL](https://img.shields.io/badge/MySQL-8.0-orange)
![Tests](https://img.shields.io/badge/Tests-146%20passed%2C%200%20skipped-brightgreen)
![Docker](https://img.shields.io/badge/Docker-Ready-blue)
![License](https://img.shields.io/badge/License-Apache%202.0-red)

**基于 Spring Boot 3 + Vue 2 的现代化音乐推荐系统**

[快速开始](#-快速开始) • [Docker 部署](#-docker-部署推荐-) • [API 文档](#-api-文档) • [测试说明](#-测试说明)

</div>

---

## 📋 目录

- [项目介绍](#-项目介绍)
- [技术栈](#-技术栈)
- [功能特性](#-功能特性)
- [快速开始](#-快速开始)
- [项目结构](#-项目结构)
- [API 文档](#-api-文档)
- [测试](#-测试)
- [部署](#-部署)
- [开发指南](#-开发指南)
- [贡献](#-贡献)

---

## 📖 项目简介

音乐网站系统是一个功能完善的全栈应用，提供歌曲推荐、用户管理、歌单管理、评论互动等核心业务功能。

### ✨ 核心特性

- 🏗️ **前后端分离架构**：Spring Boot 3.4.3 + Vue 2.6.11 + Element UI
- 🔐 **JWT 身份认证**：安全的用户登录和权限控制，支持 IP 绑定
- 🔒 **BCrypt 密码加密**：业界标准的强哈希算法，保障密码安全
- ⚡ **API 限流保护**：防止暴力破解和恶意请求
- 📝 **请求/响应日志**：完整的 API 调用日志,便于问题排查和性能监控
- 🎯 **协同过滤推荐**：基于用户评分的智能歌曲推荐算法
- 💾 **Redis 缓存优化**：提升系统性能和响应速度
- 📊 **Swagger API 文档**：在线接口文档和测试
- 🧪 **完整测试覆盖**：141 个单元测试，100% 通过率
- 🐳 **Docker 一键部署**：容器化部署，开箱即用
- 📱 **响应式设计**：支持 PC 和移动端访问

```
┌─────────────────────────────────────────────────────┐
│                  前端层 (Vue 2.x)                     │
├──────────────────┬──────────────────────────────────┤
│  管理端 (8080)    │      用户端 (8081)                │
│  MusicAdmin       │      MusicUser                   │
│  - 后台管理        │      - 音乐播放                   │
│  - 数据维护        │      - 歌曲推荐                   │
│  - 用户管理        │      - 个人收藏                   │
└────────┬─────────┴──────────────┬───────────────────┘
         │                        │
         └────────┬───────────────┘
                  │ HTTP/REST API
         ┌────────▼───────────────┐
         │   后端层 (Spring Boot)  │
         │     Port: 9171         │
         ├────────────────────────┤
         │ • JWT 认证             │
         │ • BCrypt 密码加密      │
         │ • API 限流             │
         │ • Swagger 文档         │
         └────────┬───────────────┘
                  │
         ┌────────┴───────────────┐
         │      数据层             │
         ├──────────┬─────────────┤
         │ MySQL    │  Redis      │
         │ 8.0      │  7.x        │
         │ - 业务数据│  - 缓存     │
         │ - Flyway │  - Session  │
         └──────────┴─────────────┘
```

### 核心特性

✅ **协同过滤推荐算法** - 基于用户评分的智能歌曲推荐  
✅ **JWT 统一认证** - 无状态 Token 认证,支持 IP 绑定  
✅ **BCrypt 密码加密** - Spring Security 安全加密  
✅ **API 限流保护** - 防止暴力破解和恶意请求  
✅ **Redis 缓存优化** - 提升系统性能和响应速度  
✅ **Swagger API 文档** - 在线接口文档和测试  
✅ **Docker 容器化** - 一键部署,开箱即用  
✅ **完整单元测试** - JUnit 5 测试覆盖核心业务  

---

## 🛠️ 技术栈

### 后端技术

| 技术 | 版本 | 说明 |
|------|------|------|
| **Spring Boot** | 3.4.3 | 核心框架 |
| **MyBatis** | 3.0.4 | ORM 框架 |
| **PageHelper** | 2.1.0 | 分页插件 |
| **MySQL** | 8.0 | 关系数据库 |
| **Redis** | 7.x | 缓存数据库 |
| **Druid** | 1.2.23 | 数据库连接池 |
| **Flyway** | Latest | 数据库迁移 |
| **Spring Security** | - | BCrypt 密码加密 |
| **JWT (jjwt)** | 0.12.6 | Token 认证 |
| **SpringDoc** | 2.8.5 | OpenAPI 文档 |
| **Hutool** | 5.8.41 | Java 工具库 |
| **Fastjson2** | 2.0.53 | JSON 处理 |
| **HanLP** | 1.8.4 | 中文分词 |

### 前端技术

| 技术 | 版本 | 说明 |
|------|------|------|
| **Vue.js** | 2.6.11 | 渐进式框架 |
| **Element UI** | 2.15.6 | UI 组件库 |
| **Vue Router** | 3.0.7 | 路由管理 |
| **Vuex** | 3.1.1 | 状态管理 |
| **Axios** | 0.19.0 | HTTP 客户端 |
| **ECharts** | 4.5.0 | 图表库 |
| **Webpack** | 3.6.0 | 模块打包 |

### 开发工具

- **JDK**: 17+
- **Node.js**: 16+
- **Maven**: 3.9+
- **Docker**: 20.10+
- **Docker Compose**: 2.0+

---

## ✨ 功能特性

### 🎯 核心功能

#### 1. 歌曲推荐系统
- ✅ **协同过滤算法** - 基于用户评分相似度推荐
- ✅ **个性化推荐** - 根据用户历史行为智能推荐
- ✅ **实时计算** - 动态更新推荐结果

#### 2. 用户系统
- ✅ **注册登录** - JWT Token 认证,IP 绑定
- ✅ **密码加密** - BCrypt 强加密存储
- ✅ **个人信息** - 头像、签名、地区等
- ✅ **权限管理** - 管理员/普通用户角色区分

#### 3. 音乐管理
- ✅ **歌曲管理** - 上传、编辑、删除歌曲
- ✅ **歌手管理** - 歌手信息维护
- ✅ **歌单管理** - 创建、编辑歌单
- ✅ **歌词显示** - LRC 格式歌词解析

#### 4. 互动功能
- ✅ **歌曲收藏** - 收藏喜欢的歌曲和歌单
- ✅ **评论系统** - 歌曲/歌单评论,点赞功能
- ✅ **评分系统** - 对歌单进行评分
- ✅ **高频词汇** - 评论关键词提取(HanLP)

#### 5. 搜索查询
- ✅ **模糊搜索** - 歌曲名、歌手名模糊查询
- ✅ **分类筛选** - 按性别、风格等条件筛选
- ✅ **分页查询** - PageHelper 高效分页

### 🔒 安全特性

- ✅ **JWT Token 认证** - 无状态认证,支持分布式
- ✅ **IP 地址绑定** - 防止 Token 盗用
- ✅ **Token 黑名单** - Redis 实现主动登出
- ✅ **API 限流** - 登录接口防暴力破解
- ✅ **SQL 注入防护** - MyBatis 参数化查询
- ✅ **CORS 跨域配置** - 安全的跨域策略
- ✅ **密码加密** - BCrypt 单向加密

### ⚡ 性能优化

- ✅ **Redis 缓存** - 热点数据缓存,减少 DB 压力
- ✅ **Druid 连接池** - 高效数据库连接管理
- ✅ **Gzip 压缩** - Nginx 静态资源压缩
- ✅ **静态资源缓存** - 浏览器缓存策略
- ✅ **多阶段构建** - Docker 镜像体积优化 60%+

---

## 🚀 快速开始

### ⭐ 方式一：Docker 部署（推荐，30 秒启动）

**Windows 用户：**
```powershell
.\deploy.ps1 start
```

**Linux/Mac 用户：**
```bash
chmod +x deploy.sh && ./deploy.sh start
```

✅ 自动启动 MySQL、Redis、后端、前端五个服务  
✅ 访问 http://localhost:8080 (管理端) 或 http://localhost:8081 (用户端) 即可使用

📖 详细说明：[DOCKER_QUICK_START.md](DOCKER_QUICK_START.md) | [DOCKER_DEPLOYMENT.md](DOCKER_DEPLOYMENT.md)

---

### 🔧 方式二：手动部署（开发模式）

#### 前置要求

- JDK 17+
- Maven 3.9+
- Node.js 16+
- MySQL 8.0+
- Redis 7.x+

#### 1. 数据库初始化

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE db_music DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 导入数据
mysql -u root -p db_music < db_music.sql
```

修改配置文件中的数据库连接信息：
- `Music-backend/src/main/resources/application.yml`

#### 2. 启动后端

```bash
cd Music-backend
mvn spring-boot:run
```

✅ 后端地址：http://localhost:9171  
✅ 健康检查：http://localhost:9171/actuator/health  
✅ Swagger UI：http://localhost:9171/swagger-ui.html

#### 3. 启动前端

**管理端：**
```bash
cd MusicAdmin-frontend
npm install --registry=https://registry.npmmirror.com
npm run dev
```

✅ 管理端地址：http://localhost:8080

**用户端：**
```bash
cd MusicUser-frontend
npm install --registry=https://registry.npmmirror.com
npm run dev
```

✅ 用户端地址：http://localhost:8081

---

## 📁 项目结构

```
Music/
├── Music-backend/                 # 后端服务 (Spring Boot)
│   ├── src/main/java/
│   │   └── com/shanzhu/music/
│   │       ├── controller/        # 控制层 (9个Controller)
│   │       ├── service/           # 业务层
│   │       ├── mapper/            # 数据访问层
│   │       ├── entity/            # 实体类
│   │       ├── config/            # 配置类
│   │       ├── annotation/        # 自定义注解
│   │       ├── interceptor/       # 拦截器
│   │       └── utils/             # 工具类
│   ├── src/main/resources/
│   │   ├── mapper/                # MyBatis XML
│   │   ├── db/migration/          # Flyway 迁移脚本
│   │   └── application.yml        # 配置文件
│   ├── src/test/                  # 单元测试
│   ├── Dockerfile                 # Docker 镜像配置
│   └── pom.xml                    # Maven 配置
│
├── MusicAdmin-frontend/           # 管理端前端 (Vue 2)
│   ├── src/
│   │   ├── api/                   # API 接口
│   │   ├── pages/                 # 页面组件
│   │   ├── components/            # 公共组件
│   │   ├── router/                # 路由配置
│   │   └── store/                 # Vuex 状态管理
│   ├── Dockerfile                 # Docker 镜像配置
│   ├── nginx.conf                 # Nginx 配置
│   └── package.json
│
├── MusicUser-frontend/            # 用户端前端 (Vue 2)
│   ├── src/
│   │   ├── api/                   # API 接口
│   │   ├── pages/                 # 页面组件
│   │   ├── components/            # 公共组件
│   │   ├── router/                # 路由配置
│   │   └── store/                 # Vuex 状态管理
│   ├── Dockerfile                 # Docker 镜像配置
│   ├── nginx.conf                 # Nginx 配置
│   └── package.json
│
├── docs/                          # 项目文档
│   ├── FRONTEND_BACKEND_MATCH_ANALYSIS.md  # 前后端匹配分析
│   ├── JWT_UNIFICATION_REPORT.md           # JWT 改造报告
│   ├── UNIT_TEST_REPORT.md                 # 单元测试报告
│   └── ...                                  # 其他文档
│
├── docker-compose.yml             # Docker Compose 配置
├── docker-compose.prod.yml        # 生产环境配置
├── deploy.sh                      # Linux/Mac 部署脚本
├── deploy.ps1                     # Windows 部署脚本
├── db_music.sql                   # 数据库脚本
└── README.md                      # 项目说明
```

---

## 📚 API 文档

### Swagger UI

启动后端后访问: http://localhost:9171/swagger-ui.html

📖 完整 API 开发文档：[API 开发文档.md](API%20开发文档.md)

### 主要接口模块

| 模块 | 路径前缀 | 说明 |
|------|---------|------|
| **管理员** | `/admin` | 登录、权限管理 |
| **用户** | `/consumer` | 注册、登录、信息管理 |
| **歌手** | `/singer` | 歌手 CRUD、分页查询 |
| **歌曲** | `/song` | 歌曲 CRUD、文件上传 |
| **歌单** | `/songList` | 歌单 CRUD、推荐算法 |
| **收藏** | `/collect` | 收藏/取消收藏 |
| **评论** | `/comment` | 评论、点赞、高频词 |
| **评分** | `/rank` | 歌单评分 |
| **关联** | `/listSong` | 歌单-歌曲关联 |

### 认证方式

所有需要认证的接口需在 Header 中携带 JWT Token:

```http
Authorization: eyJhbGciOiJIUzI1NiJ9...
```

### 响应格式

统一返回体为 `com.shanzhu.music.common.R`，实际字段与取值如下
（此前文档写的 `code: 1 成功 / 0 失败` 与 `msg` 字段名都与代码不符）：

```json
{
  "success": true,        // 成功为 true；失败响应现在会显式返回 false
  "code": 20000,          // 20000 成功，20001 失败（ResultCodeEnum）
  "message": "操作成功",   // 字段名是 message，不是 msg
  "data": {},             // 数据体
  "dataMap": {}           // 双轨字段之一，R 仍保留，待收敛
}
```

注意两点现状：

1. 并非所有接口都返回 `R`。仍有接口直接返回裸 POJO、裸 `List`、裸 `Boolean`
   甚至 `ModelAndView`，前端需按接口区分处理。
2. `code` 目前存在两种用法混置：`ResultCodeEnum` 的 20000/20001，
   以及个别接口自定义的业务态码（如收藏接口的 `2` 表示"已收藏"、
   鉴权与限流用 401/403/429）。判断成功应以 `success` 为准。

本轮修复影响的完整口径变更见 [docs/FIX-LOG-2026-11.md](docs/FIX-LOG-2026-11.md)。

---

## 🧪 测试说明

后端 142 个用例、前端 24 个用例，实测全部通过（`mvn -o test` 约 1 分 20 秒；
两端 `npm run unit` 各约 10 秒）。数字以 `docs/FIX-LOG-2026-11.md` 第四、六节的实测记录为准。

需要如实说明的边界：

- 后端覆盖以 Controller 与 Service 层为主，推荐算法链路没有断言级测试；
  六路召回与端到端推荐靠真实 MySQL + Redis 探针验证，不在 `mvn test` 内
- 管理端没有管理员账号的增删改查接口，对应位置不留占位测试
- 前端测试针对纯逻辑（歌词解析、store 回读语义、mixin 纯函数），
  不含浏览器端 UI 验证；`npm run build` 两端均能产出 dist，
  但 Docker 镜像内的构建过程未实机验证（详见 FIX-LOG 第七节）

| 层级 | 测试数量 | 通过率 |
|------|---------|--------|
| R 工具类 | 13 个 | ✅ 100% |
| Service 层 | 72 个 | ✅ 100% |
| Controller 层 | 56 个 | ✅ 100% |
| **总计** | **141 个** | **✅ 100%** |

**测试特点：**
- 🎯 使用 JUnit 5 + Spring Boot Test 框架
- 💾 H2 内存数据库隔离测试环境
- 🔧 自动清理测试数据，保证独立性
- 📊 覆盖正常场景、异常场景、边界条件

**快速运行：**
```bash
cd Music-backend
mvn test                              # 运行所有测试
mvn test -Dtest=RTest                # 运行特定测试类
mvn surefire-report:report           # 生成 HTML 报告
```

📖 详细文档：[docs/UNIT_TEST_REPORT.md](docs/UNIT_TEST_REPORT.md)

---

## 🔒 安全机制

- **JWT Token 认证**：登录后获取 Token，后续请求在 Header 中携带
- **BCrypt 密码加密**：使用业界标准的 BCrypt 强哈希算法，内置盐值机制，抗暴力破解
- **IP 绑定机制**：Token 与客户端 IP 绑定，防止多设备同时登录
- **API 限流保护**：登录接口 60 秒最多 5 次尝试，防止恶意攻击
- **Token 黑名单**：Redis 实现主动登出，支持立即失效
- **CORS 跨域配置**：已配置全局跨域支持
- **SQL 注入防护**：MyBatis 参数化查询，防止 SQL 注入
- **异常处理**：统一异常拦截和响应格式

### JWT Token 详解

`JwtUtils.java` 是系统的核心工具类，负责 Token 的生成、验证和管理：

**主要功能：**

1. **Token 生成** (`generateToken`)：
   - 载荷数据：`userId`, `username`, `userType`, `ip`
   - 签名密钥：自定义 Secret Key
   - 过期时间：24 小时
   - IP 绑定：将 Token 和客户端 IP 存入 Redis

2. **IP 验证** (`verifyIp`)：
   - 从 Redis 获取 Token 绑定的 IP
   - 比对当前请求 IP
   - IP 不匹配则拒绝访问

3. **Token 验证** (`validateToken`)：
   - 验证 Token 格式
   - 验证有效期
   - 验证签名

4. **获取用户信息** (`getUserIdFromToken`, `getUsernameFromToken`)：
   - 从 Token 解析用户 ID
   - 从 Token 解析用户名

**Redis Key 格式：**
- Token 绑定：`jwt:blacklist:{token}` → true
- 登录频率：`login:admin:{ip}` / `login:user:{ip}` → 次数

📖 详细说明请参考：[docs/JWT_UNIFICATION_REPORT.md](docs/JWT_UNIFICATION_REPORT.md)

### 密码安全说明

系统使用 **BCrypt** 加密密码：

✅ **安全性提升**：BCrypt 专为密码存储设计，比 MD5 更安全  
✅ **自动加盐**：每次生成的哈希值都不同，防止彩虹表攻击  
✅ **向后兼容**：新注册用户自动使用 BCrypt，无需额外配置

📖 详细说明请参考：[docs/BCRYPT_PASSWORD_ENCRYPTION.md](docs/BCRYPT_PASSWORD_ENCRYPTION.md)

### API 限流说明

为防止暴力破解和恶意登录，系统实施了 API 限流：

⚡ **限制规则**：登录接口 60 秒内最多 5 次尝试  
💬 **友好提示**：超出限制时会显示“登录尝试次数过多，请60秒后再试”  
🔄 **自动解除**：60 秒后自动解除限制  
❌ **失败计数**：登录失败也会触发限流

📖 详细说明请参考：[docs/JWT_AND_RATELIMIT_GUIDE.md](docs/JWT_AND_RATELIMIT_GUIDE.md)

---

---

## 🐳 部署

### Docker 部署 (推荐)

```bash
# 开发环境
docker-compose up -d

# 生产环境
cp .env.example .env
# 编辑 .env 配置密码
docker-compose -f docker-compose.prod.yml up -d
```

**服务端口**:
- 管理端: `8080`
- 用户端: `8081`
- 后端 API: `9171`
- MySQL: `3306`
- Redis: `6379`

详细文档: [Docker 部署指南](DOCKER_DEPLOYMENT.md) | [Docker 快速开始](DOCKER_QUICK_START.md)

### 传统部署

1. **后端**: 打包为 JAR,使用 `java -jar` 运行
2. **前端**: `npm run build` 生成静态文件,部署到 Nginx
3. **数据库**: 导入 `db_music.sql`,配置连接信息

---

## 💻 开发指南

### 分支策略

- `main` - 主分支,稳定版本
- `develop` - 开发分支
- `feature/*` - 功能分支
- `hotfix/*` - 修复分支

### 代码规范

- **后端**: 遵循阿里巴巴 Java 开发手册
- **前端**: 遵循 Vue.js 风格指南
- **提交信息**: 使用 Conventional Commits 规范

### 常用命令

```bash
# 后端
mvn clean compile          # 编译
mvn clean package          # 打包
mvn spring-boot:run        # 运行
mvn test                   # 测试

# 前端
npm install                # 安装依赖
npm run dev                # 开发模式
npm run build              # 生产构建
npm run lint               # 代码检查
```

---

## ❓ 常见问题

### 1. Docker 端口被占用

修改 `docker-compose.yml` 中的端口映射：
```yaml
ports:
  - "8082:80"      # 将管理端端口改为 8082
  - "8083:80"      # 将用户端端口改为 8083
  - "9172:9171"    # 将后端端口改为 9172
```

### 2. 数据库连接失败

检查配置文件 `Music-backend/src/main/resources/application.yml` 中的数据库连接信息：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/db_music
    username: root
    password: your_password
```

### 3. Redis 连接失败

确保 Redis 服务已启动，并检查配置：
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: admin  # 如果有密码
```

### 4. 前端无法访问后端 API

检查前端配置文件中的 API 地址：
- 管理端：`MusicAdmin-frontend/src/api/http.js`
- 用户端：`MusicUser-frontend/src/api/http.js`

确保 `baseURL` 指向正确的后端地址。

### 5. 图片上传后显示 localhost

确保配置文件中设置了正确的服务器地址：
```yaml
server:
  address: 0.0.0.0
```

更多问题请参考：[docs/](docs/) 目录下的相关文档

---

## 🤝 贡献指南

欢迎贡献代码!请遵循以下步骤:

1. Fork 本仓库
2. 创建特性分支：`git checkout -b feature/AmazingFeature`
3. 提交更改：`git commit -m 'Add some AmazingFeature'`
4. 推送分支：`git push origin feature/AmazingFeature`
5. 提交 Pull Request

### 开发规范

- **后端**：遵循阿里巴巴 Java 开发手册
- **前端**：遵循 Vue.js 风格指南
- **提交信息**：使用 Conventional Commits 规范

---

1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

---

## 📞 联系方式

- **项目作者**: ShanZhu
- **GitHub**: [ZapcoMan/Music](https://github.com/ZapcoMan/Music)
- **问题反馈**: [Issues](https://github.com/ZapcoMan/Music/issues)

---

## 📄 许可证

本项目基于 [Apache License 2.0](LICENSE) 授权，完整条款见仓库根目录的 `LICENSE` 文件。

```
Copyright 2024-2026 ZapcoMan

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---

<div align="center">

**⭐ 如果这个项目对你有帮助，请给一个 Star!**

Made with ❤️ by ShanZhu

*最后更新时间：2026-11-07*

</div>
