# 开发指南

本文是本地开发口径的唯一出处：端口、构建链路、环境变量、测试跑法、分支与提交规范。
部署相关内容见 [DOCKER_DEPLOYMENT.md](DOCKER_DEPLOYMENT.md)，接口契约见 [API 开发文档.md](../API%20开发文档.md)。

---

## 1. 环境要求

| 组件 | 版本 | 说明 |
|------|------|------|
| JDK | 17 | `pom.xml` 的 `java.version`，Spring Boot 3.4.3 要求 |
| Maven | 3.9+ | 仓库自带 `.mvn/jvm.config`，强制 Maven JVM 用 UTF-8 |
| Node.js | 20 | CI 与前端镜像均用 20；本机更高版本亦可（测试 setup 已补 jsdom 缺失的 API） |
| MySQL | 8.0 | 本地开发需自建空库 `db_music`（utf8mb4） |
| Redis | 7 | **后端测试与本地启动都需要**，见第 5 节 |

`.mvn/jvm.config` 内容为 5 个编码参数（`file.encoding`、`sun.stdout.encoding`、`sun.stderr.encoding`、
`stdout.encoding`、`stderr.encoding` 均为 UTF-8）。它解决的是 GBK 主机上编译输出与测试控制台乱码，
不是 Spring 配置键，不要写进 `application.yml`。

---

## 2. 端口全景（最容易搞错的地方）

同一个前端工程在三种场景下端口不同：

| 场景 | 管理端 | 用户端 | 端口来源 |
|------|--------|--------|---------|
| `npm run dev` | **9172** | **9173** | 各自 `vue.config.js` 的 `devServer.port` |
| Docker 部署（宿主机） | 8080 | 8081 | `docker-compose.yml` 的 `ports` 映射 |
| Docker 部署（容器内） | 8080 | 8080 | `nginx.conf` 的 `listen`（nginx-unprivileged 非 root） |

后端固定 **9171**（三个 profile 一致，无 `context-path`）。

> ⚠️ `config/index.js` 里的 `dev.port = 8080/8081` 与 `proxyTable: {}` 属于旧
> `webpack-dev-server` 链路。当前 `npm run dev` 走 `vue-cli-service`，**不读这个文件**；
> `build/webpack.dev.conf.js` 也没有任何脚本引用它。改端口请改 `vue.config.js`。

### 跨域：dev 无代理，依赖后端 CORS 白名单

两个前端工程的 `vue.config.js` 都只设了端口，**没有 proxy**；`config/index.js` 的 `proxyTable` 是空对象。
浏览器直接跨域访问 `http://localhost:9171`，因此后端 CORS 白名单必须包含前端 origin。

白名单唯一出处是 `SecurityConfig`（`WebMvcConfig` 不再重复定义），配置键 `security.cors.allowed-origins`：

| 档位 | 取值来源 | 默认值 |
|------|---------|--------|
| dev | `SecurityConfig` 的 `@Value` 默认值（`application-dev.yml` **未覆盖**该键） | `http://localhost:8080,http://localhost:8081,http://localhost:9172` |
| prod | `application-prod.yml` 的 `${CORS_ALLOWED_ORIGINS:...}` | `http://localhost:8080,http://localhost:8081` |

> ⚠️ **默认白名单含管理端 dev 端口 9172，但不含用户端 dev 端口 9173。**
> 用 `npm run dev` 起用户端联调时，需要把 9173 加进白名单，否则浏览器预检失败
> （curl 不带 Origin 头，所以命令行测全绿、页面里全红）。两种做法：
>
> ```bash
> # 方式一：启动后端时注入环境变量（prod compose 用的就是这个形式）
> SECURITY_CORS_ALLOWED_ORIGINS=http://localhost:9172,http://localhost:9173 mvn spring-boot:run
>
> # 方式二：在 application-dev.yml 补一段（本地改动，注意不要提交弱化的白名单）
> # security:
> #   cors:
> #     allowed-origins: http://localhost:9172,http://localhost:9173
> ```
>
> 允许方法已含 `PATCH`（改资料/改曲库用），来源必须是精确值——`allowCredentials=true` 时不能用通配符。

---

## 3. 后端本地启动

