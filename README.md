# 🎵 音乐网站 (Music Website)

<div align="center">

![Version](https://img.shields.io/badge/Version-v3.0.0-informational)
![CI](https://github.com/ZapcoMan/Music/actions/workflows/ci.yml/badge.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-brightgreen)
![Vue](https://img.shields.io/badge/Vue-2.6.11-4FC08D)
![MySQL](https://img.shields.io/badge/MySQL-8.0-orange)
![Tests](https://img.shields.io/badge/Tests-297%20passed%2C%200%20skipped-brightgreen)
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
- 🎯 **推荐引擎** - 六路召回（协同过滤/内容/个性化/标签/搜索历史/热门）+ 特征精排，参数配置驱动
- 🏷️ **结构化标签系统** - tag/song_tag/singer_tag 中间表为唯一真源，支持标签维护与热门标签查询
- 🔍 **搜索历史** - 按令牌归属记录/读取/清空，驱动召回并支持服务端关键词过滤
- 💾 **Redis 缓存优化** - 缓存键统一收敛于 `CacheKeys`，延时双删保证一致性
- 📊 **Swagger API 文档** - SpringDoc OpenAPI 在线接口文档和测试
- 🧪 **完整测试覆盖**：后端 297 个用例 + 前端 24 个用例，全部通过（Skipped 0）
- 📈 **可观测性** - traceId 贯穿请求与异步日志，Actuator + Prometheus 指标
- 🐳 **Docker 一键部署**：容器化部署，环境变量必填、缺配置即拒绝启动
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
         │ • JWT 认证 + 角色守卫   │
         │ • BCrypt 密码加密      │
         │ • API 限流 + 审计日志   │
         │ • 六路召回 + 特征精排   │
         │ • Swagger / Prometheus │
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

✅ **六路召回 + 特征精排** - 协同过滤/内容/个性化/标签/搜索历史/热门，权重与算法参数经 `recommendation.*` 配置驱动  
✅ **JWT 统一认证** - 无状态 Token 认证，签发方/受众校验，IP 绑定，角色默认拒绝  
✅ **BCrypt 密码加密** - Spring Security 强哈希（强度 12），口令校验始终问数据库  
✅ **API 限流保护** - 10 个敏感接口注解式限流，Redis 故障按敏感度降级  
✅ **Redis 缓存优化** - 键前缀收敛于 `CacheKeys`，延时双删 + 事务 afterCommit  
✅ **Swagger API 文档** - SpringDoc OpenAPI 在线接口文档和测试  
✅ **Docker 容器化** - 一键部署，compose 密钥变量必填、缺失即拒绝启动  
✅ **完整测试与质量门禁** - JUnit 5 用例 297 项全绿，JaCoCo 四项门禁 + SpotBugs High=0  

---

## 🛠️ 技术栈

### 后端技术

| 技术 | 版本 | 说明 |
|------|------|------|
| **Spring Boot** | 3.4.3 | 核心框架 |
| **MyBatis** | 3.0.4 | ORM 框架 |
| **PageHelper** | 2.1.0 | 分页插件（每页上限统一在 `common/PageLimits`） |
| **MySQL** | 8.0 | 关系数据库 |
| **Redis** | 7.x | 缓存数据库 |
| **Druid** | 1.2.23 | 数据库连接池 |
| **Flyway** | Boot BOM | 数据库迁移（库结构唯一出处：`db/migration`，现为 V1–V13） |
| **Spring Security** | Boot BOM | 仅提供 BCrypt 加密与 CORS 配置源 |
| **JWT (jjwt)** | 0.12.6 | Token 认证（含签发方/受众校验） |
| **Micrometer Prometheus** | Boot BOM | `/actuator/prometheus` 指标导出 |
| **SpringDoc** | 2.8.5 | OpenAPI 文档 |
| **Hutool** | 5.8.41 | Java 工具库 |
| **Fastjson2** | 2.0.53 | JSON 处理 |
| **HanLP** | portable-1.8.4 | 中文分词（评论高频词） |
| **Lombok** | 1.18.36 | 样板代码消除 |

### 前端技术

| 技术 | 版本 | 说明 |
|------|------|------|
| **Vue.js** | 2.5.2 ~ 2.6.11 | 渐进式框架（用户端 2.6.11、管理端 2.5.2） |
| **Element UI** | 2.15.6 / 2.3.3 | UI 组件库（用户端 / 管理端） |
| **Vue Router** | 3.0.7 / 3.0.1 | 路由管理 |
| **Vuex** | 3.0.1 / 3.1.1 | 状态管理 |
| **Axios** | 0.18 / 0.19 | HTTP 客户端（用户端仍为 0.18，属已知依赖陈旧项） |
| **ECharts / v-charts** | 4.5.0 / 1.19.0 | 图表库（仅管理端） |
| **Webpack** | 3.6.0 | 模块打包（vue-cli 2 工具链 + dart-sass/sass-loader 7.3.1，组合自洽但大版本落后） |

### 质量工具

| 工具 | 版本 | 说明 |
|------|------|------|
| **JaCoCo** | 0.8.12 | 覆盖率门禁：CLASS 90% / METHOD 80% / LINE 70% / BRANCH 50% |
| **SpotBugs** | 4.8.6.4 | 绑定 `verify` 阶段，`threshold=High` 报告 0 项 |
| **ESLint** | - | 两端 `plugin:vue/strongly-recommended` 档位，0 error |
| **CI** | GitHub Actions | `ci.yml`（后端 verify + 真实 MySQL 迁移链）、`frontend-ci.yml`（两端 lint/unit/build） |

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
- ✅ **六路召回** - 协同过滤 0.40 / 内容 0.25 / 个性化 0.20 / 标签 0.20 / 搜索历史 0.20 / 热门 0.15
- ✅ **特征精排** - 10 个排序特征 + FM 二阶交叉，新歌分档加性增益
- ✅ **配置驱动** - 权重、量纲上界、FM 系数均可在 `recommendation.*` 调整
- ✅ **结果缓存** - 推荐结果入 Redis（`CacheEntry` 包装，修复过 Jackson 数组类型标记无法回读的问题）

#### 2. 用户系统
- ✅ **注册登录** - JWT Token 认证，IP 绑定，登录/登出/注册走 `/api/v1/.../sessions` 资源式路径
- ✅ **密码加密** - BCrypt（强度 12）强哈希存储，实体 `@JsonIgnore` 防止哈希外泄
- ✅ **个人信息** - 完整私密资料由 `GET /api/v1/consumers/profile` 提供，身份只取自令牌
- ✅ **权限管理** - `@RequireUserType` 角色域声明，未声明的端点默认 403（fail-closed）

#### 3. 音乐管理
- ✅ **歌曲管理** - 上传、编辑、删除歌曲（写接口 RESTful 化，部分更新用 PATCH）
- ✅ **歌手管理** - 歌手信息维护，删除时按歌手批量级联清理歌曲
- ✅ **歌单管理** - 创建、编辑歌单，歌单-歌曲关联为子资源 `/api/v1/song-lists/{id}/songs`
- ✅ **歌词显示** - LRC 格式歌词解析
- ✅ **文件上传** - 六处上传统一收进 `FileStorageService`：扩展名白名单 + 文件头签名核对 + 服务端生成文件名

#### 4. 互动功能
- ✅ **歌曲/歌单收藏** - 收藏与取消收藏，身份从令牌取；歌单收藏（type=1）读写两端已接通
- ✅ **评论系统** - 歌曲/歌单评论；评论发布后不可修改（产品口径，撤回等于删了重发）；点赞为数据库原子自增
- ✅ **评分系统** - 1–10 分制评歌单，重复提交更新既有行而非报错
- ✅ **高频词汇** - 评论关键词提取（HanLP）

#### 5. 搜索与查询
- ✅ **模糊搜索** - 歌曲名、歌手名模糊查询；管理端用户/歌手列表支持 `?keyword=` 服务端过滤
- ✅ **搜索历史** - 写入/读取/清空/过期清理全部接通，接口不接收 userId
- ✅ **分类筛选** - 按性别、风格等条件筛选（集合过滤用查询参数）
- ✅ **分页查询** - PageHelper 分页，每页上限统一在 `common/PageLimits` 夹住

#### 6. 标签与统计
- ✅ **标签维护** - `PUT /api/v1/songs/{id}/tags`、`PUT /api/v1/singers/{id}/tags`（整组替换）、`POST /api/v1/tags`
- ✅ **热门标签** - `GET /api/v1/tags/hot`、`GET /api/v1/tags/songs`
- ✅ **全站统计** - `GET /api/v1/statistics/overview`（仅 admin），SQL 侧 count/group by 聚合

### 🔒 安全特性

- ✅ **JWT Token 认证** - 无状态认证；校验签名同时强制签发方/受众（iss/aud），防同密钥他服务令牌串用
- ✅ **密钥强制注入** - prod 无默认 `JWT_SECRET`，缺失即启动失败
- ✅ **IP 地址绑定** - 防止 Token 盗用；代理透传头默认不采信（`security.ip.trust-proxy-headers`）
- ✅ **Token 黑名单** - Redis 实现主动登出，TTL 与令牌剩余有效期对齐；Redis 故障时 fail-open 并记 error
- ✅ **API 限流** - 10 个敏感接口分档限流，计数原子化（INCR）；Redis 故障时拒绝型限流返回 503
- ✅ **角色默认拒绝** - 全部业务端点（含读接口）必须声明 `@RequireUserType`，漏标即 403，守卫测试扫描全部端点
- ✅ **SQL 注入防护** - MyBatis 参数化查询 + Druid wall 过滤器
- ✅ **CORS 跨域配置** - 单一出处（SecurityConfig），精确来源白名单 + 显式枚举请求头
- ✅ **凭据不落盘** - 令牌只认 Authorization 头（`?token=` 回落已关），查询串进日志前按键名脱敏
- ✅ **密码加密** - BCrypt 单向加密（强度 12）

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

库结构唯一出处是 `Music-backend/src/main/resources/db/migration` 下的 Flyway 脚本（现为 V1–V13）：
首次启动时由 Flyway 自动建库迁移，**无需手工导入 SQL**。

> 根目录的 `db_music.sql` 在 v3.0.0 后已停用（compose 已摘掉 `/docker-entrypoint-initdb.d` 挂载），
> 仅作历史参考；恢复数据请走 `mysqldump` 现场备份。
> 对已有数据的库不要直接靠配置兼容——dev 配置 `baseline-on-migrate: false`，这种环境会启动失败而非静默重放迁移。

修改配置文件中的数据库连接信息：
- `Music-backend/src/main/resources/application-dev.yml`（本地默认值可被环境变量覆盖）

#### 2. 启动后端

```bash
cd Music-backend
# 生产/联调环境必须提供 JWT_SECRET；dev profile 有明确标注的占位串，仅供本地起服务
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
│   │       ├── controller/        # 控制层 (12 个 Controller，接口已整体 RESTful 化到 /api/v1)
│   │       ├── service/           # 业务层
│   │       │   ├── ranking/       # 特征精排（RankingService / FeatureEngineering / GBDT / 新歌扶持）
│   │       │   └── recall/        # 六路召回策略与融合（RecallStrategy / RecallFusionService）
│   │       ├── mapper/            # 数据访问层 (13 个 Mapper)
│   │       ├── entity/            # 实体类（po / dto / vo，如 RecallResult、ConsumerLoginVo）
│   │       ├── common/            # R / ResultCodeEnum / CacheKeys / PageLimits 等统一口径
│   │       ├── config/            # 配置类（Security/WebMvc/Redis/OpenApi/Async/File 等）
│   │       ├── annotation/        # @RateLimit、@RequireUserType
│   │       ├── interceptor/       # JWT 认证 / 限流 / 请求响应日志拦截器
│   │       ├── exception/         # GlobalExceptionHandler（含 405/415/400/404 协议层处理）
│   │       └── utils/             # JwtUtils / IpUtils / RedisUtils / PasswordUtils 等
│   ├── src/main/resources/
│   │   ├── mapper/                # MyBatis XML
│   │   ├── db/migration/          # Flyway 迁移脚本（V1–V13，库结构唯一出处）
│   │   └── application*.yml       # 主配置 + dev/test/prod 环境配置
│   ├── src/test/                  # 单元测试（H2 + MockMvc，BaseTest 统一预置令牌与清缓存）
│   ├── spotbugs-exclude.xml       # SpotBugs 精准排除（仅 Lombok @Data 的 EI_EXPOSE_REP/REP2）
│   ├── Dockerfile                 # Docker 镜像配置
│   └── pom.xml                    # Maven 配置（JaCoCo / Surefire / SpotBugs / build-info）
│
├── MusicAdmin-frontend/           # 管理端前端 (Vue 2)
│   ├── src/
│   │   ├── api/                   # API 接口（http.js 从 VUE_APP_API_BASE_URL 派生 HOST/API_HOST）
│   │   ├── pages/                 # 页面组件
│   │   ├── components/            # 公共组件
│   │   ├── router/                # 路由配置（含路由守卫）
│   │   └── store/                 # Vuex 状态管理
│   ├── config/load-env.js         # webpack3 链路的 .env 读取（零依赖）
│   ├── Dockerfile                 # 非 root nginx-unprivileged，容器内监听 8080
│   ├── nginx.conf                 # Nginx 配置（拦截 .map，生产无 source map）
│   └── package.json
│
├── MusicUser-frontend/            # 用户端前端 (Vue 2)，结构同上
│
├── docs/                          # 项目文档
│   ├── FIX-LOG-2026-11.md         # ★ 当前真实口径的唯一出处（与其它文档冲突时以它和代码为准）
│   ├── 01-AUTHENTICATION.md       # JWT 认证
│   ├── 02-RATE-LIMITING.md        # API 限流
│   ├── 03-LOGGING.md              # 日志体系
│   ├── 04-REQUEST-AUDIT.md        # 请求/响应审计
│   ├── 05-PARAM-VALIDATION.md     # 参数校验
│   ├── 06-CODE-COVERAGE.md        # JaCoCo 覆盖率门禁
│   ├── RECALL_AND_SEARCH_HISTORY_FEATURE.md  # 召回与搜索历史
│   ├── TAG_SYSTEM_OPTIMIZATION.md / REDIS_*.md / DOUBLE_DELETE_STRATEGY.md 等专题
│   └── UNIT_TEST_REPORT.md        # 单测报告（历史快照，现状见顶部口径声明）
│
├── .github/workflows/             # CI：ci.yml（后端+迁移链）/ frontend-ci.yml（前端）
├── docker-compose.yml             # Docker Compose 配置（密钥变量 ${VAR:?} 必填）
├── docker-compose.prod.yml        # 生产环境配置
├── deploy.sh / deploy.ps1         # 部署脚本（Linux-Mac / Windows）
├── db_music.sql                   # 已停用的历史 dump（见上文说明）
└── README.md                      # 项目说明
```

---

## 📚 API 文档

### Swagger UI

启动后端后访问: http://localhost:9171/swagger-ui.html

📖 完整 API 开发文档：[API 开发文档.md](API%20开发文档.md)

### 主要接口模块

自 v2.0.0 起全部业务接口挂在 `/api/v1` 之下，动词式路径已改资源式（旧路径无兼容层，调用会 404）：

| 模块 | 路径 | 说明 |
|------|---------|------|
| **管理员** | `POST/DELETE /api/v1/admin/sessions` | 登录 / 登出（登出真正吊销令牌） |
| **用户** | `/api/v1/consumers` | 注册（`/registrations`）、登录会话、资料（`/profile`）、分页（`/page?keyword=`） |
| **歌手** | `/api/v1/singers` | 歌手 CRUD、分页，部分更新用 PATCH |
| **歌曲** | `/api/v1/songs` | 歌曲 CRUD、文件上传 |
| **歌单** | `/api/v1/song-lists` | 歌单 CRUD、`?style=` 筛选 |
| **歌单-歌曲关联** | `/api/v1/song-lists/{songListId}/songs` | 子资源式关联管理 |
| **收藏** | `/api/v1/collections` | 收藏/取消收藏，身份从令牌取；歌单收藏走 `/collections/songList` |
| **评论** | `/api/v1/comments`、`/api/v1/songs/{songId}/comments` 等 | 发布/删除/查询/点赞；发布后不可修改 |
| **评分** | `POST /api/v1/ranks`、`GET /api/v1/song-lists/{songListId}/rank` | 提交歌单评分 / 查该歌单均分 |
| **标签** | `/api/v1/tags`、`/api/v1/songs/{id}/tags`、`/api/v1/singers/{id}/tags` | 标签字典、热门标签、整组替换（PUT） |
| **搜索历史** | `/api/v1/search-history` | 记录/读取/清空，仅 consumer 角色，不接收 userId |
| **统计** | `/api/v1/statistics/overview` | 全站概览（仅 admin） |

> **静态资源是例外**：`/song/*`、`/img/**`、`/userImages/**` 不带 `/api/v1` 前缀。
> 因为 `<audio>`/`<img>` 发不出 Authorization 头，且库里历史 URL 就是 `/song/1715868205989` 这种形态；
> 反向代理改写路径时不要动这三条。

### 认证方式

所有需要认证的接口需在 Header 中携带 JWT Token（**只认头，不支持 `?token=` 查询参数**）：

```http
Authorization: eyJhbGciOiJIUzI1NiJ9...
```

未携带/无效 Token 返回 401；角色不匹配或接口未声明角色域返回 403（默认拒绝）。

### 响应格式

统一返回体为泛型 `com.shanzhu.music.common.R<T>`（v2.0.0 已消除裸 POJO/裸 List 返回，
并删除了 `dataMap` 第二条数据载体）：

```json
{
  "success": true,        // 成功为 true，失败为 false——判断成功应以它为准
  "code": 20000,          // 20000 成功，20001 失败，20002 参数错误（ResultCodeEnum）
  "message": "操作成功",   // 字段名是 message，不是 msg
  "data": {}              // 数据载荷（R<T> 的泛型参数）
}
```

HTTP 状态码与 body 里的 code 两层各自表意：鉴权/限流用 401/403/429/503，
参数错误 HTTP 400 + code 20002；而业务失败（如“用户名已存在”）仍返回 HTTP 200 + `success=false`。
收藏等个别接口历史上另有自定义态码，新接口不应再扩展这种用法。

本轮修复影响的完整口径变更见 [docs/FIX-LOG-2026-11.md](docs/FIX-LOG-2026-11.md)，版本级摘要见 [CHANGELOG.md](CHANGELOG.md)。

---

## 🧪 测试说明

后端 297 个用例、前端 24 个用例（用户端 13 + 管理端 11），实测全部通过
（`mvn -B clean verify` 含 JaCoCo/SpotBugs 门禁；两端 `npm run unit` 各约 10 秒）。
数字以 [CHANGELOG.md](CHANGELOG.md) 的 v3.0.0「验证」节与 `docs/FIX-LOG-2026-11.md` 的实测记录为准。

| 维度 | 实测水位 | 门禁（pom 中按实测留余量设定） |
|------|---------|--------|
| 类覆盖 | CLASS 97.3% | ≥ 90% |
| 方法覆盖 | METHOD 86.2% | ≥ 80% |
| 行覆盖 | LINE 76.7% | ≥ 70% |
| 分支覆盖 | BRANCH 56.5% | ≥ 50% |

需要如实说明的边界：

- 后端覆盖以 Controller 与 Service 层为主；六路召回与端到端推荐靠真实 MySQL + Redis 探针验证，
  不在 `mvn test` 内
- 管理端没有管理员账号的增删改查接口（AdminMapper 只有 verifyPassword/getByName），对应位置不留占位测试
- 前端测试针对纯逻辑（歌词解析、store 回读语义、mixin 纯函数），不含浏览器端 UI 验证；
  两端 `npm run build` 已实跑通过且产物无 `.map`，但 Docker 镜像内的构建过程未实机验证
- 种子口令哈希的明文未记录在仓库任何位置，“管理员用正确口令登录”在无凭据环境下无法断言

**测试特点：**
- 🎯 使用 JUnit 5 + Spring Boot Test 框架，三种跑法（反序字母序/默认序/clean verify）全绿后才定版
- 💾 H2 内存数据库隔离测试环境，`BaseTest` 每个用例前清 `music:*` 缓存并预置令牌
- 🔧 系统性清除假绿：业务失败也返 200，断言必须落在 `data` 内容/回查数据库/越权后行仍在
- 📊 管理接口鉴权矩阵（端点 × 匿名/消费者/管理员）+ 端点角色声明守卫扫描全部端点

**快速运行：**
```bash
cd Music-backend
mvn test                              # 运行所有测试
mvn -B clean verify                   # 测试 + 覆盖率门禁 + SpotBugs（与 CI 同一条命令）
mvn test -Dtest=RTest                # 运行特定测试类
mvn surefire-report:report           # 生成 HTML 报告
```

📖 详细文档：[docs/UNIT_TEST_REPORT.md](docs/UNIT_TEST_REPORT.md)

---

## 🔒 安全机制

- **JWT Token 认证**：登录后获取 Token，后续请求在 Header 中携带；校验签名 + 过期 + 签发方/受众
- **BCrypt 密码加密**：强度 12，内置盐值；口令校验始终问数据库（缓存里无密码字段，第二次登录被拒的缺陷已修）
- **IP 绑定机制**：Token 与客户端 IP 绑定；透传头默认不采信，防伪造绕过
- **API 限流保护**：登录/注册 60 秒最多 5 次，评论/点赞等分档限流
- **Token 黑名单**：Redis 实现主动登出，TTL 与令牌剩余有效期对齐
- **角色默认拒绝**：全部业务端点必须声明 `@RequireUserType`，漏标即 403
- **CORS 跨域配置**：单一出处 SecurityConfig，精确来源白名单
- **SQL 注入防护**：MyBatis 参数化查询 + Druid wall 过滤器
- **审计日志**：operation_log 落库（登录/注册/登出等），保留期 `audit.log.retention-days` 默认 90 天
- **异常处理**：GlobalExceptionHandler 统一拦截，405/415/400/404 各归其位

### JWT Token 详解

`JwtUtils.java` 是系统的核心工具类，负责 Token 的生成、验证和管理：

**主要功能：**

1. **Token 生成** (`generateToken`)：
   - 载荷数据：`sub`(username)、`userId`、`userType`、`ip`，另含 `iss`/`aud`/`iat`/`exp`
   - 签名密钥：来自 `${jwt.secret}`，**prod 无默认值，缺失即启动失败**
   - 签名算法：由密钥长度决定（jjwt 自动选择，本项目密钥强度 ≥ 448bit，实际 HMAC-SHA384）
   - 过期时间：`jwt.expiration`，默认 24 小时；`getExpirationSeconds()` 派生出参，不再双来源
   - IP 绑定：将当前请求 IP 写入 Token 载荷，每次请求比对

2. **身份与受众校验** (`getClaimsFromToken`)：
   - 除签名与有效期外，强制 `requireIssuer("music-backend")` 与 `requireAudience("music-clients")`，
     防止同一密钥在他服务签发的令牌在本系统被当作合法凭证

3. **认证拦截** (`JwtAuthenticationInterceptor`)：
   - 匿名入口按（方法+路径）精确放行：`POST /api/v1/admin/sessions`、`POST /api/v1/consumers/sessions`、`POST /api/v1/consumers/registrations`
   - 其余接口无 Token 一律 401；IP 不匹配 403；黑名单命中 401
   - 黑名单读取在 Redis 故障时 fail-open（已登出令牌仍受签名/过期约束，窗口有界）但记 error 级日志以便告警
   - 未声明角色域的 Controller 方法 fail-closed 返回 403

4. **登出** (`DELETE /api/v1/{admin|consumers}/sessions`)：
   - 将当前令牌写入 `jwt:blacklist:{token}`，TTL = `getRemainingSeconds(token)`，与令牌自身有效期对齐

**Redis Key 格式（唯一出处 `common/CacheKeys`）：**
- 业务缓存：`music:song:` / `music:singer:` / `music:songlist:` / `music:list_song:` / `music:collect:` / `music:comment:` / `music:consumer:` / `music:recommendation:user:`
- 令牌黑名单：`jwt:blacklist:{token}` → true
- 限流计数：`rate:{注解前缀}{user:userId | ip:ip}`（不拼原始 URI，防 URL 变体绕过；登录为 `login:admin:` / `login:consumer:`）

📖 详细说明请参考：[docs/01-AUTHENTICATION.md](docs/01-AUTHENTICATION.md)

### 密码安全说明

系统使用 **BCrypt** 加密密码：

✅ **安全性提升**：BCrypt 专为密码存储设计，比 MD5 更安全  
✅ **自动加盐**：每次生成的哈希值都不同，防止彩虹表攻击  
✅ **向后兼容**：新注册用户自动使用 BCrypt，无需额外配置

📖 详细说明请参考：[docs/BCRYPT_PASSWORD_ENCRYPTION.md](docs/BCRYPT_PASSWORD_ENCRYPTION.md)（文中接口路径为改造前快照，现状路径见上方 API 文档节）

### API 限流说明

为防止暴力破解和恶意请求，系统对敏感接口实施注解式限流（`@RateLimit`，现共 10 处）：

⚡ **限制规则**：登录/注册/头像上传 60 秒最多 5 次；评论发布、资料修改 10 次；评论删除、用户删除 20 次；点赞 30 次  
🔢 **计数原子化**：INCR + 首次设过期，并发下不会被“get→判空→set(1)”直接绕过  
👤 **限流主体**：已登录请求按用户维度计数（避免同一 NAT 出口下全楼共用计数器），匿名请求按 IP  
💬 **友好提示**：超出限制返回 HTTP 429 + “登录尝试次数过多，请60秒后再试”等提示  
🛡 **故障降级**：Redis 不可达时，默认拒绝型限流返回 503（爆破闸门不因缓存故障默默解除）；读接口可显式 `failOpen=true` 放行  
🔄 **自动解除**：时间窗口到期自动解除；测试基类按 `rate:*` 一次清零所有计数器

📖 详细说明请参考：[docs/02-RATE-LIMITING.md](docs/02-RATE-LIMITING.md)

---

---

## 🐳 部署

### Docker 部署 (推荐)

```bash
# 开发环境
docker-compose up -d

# 生产环境
cp .env.example .env
# 编辑 .env：MYSQL_ROOT_PASSWORD / REDIS_PASSWORD / JWT_SECRET 均为必填，缺任一个 compose 直接拒绝启动
docker-compose -f docker-compose.prod.yml up -d
```

**服务端口**:
- 管理端: `8080`
- 用户端: `8081`
- 后端 API: `9171`
- MySQL: `3306`、Redis: `6379`（v3.0.0 起 compose **不再向宿主机暴露** 3306/6379，仅供容器内访问）

补充口径：
- 前端镜像以非 root 运行（`nginxinc/nginx-unprivileged`），容器内监听 8080，宿主端口映射不变
- 数据库初始化唯一出处是 Flyway 迁移脚本，compose 不再挂载根目录 `db_music.sql`

详细文档: [Docker 部署指南](DOCKER_DEPLOYMENT.md) | [Docker 快速开始](DOCKER_QUICK_START.md)

### 传统部署

1. **后端**: `mvn -B clean package` 打包为 JAR，使用 `java -jar` 运行（需先配好环境变量）
2. **前端**: `npm run build` 生成静态文件，部署到 Nginx（`.env.production` 的 `VUE_APP_API_BASE_URL` 控制后端地址）
3. **数据库**: 建一个空库 `db_music`（utf8mb4）即可，表结构由 Flyway 首次启动自动迁移

---

## 💻 开发指南

### 分支策略

仓库自 2026-11-08 起**只保留 `master` 一个分支**（远端 `main` 已删除，`origin/HEAD` 指向 `master`）：

- `master` - 唯一长期分支，每次发布打附注 tag（如 `v3.0.0`）
- `feature/*`、`hotfix/*` - 临时分支，合入后即删

注意：CI 触发分支与 GitHub 默认分支必须同步修改，不一致时的症状不是 CI 失败而是“推了但一条 run 都没有”。

### 代码规范

- **后端**: 遵循阿里巴巴 Java 开发手册，`mvn -B clean verify`（含 SpotBugs High 门禁）不绿不提交
- **前端**: ESLint `plugin:vue/strongly-recommended` 档位，两端 0 error
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

确保 Redis 服务已启动，并检查配置（口令无默认兜底，必须与环境变量一致）：
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: ${REDIS_PASSWORD}  # 必填，无默认值
```

### 4. 前端无法访问后端 API

前端只有一个地址来源：环境变量 `VUE_APP_API_BASE_URL`（提供 origin），
`src/api/http.js` 从它派生 `HOST`（静态资源）与 `API_HOST`（业务接口 `/api/v1`）：
- 管理端：`MusicAdmin-frontend/.env.development` / `.env.production`
- 用户端：`MusicUser-frontend/.env.development` / `.env.production`

不要直接改 `http.js` 里的硬编码地址；构建链路（webpack3）也已通过 `config/load-env.js` 接通 `.env`。

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
- **前端**：ESLint strongly-recommended 档位，提交前跑通 `npm run lint` / `npm run unit`
- **提交信息**：使用 Conventional Commits 规范

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

*最后更新时间：2026-11-10（对应 v3.0.0）*

</div>
