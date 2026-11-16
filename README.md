# 🎵 音乐网站 (Music Website)

<div align="center">

![Version](https://img.shields.io/badge/Version-3.0.0%20unreleased-informational)
![CI](https://github.com/ZapcoMan/Music/actions/workflows/ci.yml/badge.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-brightgreen)
![Vue](https://img.shields.io/badge/Vue-2.7.x-4FC08D)
![MySQL](https://img.shields.io/badge/MySQL-8.0-orange)
![Tests](https://img.shields.io/badge/Tests-298%20cases-informational)
![License](https://img.shields.io/badge/License-Apache%202.0-red)

**基于 Spring Boot 3 + Vue 2 的音乐推荐系统（前后端分离）**

[快速开始](#-快速开始) • [接口约定](#-接口约定) • [测试与门禁](#-测试与质量门禁) • [文档索引](#-文档索引)

</div>

---

## 📖 项目简介

一个功能完整的全栈音乐网站：曲库维护、歌单与收藏、评论互动、评分统计，以及配置驱动的个性化推荐。
后端提供 12 个 Controller / 74 个 REST 端点，前端分为管理端与用户端两个独立工程。

**核心能力**

- 🎯 **推荐引擎** — 六路召回（协同过滤 / 内容 / 个性化 / 标签 / 搜索历史 / 热门）+ 特征精排，权重与算法参数全部由 `recommendation.*` 配置驱动
- 🔐 **JWT 认证** — 校验签名、有效期与签发方/受众（iss/aud），令牌绑定客户端 IP，角色默认拒绝（漏标 `@RequireUserType` 即 403）
- 🔒 **BCrypt 口令加密** — 强度因子 12，实体 `@JsonIgnore` 防哈希外泄，口令校验始终问数据库
- ⚡ **注解式限流** — 10 个敏感端点分档限流，Redis INCR 原子计数，缓存故障时拒绝型限流返回 503
- 💾 **Redis 缓存** — 键前缀统一收敛于 `CacheKeys`，写操作延时双删并挂事务 `afterCommit`
- 🏷️ **结构化标签** — `tag` / `song_tag` / `singer_tag` 中间表为唯一真源，支持整组替换与热门标签
- 🔍 **搜索历史** — 按令牌归属记录/读取/清空，参与召回，接口不接收 `userId`
- 📝 **可观测性** — traceId 贯穿请求与异步日志，操作审计落库，Actuator + Prometheus 指标
- 📚 **在线接口文档** — SpringDoc OpenAPI（dev 档开启，prod 档关闭）
- 🧪 **质量门禁** — JaCoCo 四项覆盖率阈值 + SpotBugs High=0，CI 另跑真实 MySQL 迁移链

```
┌──────────────────────────────────────────────────────┐
│                    前端层 (Vue 2)                     │
├───────────────────────┬──────────────────────────────┤
│  管理端 MusicAdmin     │  用户端 MusicUser             │
│  容器 8080 / 开发 9172 │  容器 8080(宿主 8081) / 开发 9173│
│  曲库与用户治理         │  播放、推荐、收藏、评论         │
└───────────┬───────────┴───────────────┬──────────────┘
            └───────────┬───────────────┘
                        │ HTTP / REST  /api/v1
              ┌─────────▼──────────┐
              │  后端 Spring Boot   │
              │      :9171         │
              │  JWT + 角色守卫     │
              │  限流 + 审计日志    │
              │  六路召回 + 精排    │
              └─────────┬──────────┘
                        │
              ┌─────────▼──────────┐
              │   MySQL 8.0        │  Flyway V1–V13 建库
              │   Redis 7          │  缓存 / 限流 / 令牌黑名单
              └────────────────────┘
```

> 容器部署时 MySQL 与 Redis **不向宿主机暴露端口**，仅 compose 内部网络可达。

---

## 🛠️ 技术栈

### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.4.3 | 核心框架（JDK 17） |
| MyBatis | 3.0.4 | ORM，XML 映射 13 个 Mapper |
| PageHelper | 2.1.0 | 分页；每页上限统一由 `common/PageLimits` 夹住（100） |
| MySQL / Druid | 8.0 / 1.2.23 | 数据库与连接池（`stat,wall` 过滤器） |
| Redis | 7.x | 缓存、限流计数、令牌黑名单 |
| Flyway | Boot BOM | 库结构唯一出处 `db/migration`，现为 V1–V13 |
| Spring Security | Boot BOM | 仅提供 BCrypt 与 CORS 配置源，授权由 MVC 拦截器承担 |
| jjwt | 0.12.6 | Token 签发与校验（含 iss/aud） |
| SpringDoc | 2.8.5 | OpenAPI 文档 |
| Micrometer Prometheus | Boot BOM | `/actuator/prometheus` |
| Hutool / Fastjson2 | 5.8.41 / 2.0.53 | 工具库与 JSON |
| HanLP | portable-1.8.4 | 中文分词（评论高频词） |
| Lombok | 1.18.36 | 样板代码消除 |

### 前端

两个工程均为 Vue 2 + Element UI，版本以 `package-lock.json` 实际锁定值为准
（`package.json` 里的声明范围较旧，例如管理端声明 `vue ^2.5.2`，实际锁定 2.7.16）。

| 技术 | 管理端 | 用户端 |
|------|--------|--------|
| Vue | 2.7.16 | 2.7.15 |
| Element UI | 2.15.14 | 2.15.14 |
| Vue Router / Vuex | 3.6.5 / 3.6.2 | 3.6.5 / 3.6.2 |
| Axios | 0.19.2 | 0.18.1 |
| ECharts / v-charts | 4.9.0 / 1.19.0 | — |
| Jest | 22.4.4 | 22.4.4 |
| 页面 / 路由 | 14 个 `.vue` / 10 条路由 | 27 个 `.vue` / 11 条路由 |

**双构建链路**（两端一致，容易踩坑）：

- `npm run dev` → `vue-cli-service serve`（@vue/cli-service 4.5.19，内嵌 webpack 4.47.0），端口读 `vue.config.js`
- `npm run build` → `node build/build.js`（webpack 3.12.0 老模板链路），端口与 `proxyTable` 读 `config/index.js`
- 路由均为 **hash 模式**；dev 无代理，跨域直连后端依赖后端 CORS 白名单

详见 [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md)。

### 质量工具

| 工具 | 版本 | 门禁 |
|------|------|------|
| JaCoCo | 0.8.12 | CLASS ≥ 90% / METHOD ≥ 80% / LINE ≥ 70% / BRANCH ≥ 50%，绑 `verify` |
| SpotBugs | 4.8.6.4 | `threshold=High`，绑 `verify`，排除表 `spotbugs-exclude.xml` |
| ESLint | 4.x | `plugin:vue/strongly-recommended` |
| GitHub Actions | — | `ci.yml`（后端 verify + 真实 MySQL 迁移链）、`frontend-ci.yml`（两端 lint/unit/build） |

**环境要求**：JDK 17、Maven 3.9+、Node.js 20（CI 与前端镜像均用 20）、MySQL 8.0、Redis 7；
容器部署另需 Docker 20.10+ 与 Compose 2.0+。

---

## 🚀 快速开始

### 方式一：Docker 部署（5 个服务）

```bash
cp .env.example .env    # 必须先编辑 .env 填入真实口令
# Linux/Mac
chmod +x deploy.sh && ./deploy.sh start
# Windows
.\deploy.ps1 start
```

compose 对 `MYSQL_ROOT_PASSWORD`、`DB_USERNAME`、`DB_PASSWORD`、`REDIS_PASSWORD`、`JWT_SECRET`
使用 `${VAR:?}` 必填语法，缺任一个直接拒绝启动。`DB_USERNAME` 需填 `root`
（mysql 镜像只创建 root 账号），且 `DB_PASSWORD` 与 `MYSQL_ROOT_PASSWORD` 必须一致。

| 服务 | 宿主机端口 | 容器端口 |
|------|-----------|---------|
| 管理端前端 | 8080 | 8080 |
| 用户端前端 | 8081 | 8080 |
| 后端 API | 9171 | 9171 |
| MySQL / Redis | 不暴露 | 3306 / 6379 |

> ⚠️ **默认档位是 prod**：`docker-compose.yml` 的 `SPRING_PROFILES_ACTIVE` 默认为 `prod`，
> 因此 Swagger UI 与 Druid 监控台**默认关闭**（访问 `/swagger-ui.html` 会 404）。
> 本地联调请在 `.env` 里显式设 `SPRING_PROFILES_ACTIVE=dev`。
> 生产部署另需 `CORS_ALLOWED_ORIGINS`（prod compose 必填）与 `API_BASE_URL`
> （否则前端产物会把后端地址烘焙成 `http://localhost:9171`）。

📖 [Docker 快速开始](docs/DOCKER_QUICK_START.md) • [完整部署指南](docs/DOCKER_DEPLOYMENT.md)

### 方式二：本地开发

前置：JDK 17、Maven 3.9+、Node.js 20、MySQL 8.0、Redis 7（本机需可连）。

```bash
# 1. 建一个空库 db_music（utf8mb4），表结构由 Flyway 首次启动自动迁移，无需导入 SQL
# 2. 后端（dev 档默认值可被环境变量覆盖）
cd Music-backend && mvn spring-boot:run

# 3. 前端（两个终端分别跑）
cd MusicAdmin-frontend && npm install && npm run dev
cd MusicUser-frontend  && npm install && npm run dev
```

| 入口 | 地址 |
|------|------|
| 后端 API | http://localhost:9171 |
| 健康检查 | http://localhost:9171/actuator/health |
| Swagger UI（dev 档） | http://localhost:9171/swagger-ui.html |
| 管理端（`npm run dev`） | **http://localhost:9172** |
| 用户端（`npm run dev`） | **http://localhost:9173** |

> 开发模式端口来自各自的 `vue.config.js`，**不是** 8080/8081——后者只在容器部署时成立。
> `config/index.js` 里的 8080/8081 属于 build 链路的历史配置，`npm run dev` 不读它。
> 后端 dev 档的 `JWT_SECRET` 是明确标注的占位串，仅供本地起服务；生产/联调必须注入真实密钥。

📖 [开发指南](docs/DEVELOPMENT.md) • [故障排查](docs/TROUBLESHOOTING.md)

---

## 📁 项目结构

```
Music/
├── Music-backend/                     # 后端服务 (Spring Boot 3 / JDK 17)
│   ├── src/main/java/com/shanzhu/music/
│   │   ├── controller/                # 12 个 Controller，74 个端点，统一挂 /api/v1
│   │   ├── service/                   # 业务层
│   │   │   ├── ranking/               # 特征精排（FeatureEngineering / 加权排序 / 新歌扶持）
│   │   │   └── recall/                # 六路召回策略与融合
│   │   ├── mapper/                    # 13 个 Mapper
│   │   ├── entity/                    # po / dto / vo
│   │   ├── common/                    # R<T> / ResultCodeEnum / CacheKeys / PageLimits
│   │   ├── config/                    # Security / WebMvc / Redis / OpenApi / Async / File
│   │   ├── annotation/                # @RateLimit、@RequireUserType
│   │   ├── interceptor/               # 日志(0) → JWT(1) → 限流(2)
│   │   ├── exception/                 # GlobalExceptionHandler
│   │   ├── task/                      # 审计日志过期清理
│   │   └── utils/                     # JwtUtils / IpUtils / RedisUtils / PasswordUtils
│   ├── src/main/resources/
│   │   ├── mapper/                    # MyBatis XML
│   │   ├── db/migration/              # Flyway V1–V13（库结构唯一出处）
│   │   └── application*.yml           # 主配置 + dev / test / prod
│   ├── src/test/                      # 33 个测试类（H2 + MockMvc，BaseTest 预置令牌并清缓存）
│   ├── .mvn/jvm.config                # 强制 Maven JVM UTF-8（GBK 主机防乱码）
│   ├── spotbugs-exclude.xml
│   ├── Dockerfile                     # 多阶段：maven:3.9-temurin-17 → temurin:17-jre-alpine
│   └── pom.xml
├── MusicAdmin-frontend/               # 管理端 (Vue 2 + Element UI)
├── MusicUser-frontend/                # 用户端 (Vue 2 + Element UI)
│   └── 两端同构：src/{api,pages,router,store}、config/load-env.js、
│       vue.config.js(dev 端口)、build/(webpack3 生产链路)、Dockerfile、nginx.conf
├── docs/                              # 专题文档（见下方索引）
├── .github/workflows/                 # ci.yml / frontend-ci.yml
├── docker-compose.yml                 # 开发/本地（密钥变量必填）
├── docker-compose.prod.yml            # 生产（资源限制 + CORS 必填）
├── deploy.sh / deploy.ps1             # 部署脚本
├── API 开发文档.md                     # 接口契约与调用规范
├── CHANGELOG.md                       # 版本级变更摘要
├── db_music.sql                       # 已停用的历史 dump，勿手工导入
└── README.md
```

---

## 🔌 接口约定

- 全部业务接口挂 `/api/v1`，资源式路径，部分更新用 `PATCH`，标签整组替换用 `PUT`；旧动词式路径无兼容层（调用会 404）
- **静态资源例外**：`/song/*`、`/img/**`、`/userImages/**` 不带前缀（`<audio>`/`<img>` 发不出 Authorization 头）
- 认证只认请求头 `Authorization: <裸 JWT>`，**不带 `Bearer ` 前缀**，不支持 `?token=` 查询参数
- 匿名入口仅 3 条（按方法+路径成对放行）：`POST /api/v1/admin/sessions`、`POST /api/v1/consumers/sessions`、`POST /api/v1/consumers/registrations`
- 统一返回体 `R<T>`：`success` / `code`（20000 成功、20001 失败、20002 参数错误）/ `message` / `data`；判断成败以 `success` 为准
- HTTP 状态码与 body 的 `code` 两层各自表意：鉴权/限流用 401/403/429/503，参数错误 400 + 20002，业务失败仍是 HTTP 200 + `success=false`

📖 完整接口清单与调用示例：[API 开发文档.md](API%20开发文档.md) • 在线文档：Swagger UI（dev 档）

---

## 🧪 测试与质量门禁

| 侧 | 规模 | 跑法 |
|----|------|------|
| 后端 | **298 个用例**（225 个 `@Test` + 5 个 `@ParameterizedTest` 展开 73 项），33 个测试类 | `mvn -B clean verify`（与 CI 同一条命令，含 JaCoCo + SpotBugs） |
| 管理端 | 11 个用例（mixin 纯函数） | `npm run unit` |
| 用户端 | 13 个用例（歌词解析 6 + 播放器 store 7） | `npm run unit` |

后端用例数按注解与数据源静态核算：`AdminWriteAuthMatrixTest` 的 22 条 admin 端点 × 2 种身份
+ 11 条 consumer 专属端点 × 2 种身份 = 66 项，`CommentControllerTest` 的 7 条非法评论 = 7 项。

**覆盖率门禁与实测水位**

| 维度 | 门禁（pom） | v3.0.0 实测水位 |
|------|------------|----------------|
| CLASS | ≥ 90% | 97.3% |
| METHOD | ≥ 80% | 86.2% |
| LINE | ≥ 70% | 76.7% |
| BRANCH | ≥ 50% | 56.5% |

> 实测水位是 v3.0.0 定版时的记录（见 [CHANGELOG.md](CHANGELOG.md)），此后新增了 1 个冷启动降级用例。
> 上表用例数为静态核算结果；**本机若无 Redis（localhost:6379）则无法复跑后端测试**，
> 通过与否以 CI 为准，不以本表数字宣称"全绿"。

**需要如实说明的覆盖边界**

- 后端覆盖以 Controller 与 Service 层为主；六路召回与端到端推荐靠真实 MySQL + Redis 探针验证，不在 `mvn test` 内
- 测试用 H2 + `src/test/resources/schema.sql`，**不走 Flyway**；迁移链由 CI 的独立 job 在真实 MySQL 8.0 上按 V1→V13 分段验证
- `src/main/resources/application-test.yml` 被 `src/test/resources/` 同名文件按 classpath 优先级遮蔽，测试配置只有后者生效
- 前端测试针对纯逻辑，不含浏览器端 UI 验证；Docker 镜像内的前端构建过程未实机验证
- 管理端没有管理员账号的增删改查接口（`AdminMapper` 只有 `verifyPassword`/`getByName`），对应位置不留占位测试
- 种子口令哈希的明文未记录在仓库任何位置，"管理员用正确口令登录"在无凭据环境下无法断言

📖 [单元测试报告](docs/UNIT_TEST_REPORT.md) • [覆盖率门禁](docs/06-CODE-COVERAGE.md)

---

## 📚 文档索引

每类内容只有一个出处；历史专题文档顶部均有「口径声明」标注与现状的差异，冲突时以代码和
[docs/FIX-LOG-2026-11.md](docs/FIX-LOG-2026-11.md) 为准。

| 主题 | 文档 |
|------|------|
| 本地开发、端口与双构建链路、分支与提交规范 | [DEVELOPMENT.md](docs/DEVELOPMENT.md) |
| 故障排查（端口/数据库/Redis/跨域/Swagger 404） | [TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md) |
| Docker 快速开始 / 完整部署指南 | [DOCKER_QUICK_START.md](docs/DOCKER_QUICK_START.md) • [DOCKER_DEPLOYMENT.md](docs/DOCKER_DEPLOYMENT.md) |
| 接口契约与调用规范 | [API 开发文档.md](API%20开发文档.md) |
| JWT 认证与令牌管理 | [01-AUTHENTICATION.md](docs/01-AUTHENTICATION.md) |
| API 限流 | [02-RATE-LIMITING.md](docs/02-RATE-LIMITING.md) |
| 日志体系 / 请求审计 / 参数校验 | [03-LOGGING.md](docs/03-LOGGING.md) • [04-REQUEST-AUDIT.md](docs/04-REQUEST-AUDIT.md) • [05-PARAM-VALIDATION.md](docs/05-PARAM-VALIDATION.md) |
| 覆盖率门禁 / 单测报告 | [06-CODE-COVERAGE.md](docs/06-CODE-COVERAGE.md) • [UNIT_TEST_REPORT.md](docs/UNIT_TEST_REPORT.md) |
| 安全配置 / BCrypt 口令加密 | [SECURITY_CONFIG_GUIDE.md](docs/SECURITY_CONFIG_GUIDE.md) • [BCRYPT_PASSWORD_ENCRYPTION.md](docs/BCRYPT_PASSWORD_ENCRYPTION.md) |
| 召回与搜索历史 / 特征工程 / 标签系统 | [RECALL_AND_SEARCH_HISTORY_FEATURE.md](docs/RECALL_AND_SEARCH_HISTORY_FEATURE.md) • [FEATURE_ENGINEERING_REAL_CALCULATION.md](docs/FEATURE_ENGINEERING_REAL_CALCULATION.md) • [TAG_SYSTEM_OPTIMIZATION.md](docs/TAG_SYSTEM_OPTIMIZATION.md) |
| Redis 缓存 / 延时双删 | [REDIS_CACHE_OPTIMIZATION.md](docs/REDIS_CACHE_OPTIMIZATION.md) • [REDIS_USAGE.md](docs/REDIS_USAGE.md) • [DOUBLE_DELETE_STRATEGY.md](docs/DOUBLE_DELETE_STRATEGY.md) |
| 统一返回体 R\<T\> / OpenAPI / 分页 | [R_GENERIC_UPGRADE_REPORT.md](docs/R_GENERIC_UPGRADE_REPORT.md) • [SPRINGDOC_OPENAPI_CONFIG.md](docs/SPRINGDOC_OPENAPI_CONFIG.md) • [PAGEINATION_UPGRADE.md](docs/PAGEINATION_UPGRADE.md) |
| 当前真实口径与修复记录（内部） | [FIX-LOG-2026-11.md](docs/FIX-LOG-2026-11.md) |
| 版本级变更摘要 | [CHANGELOG.md](CHANGELOG.md) |

> `docs/` 下另有若干改造当时的历史快照（前后端匹配分析、R 类升级报告、Spring Security 修复报告、
> Docker 配置完成报告、测试方法映射检查等），仅作沿革参考，**照抄其中的 URL 会拿到 404**。

---

## 🤝 贡献

1. Fork 本仓库，从 `master` 切特性分支：`git checkout -b feature/AmazingFeature`
2. 后端提交前跑通 `mvn -B clean verify`；前端跑通 `npm run lint` 与 `npm run unit`
3. 提交信息用 Conventional Commits（中文描述），推送后提 Pull Request

仓库自 2026-11-08 起只保留 `master` 一个长期分支，发布打附注 tag。
分支与 CI 触发条件必须同步修改——不一致时的症状不是 CI 失败，而是"推了但一条 run 都没有"。

📖 详见 [开发指南](docs/DEVELOPMENT.md)

---

## 📄 许可证与联系

本项目基于 [Apache License 2.0](LICENSE) 授权，完整条款见仓库根目录 `LICENSE`。

- **GitHub**：[ZapcoMan/Music](https://github.com/ZapcoMan/Music)
- **问题反馈**：[Issues](https://github.com/ZapcoMan/Music/issues)

```
Copyright 2024-2026 ZapcoMan

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```

---

<div align="center">

**⭐ 如果这个项目对你有帮助，请给一个 Star!**

*最后更新：2026-11-16（按代码事实全量对账；pom 版本 3.0.0 尚未打 tag，最新发布 tag 为 v2.0.1）*

</div>