```bash
cd Music-backend
mvn spring-boot:run          # 默认 dev 档（由 pom 的 profile 过滤写入 application.yml）
```

profile 由 `application.yml` 的 `@spring.profiles.active@` 占位符在 Maven 资源过滤时填入，
pom 默认 `dev`。切换档位：`mvn spring-boot:run -Dspring-boot.run.profiles=prod`，或设 `SPRING_PROFILES_ACTIVE`。

### dev / prod 关键差异

| 项 | dev | prod |
|----|-----|------|
| Druid 连接池 | initial 5 / min 5 / max 20 | 10 / 10 / 50 |
| 慢 SQL 阈值 | 3000 ms | 5000 ms |
| Druid 监控台 | 开启，仅允许 127.0.0.1 | **关闭**（`web-stat-filter` 与 `stat-view-servlet` 均关） |
| 上传限制 | 单文件 100MB / 请求 200MB | 单文件 100MB / 请求 110MB |
| SpringDoc / Swagger | **开启** | **关闭**（`/swagger-ui.html` 会 404） |
| 日志 | `com.shanzhu.music=DEBUG` | root `WARN`，保留 90 天 / 20GB 上限 |
| 密钥类默认值 | 有明确标注的占位串 | **无默认值，缺失即启动失败** |

三档共同点：`server.port=9171`、MyBatis `map-underscore-to-camel-case=true`、
Flyway `enabled=true` / `validate-on-migrate=true` / **`baseline-on-migrate=false`**、
Actuator 暴露 `health,info,metrics,prometheus`。

> `baseline-on-migrate=false` 是刻意的：对已有数据的库启动会失败，而不是静默重放迁移
> （V1 含 `DROP TABLE`，重放等于清库）。已有数据的环境请走 `mysqldump` 现场备份恢复。

### 环境变量占位符

| 变量 | dev 默认 | prod 默认 |
|------|---------|----------|
| `DB_USERNAME` / `DB_PASSWORD` | `root` / `root` | **无默认（必填）** |
| `DB_HOST` / `DB_PORT` | localhost / 3306（写在 URL 里） | `localhost` / `3306` |
| `REDIS_PASSWORD` | `admin` | **无默认（必填）** |
| `REDIS_HOST` / `REDIS_PORT` | localhost / 6379 | `localhost` / `6379` |
| `JWT_SECRET` | 明确标注的 dev 占位串 | **无默认（必填）** |
| `DRUID_LOGIN_USERNAME` / `DRUID_LOGIN_PASSWORD` | `admin` / `admin` | 空 |
| `TRUST_PROXY_HEADERS` | — | `false` |
| `CORS_ALLOWED_ORIGINS` | — | `http://localhost:8080,http://localhost:8081` |

其余安全/治理类键的出处（容易记混，逐个列清）：

| 键 | 取值 | 出处 |
|----|------|------|
| `jwt.expiration` | `86400000`（24 小时） | dev / prod yml **均硬编码**，无环境变量占位（不存在 `JWT_EXPIRATION` 这个变量） |
| `rate-limit.enabled` | `true` | dev yml 与 prod yml **均显式定义**；代码默认值也是 `true` |
| `security.ip.trust-proxy-headers` | `false` | prod yml 为 `${TRUST_PROXY_HEADERS:false}`；dev yml 未定义，走代码默认 `false` |
| `security.cors.allowed-origins` | 见第 2 节 | prod yml 为 `${CORS_ALLOWED_ORIGINS:...}`；dev yml 未定义，走 `SecurityConfig` 的 `@Value` 默认值 |
| `audit.log.retention-days` | `90` | **仅代码默认值**，任何 yml 都未定义该键 |

logback 另有 `${consoleCharset:-UTF-8}`（JVM 参数，非 Spring 键）。

> `JWT_SECRET` 用于 HMAC-SHA384，jjwt 按 RFC 7518 要求密钥不短于哈希输出，
> 实际需 ≥ 56 个 ASCII 字符（448 bit）。dev 占位串仅供本地起服务，禁止用于任何联调/生产环境。

### 数据库初始化

库结构唯一出处是 `Music-backend/src/main/resources/db/migration` 的 **V1–V13**，首次启动自动迁移。

- V1 初始化全库 → V2 搜索历史与 tags 列 → V3 标签中间表 → V4 热路径索引 → V5 操作审计表
- V6/V7 标签与搜索历史种子 → V8 孤儿引用补齐 → V9/V10 字符集转 utf8mb4
- V11 歌单收藏种子 → V12 账号与关联唯一约束 → V13 标签真值收口并 **DROP `user_ratings`**

> 根目录 `db_music.sql` 是 V1 时代的整库旧快照（11 张表，含已被 V13 删除的 `user_ratings`
> 与旧 `flyway_schema_history` 记录），**已停用，勿手工导入**。
> `Music-backend/fix_flyway.sql` 是历史遗留的手工修复脚本（删一条失败的 V2 迁移记录），
> 迁移链现由 CI 的独立 job 在真实 MySQL 上验证，不要再手工执行它。

---

## 4. 前端本地启动

```bash
cd MusicAdmin-frontend   # 或 MusicUser-frontend
npm install --registry=https://registry.npmmirror.com
npm run dev              # 管理端 → http://localhost:9172，用户端 → http://localhost:9173
```

### 双构建链路

两个工程的 `package.json` scripts 完全相同，但 dev 与 build 走的是两套工具链：

| 命令 | 实际执行 | 工具链 | 环境变量注入 |
|------|---------|--------|-------------|
| `npm run dev` | `vue-cli-service serve` | @vue/cli-service 4.5.19（内嵌 webpack 4.47.0） | cli 内置 dotenv 读 `.env` + `.env.development` |
| `npm run build` | `node build/build.js` | webpack 3.12.0 老模板 | `webpack.prod.conf.js` → `config/prod.env.js` → `config/load-env.js` 读 `.env` + `.env.production` |
| `npm run unit` | `jest --config test/unit/jest.conf.js --coverage` | Jest 22.4.4 + babel-jest + vue-jest | — |
| `npm run lint` | `eslint --ext .js,.vue src test/unit` | ESLint 4.x，`plugin:vue/strongly-recommended` | — |

`config/load-env.js` 是零依赖实现，优先级为 **真实环境变量 > `.env.<mode>` > `.env`**，
输出交给 `DefinePlugin` 的字面量（字符串再包一层引号）。它只服务 build 链路；
`config/dev.env.js` 引用的 `webpack.dev.conf.js` 当前无脚本调用，属历史遗留。

### 后端地址只有一个来源

`src/api/http.js` 从 `process.env.VUE_APP_API_BASE_URL` 派生两个基址：

- `HOST` = 该 origin，用于静态资源（`/song`、`/img`、`/userImages`）
- `API_HOST` = 该 origin + `/api/v1`，用于业务接口（axios `baseURL`）

默认回落 `http://localhost:9171`。六个 `.env` 文件（两端各 `.env` / `.env.development` / `.env.production`）
均已入库，值都是 `http://localhost:9171`，**不含任何密钥**（根 `.gitignore` 精确放行这六个文件）。

> 生产构建必须覆盖：`docker build --build-arg VUE_APP_API_BASE_URL=https://api.example.com`，
> 或设 `API_BASE_URL` 让 prod compose 传入。否则产物会把后端地址烘焙成 localhost。
> 不要直接改 `http.js` 里的地址。

### 请求约定

- token 存 `localStorage`：管理端键 `adminToken`，用户端键 `userToken`
- 请求头 `Authorization: <裸 JWT>`，**两端都不加 `Bearer ` 前缀**（后端也不解析该前缀）
- 超时 15 秒，`withCredentials=false`
- 响应 401 → 管理端跳 `/`、用户端跳 `/login-in`；错误提示归一为 `err.userMessage`，
  覆盖 403/404/405/413/415/429 的状态提示
- 文件上传绕过 axios 封装，用原生 XHR/fetch 自携 Authorization 头
- 路由均为 **hash 模式**（无 `mode: 'history'`）。管理端有 `beforeEach` 守卫（白名单 `['/']`，
  其余查 `adminToken`）；用户端**无路由守卫**，登录约束靠 401 跳转
- 两端均无 mock

> `nginx.conf` 里"history 模式支持"的注释与实际 hash 路由不符；`try_files` 回落 `index.html`
> 对 hash 路由无害，但不要据此以为项目用了 history 模式。API 反代整段是注释状态，
> 容器内前端直连构建期烘焙的后端地址。

---

## 5. 测试

### 后端：本机需要 Redis

```bash
cd Music-backend
mvn test                        # 只跑测试
mvn -B clean verify             # 测试 + JaCoCo 四项门禁 + SpotBugs（与 CI 同一条命令）
mvn test -Dtest=RTest           # 单个测试类
mvn surefire-report:report      # HTML 报告
```

**前置条件**：测试配置 `src/test/resources/application-test.yml` 指向 `localhost:6379`、口令 `admin`。
本机没有 Redis 时，`BaseTest` 每用例前清缓存的动作会失败，测试无法复跑——这不是代码问题。
CI 里由 job 起 `redis:7` 容器（`--requirepass admin`）满足该前置。

数据库侧不需要 MySQL：测试用 H2（`MODE=MySQL`）+ `src/test/resources/schema.sql`，
**Flyway 在测试档关闭**。真实迁移链由 `ci.yml` 的 `mysql-migrations` job 在 MySQL 8.0 上
按 `sort -V` 分段执行 V1→V13，并断言 5 个唯一约束与语义校验。

**规模与核算口径**：298 个用例 = 225 个 `@Test` + 5 个 `@ParameterizedTest` 展开的 73 项
（鉴权矩阵 22 条 admin 端点 × 2 身份 + 11 条 consumer 专属端点 × 2 身份 = 66，非法评论 7 条 = 7）。
33 个测试类中 26 个继承 `BaseTest`（Spring 上下文 + MockMvc），7 个是纯 Mockito 单测
（`RTest`、`RequestTraceIdTest`、`FileStorageServiceTest`、`RecallFusionServiceTest`、
`TokenBlacklistServiceTest`、`JwtBlacklistDegradationTest`、`RateLimitInterceptorTest`）。
无 `@Disabled`。

> 用 `grep -c "@Test"` 统计会多算 19 个 `@TestMethodOrder`，得到错误的 244。核算请用
> `grep -cE "^\s*@Test\s*$"` 并加上参数化数据源条目数。

**隔离机制与已知边界**：

- `BaseTest` 每用例前按 `music:*` 与 `rate:*` 两个模式清键，并预置令牌；**绝不清 `jwt:blacklist:*`**
- `spring.sql.init.encoding: UTF-8` 必须保留：GBK 主机上按平台编码读 `schema.sql` 会让中文种子变乱码，
  依赖中文的断言查不到数据（历史上曾因 Redis 有旧缓存命中而假绿）
- 测试档补了 `map-underscore-to-camel-case: true` 与 dev/prod 对齐，否则 `match_count` 等列恒为 null
- `src/main/resources/application-test.yml` 被 `src/test/resources/` 同名文件按 classpath 优先级**遮蔽**，
  其中 `rate-limit.enabled: false` 等设置不生效，测试里限流开关落到代码默认 `true`
- 测试类间仍共享同一 H2 实例与 Redis 库，类间顺序独立性靠约定而非机制
- MockMvc 只接管 `RequestMappingHandlerMapping`，**看不见 actuator 端点**；
  指标是否装配由 `ActuatorMetricsAccessTest` 断 bean 验证

### 前端

```bash
npm run unit    # jest --coverage，两端各约 10 秒
npm run lint    # 0 error 是提交前提
npm run build   # 产物不应含 .map（productionSourceMap=false + nginx deny 双保险）
```

- 管理端 11 例：`test/unit/specs/admin-mixin.spec.js`，测 mixin 纯函数（性别转换、生日处理、头像上传前后钩子）
- 用户端 13 例：`lyric.spec.js` 6 例（歌词解析，含死循环回归）+ `player-store.spec.js` 7 例（播放器 store 缓存回读语义）
- `test/unit/setup.js` 为高版本 Node/jsdom 补 `sessionStorage` 内存替身
- 覆盖率排除 `main.js` 与 `router`；**无浏览器端 UI 验证**，CI 也不设覆盖率数值门槛

---

## 6. 质量门禁

| 门禁 | 阈值 | 绑定阶段 |
|------|------|---------|
| JaCoCo CLASS | ≥ 90% | `check` 默认绑 `verify`（`report` 绑 `test`） |
| JaCoCo METHOD | ≥ 80% | 同上 |
| JaCoCo LINE | ≥ 70% | 同上 |
| JaCoCo BRANCH | ≥ 50% | 同上 |
| SpotBugs | `threshold=High`，`effort=Max` | `verify` |
| Surefire | `testFailureIgnore=false`（失败即中断构建） | `test` |

SpotBugs 排除表 `spotbugs-exclude.xml` 只排除 Lombok `@Data` 生成的 `EI_EXPOSE_REP`/`EI_EXPOSE_REP2`。
Surefire 用 `@{argLine}` 延迟求值接住 JaCoCo 探针参数，并强制子进程 UTF-8；
`*IntegrationTest` 被排除在 `mvn test` 之外。

CI（`.github/workflows/`，触发分支均为 `master`）：

- `ci.yml`：`backend-verify`（temurin 17 + Redis 容器 + `mvn -B clean verify`，失败上传 surefire 报告）
  与 `mysql-migrations`（真实 MySQL 8.0 分段迁移 + 约束断言）
- `frontend-ci.yml`：matrix 两端，node 20 + `npm ci` → `lint` → `unit` → `build` → 断言 dist 无 `.map`
- 两个 workflow 都不发布 Release/Pages，不引用 secrets

---

## 7. 分支与提交规范

- 仓库自 2026-11-08 起**只保留 `master` 一个长期分支**（远端 `main` 已删除，`origin/HEAD` 指向 `master`）
- `feature/*`、`hotfix/*` 为临时分支，合入后即删；发布打附注 tag
- **CI 触发分支必须与实际推送分支一致**：不一致时的症状不是 CI 失败，而是"推了但一条 run 都没有"
- 提交信息用 Conventional Commits，描述用中文（代码标识符与命令保留英文）
- 每完成一个连贯改动就校验后提交，不攒到最后；只显式 `git add` 项目文件

后端遵循阿里巴巴 Java 开发手册，`mvn -B clean verify` 不绿不提交；
前端 `npm run lint` 与 `npm run unit` 不绿不提交。

> 版本号落点：`pom.xml` 的 `<version>` 是唯一真源，经 `build-info` goal 暴露给 `AppInfoContributor`；
> Dockerfile 用 jar 通配归一为 `app.jar`，不写死版本号。当前 pom 为 `3.0.0`，
> 但最新已发布 tag 是 `v2.0.1`——`3.0.0` 尚未打 tag。

---

## 8. 改文件前先确认它是否生效

仓库里存在几处历史遗留的"看着像配置、实际没人读"的位置，改它们不会有任何效果：

| 位置 | 状态 |
|------|------|
| `config/index.js` 的 `dev.port`、`proxyTable` | 旧 webpack-dev-server 链路，`npm run dev` 不读 |
| `build/webpack.dev.conf.js`、`config/dev.env.js` | 无脚本引用 |
| `src/main/resources/application-test.yml` | 被 `src/test/resources/` 同名文件遮蔽 |
| `pagehelper.max-page-size` | 刻意不配：对显式 `startPage` 不生效，上限唯一出处是 `common/PageLimits`（100） |
| `nginx.conf` 的 API 反代段 | 整段注释状态 |
| `db_music.sql`、`Music-backend/fix_flyway.sql` | 已停用的历史脚本 |

---

## 📖 相关文档

- [README.md](../README.md) — 项目总览
- [TROUBLESHOOTING.md](TROUBLESHOOTING.md) — 故障排查
- [DOCKER_DEPLOYMENT.md](DOCKER_DEPLOYMENT.md) — 部署
- [06-CODE-COVERAGE.md](06-CODE-COVERAGE.md) / [UNIT_TEST_REPORT.md](UNIT_TEST_REPORT.md) — 测试与覆盖率
- [FIX-LOG-2026-11.md](FIX-LOG-2026-11.md) — 当前真实口径与修复记录

*最后更新：2026-11-16（按代码事实核对）*