# 故障排查

按症状索引。每条给出**真因**与解法，并标注代码出处，避免照着过期文档改错地方。
端口与构建链路的前置知识见 [DEVELOPMENT.md](DEVELOPMENT.md)，部署细节见 [DOCKER_DEPLOYMENT.md](DOCKER_DEPLOYMENT.md)。

---

## 1. 端口被占用

### Docker 部署时

```
Error starting userland proxy: Bind for 0.0.0.0:8080 failed: port is already allocated
```

改 `docker-compose.yml` 的 `ports` **宿主侧**，容器侧不要动（前端 nginx-unprivileged 非 root，
不能绑 1024 以下端口，容器内固定 8080）：

```yaml
ports:
  - "8082:8080"   # 管理端
  - "8083:8080"   # 用户端
  - "9175:9171"   # 后端
```

> 别把后端宿主端口改成 9172——那是管理端 `npm run dev` 的端口，两种模式同时跑会撞车。

### 本地开发时

改对应工程的 `vue.config.js`（管理端 9172 / 用户端 9173），**不是** `config/index.js`。
改完前端端口后，记得把新 origin 加进后端 CORS 白名单（见第 5 条）。

---

## 2. 后端启动失败：数据库

### 本地开发

连接信息在 `Music-backend/src/main/resources/application-dev.yml`，不是 `application.yml`
（主配置只有 profile 占位与推荐参数）。dev 档默认 `root/root`、`localhost:3306/db_music`，
可用环境变量覆盖：

```bash
DB_USERNAME=root DB_PASSWORD=你的口令 mvn spring-boot:run
```

先确认空库存在：`CREATE DATABASE db_music DEFAULT CHARACTER SET utf8mb4;`
表结构由 Flyway V1–V13 自动迁移，**不要导入根目录的 `db_music.sql`**（已停用的 V1 时代快照，
含被 V13 删除的 `user_ratings` 表与旧 `flyway_schema_history` 记录，导入会造成两个结构出处）。

### Docker 部署

三个高频真因：

1. **`DB_USERNAME` 必须填 `root`**。mysql 服务只设了 `MYSQL_ROOT_PASSWORD`，没有 `MYSQL_USER`/`MYSQL_PASSWORD`，
   镜像只创建 root 账号。`.env.example` 里的示例值 `your_db_user` 会直接连不上。
2. **`DB_PASSWORD` 必须与 `MYSQL_ROOT_PASSWORD` 一致**，否则后端拿到的口令与库里的不符。
3. **宿主机连不上 3306 是正常的**：compose 不再向宿主暴露 MySQL/Redis 端口。
   需要从本机调试时，临时启用 `docker-compose.yml` 里注释掉的 `127.0.0.1:13306:3306` 映射，
   然后连 `localhost:13306`；或用 `docker-compose exec mysql mysql -u root -p`。

改口令后已存在的卷不会自动更新（MySQL 只在首次初始化时写入口令），需要重建卷——
注意 `docker-compose down -v` 会**清空数据**，先备份。

### Flyway 报错

| 症状 | 真因 | 解法 |
|------|------|------|
| 启动即失败，提示 found non-empty schema | 三档均为 `baseline-on-migrate: false`，刻意不让已有数据的库静默重放迁移（V1 含 `DROP TABLE`） | 用空库；已有数据请走 `mysqldump` 备份恢复，不要改这个开关 |
| Validate failed: migration checksum mismatch | 已执行过的迁移脚本被改动 | 恢复脚本原样；迁移链由 CI 的 `mysql-migrations` job 保证 |
| 提示版本记录 success=0 | 历史遗留失败记录 | `Music-backend/fix_flyway.sql` 是当年的手工修复脚本，**现在不要用**；先查 `flyway_schema_history` 定位真因 |

---

## 3. Redis 连接失败

```yaml
# application-prod.yml
spring:
  data:
    redis:
      password: ${REDIS_PASSWORD}   # 必填，无默认值
```

- **prod 档 `REDIS_PASSWORD` 无默认兜底**，缺失即启动失败；compose 用 `${REDIS_PASSWORD:?}` 同样强制
- dev 档默认口令是 `admin`，本地 Redis 若设了别的口令，用 `REDIS_PASSWORD=... ` 覆盖
- Docker 部署时宿主机连不上 6379 是正常的（不暴露端口），用 `docker-compose exec redis redis-cli -a "$REDIS_PASSWORD" ping`
- Redis 不可达时的行为是**分档降级**，不是一律失败：
  - 令牌黑名单读取 fail-open（放行 + 记 error 日志，已登出令牌仍受签名与过期约束）
  - 限流默认 `failOpen=false` → 返回 **503**（爆破闸门不因缓存故障默默解除）

### 后端测试跑不起来

测试配置 `src/test/resources/application-test.yml` 指向 `localhost:6379`、口令 `admin`。
本机没有 Redis 时 `BaseTest` 清缓存会失败——这是环境前置缺失，不是代码问题。
起一个口令为 `admin` 的 Redis 即可（CI 用 `redis:7 --requirepass admin` 容器）。
测试数据库侧不需要 MySQL（用 H2 + `schema.sql`，Flyway 在测试档关闭）。

---

## 4. Swagger UI / Druid 监控台 404

**真因：档位是 prod，两者在 prod 档被刻意关闭。**

| 入口 | dev | prod |
|------|-----|------|
| `/swagger-ui.html`、`/v3/api-docs` | 开 | **关**（`application-prod.yml` 的 springdoc 段） |
| `/druid/*` 监控台 | 开（仅允许 127.0.0.1） | **关**（`web-stat-filter` 与 `stat-view-servlet` 均关） |
| `/actuator/health` | 匿名可访问 | 匿名可访问 |
| `/actuator/{info,metrics,prometheus}` | 需有效 JWT | 需有效 JWT |

关键陷阱：**`docker-compose.yml`（开发档）的 `SPRING_PROFILES_ACTIVE` 默认值也是 `prod`**。
所以 `docker-compose up -d` 或 `./deploy.sh start` 起出来的后端默认没有 Swagger，
而部署脚本打印的 `Swagger UI: http://localhost:9171/swagger-ui.html` 在默认档下会 404。

解法：在 `.env` 里显式设 `SPRING_PROFILES_ACTIVE=dev`，或临时
`SPRING_PROFILES_ACTIVE=dev docker-compose up -d`。生产环境请保持关闭，用离线接口文档
[API 开发文档.md](../API%20开发文档.md)。

---

## 5. 前端调不通后端 API

### 先分清是哪种失败

| 现象 | 真因 |
|------|------|
| 浏览器控制台报 CORS / 预检失败，但 curl 正常 | 后端白名单不含前端 origin（curl 不带 Origin 头） |
| 请求发到了错误地址（如线上页面打 localhost） | `VUE_APP_API_BASE_URL` 在构建期被烘焙进产物 |
| 401 | 未带令牌、令牌无效/过期、已登出吊销 |
| 403 | IP 与令牌绑定不符，或角色不匹配 / 端点未声明角色域 |

### CORS：默认白名单不含用户端 dev 端口

白名单唯一出处是 `SecurityConfig`，键 `security.cors.allowed-origins`。
`application-dev.yml` **未覆盖**该键，因此 dev 档走代码默认值：
`http://localhost:8080,http://localhost:8081,http://localhost:9172`。

管理端 dev 端口 9172 在列，**用户端 dev 端口 9173 不在列**。起用户端联调时注入：

```bash
SECURITY_CORS_ALLOWED_ORIGINS=http://localhost:9172,http://localhost:9173 mvn spring-boot:run
```

prod 档由 `CORS_ALLOWED_ORIGINS` 控制（prod compose 中为 `${VAR:?}` 必填）。
来源必须精确值，`allowCredentials=true` 时不能用通配符；允许方法已含 `PATCH`。

### 后端地址只有一个来源

`src/api/http.js` 从 `process.env.VUE_APP_API_BASE_URL` 派生 `HOST`（静态资源）与
`API_HOST`（业务接口 `/api/v1`），默认回落 `http://localhost:9171`。

- 本地开发：改对应工程的 `.env.development`
- 生产构建：`docker build --build-arg VUE_APP_API_BASE_URL=https://api.example.com`，
  或设 `API_BASE_URL` 由 prod compose 传入
- **不要改 `http.js` 里的地址**，也不要指望运行时能改——webpack `DefinePlugin` 是源码文本替换，
  值在构建期就固定了，改 `.env.production` 后必须重新构建

两端 `.env.production` 当前值都是 `http://localhost:9171`，直接拿去部署会让线上页面打 localhost。

---

## 6. 图片 / 音频地址显示 localhost

**真因不是 `server.address`。** 后端上传接口返回的是**相对路径**
（如 `/img/songPic/1715868053-ab12cd34.jpg`，见 `FileStorageService.store`），
库里历史 URL 也是 `/song/1715868205989` 这种相对形态。绝对地址是**前端拼出来的**：

```js
// MusicUser-frontend/src/mixins/index.js
this.$store.state.configure.HOST + srcUrl
```

`HOST` 来自 `VUE_APP_API_BASE_URL`。所以显示 localhost 就说明该变量是 localhost，
按第 5 条改基址并重新构建即可。改后端 `server.address` 不会有任何效果。

### 静态资源路径不要加前缀

`/song/*`、`/img/**`、`/userImages/**` 是**裸路径**，不带 `/api/v1`：
`<audio>`/`<img>` 发不出 Authorization 头，且库里历史 URL 就是这个形态，加前缀会让全库歌曲集体 404。
反向代理改写路径时不要动这三条。

> `/song/*` 只放行**单层**是刻意收窄：音频落盘路径全部单层，而整体放行 `/song/**`
> 会把歌曲写接口一起放成匿名可调（历史上正是这样）。不要"顺手"改成双层通配。

---

## 7. 上传失败

| 症状 | 真因与解法 |
|------|-----------|
| 提示"文件超过大小上限 10MB / 100MB" | 应用层上限：图片 10MB、音频 100MB（`FileStorageService`），比 multipart 限制更严 |
| HTTP 413 | Spring multipart 限制：dev 单文件 100MB / 请求 200MB，prod 100MB / 110MB。项目自带的前端 nginx **不代理 API**（反代段是注释状态）也未设 `client_max_body_size`，上传直连后端；若你自行加了反代，需另查反代的 body 大小限制 |
| 提示扩展名不允许 | 扩展名白名单校验（小写归一后比对） |
| 扩展名对但仍被拒 | **文件头签名核对**：JPEG/PNG/GIF/WEBP、MP3(ID3)/WAV/FLAC/OGG/M4A(ftyp) 各自校验魔数，改后缀伪装无效 |
| 上传成功但访问 404 | 落盘目录与静态映射不一致，见 `FileConfig` 的 `addResourceLocations`；容器部署检查 `backend-uploads` 卷挂载 |

文件名由服务端生成（时间戳 + 随机串），不采用客户端原名。

---

## 8. 401 / 403 排查

- **请求头是裸 JWT**：`Authorization: eyJ...`，**不要加 `Bearer ` 前缀**（后端不解析该前缀，加了会 401）
- 不支持 `?token=` 查询参数（该回落已关闭，凭据不落盘）
- 匿名入口只有 3 条，按"方法 + 路径"成对放行：`POST /api/v1/admin/sessions`、
  `POST /api/v1/consumers/sessions`、`POST /api/v1/consumers/registrations`。
  同路径的 `DELETE`（登出）必须带令牌
- **403 的三种真因**：① IP 与令牌载荷不符（换网络/代理后常见）；② 角色不匹配；
  ③ 端点未声明 `@RequireUserType`（fail-closed，漏标即 403，`EndpointRoleGuardTest` 会扫出来）
- IP 取值默认**不采信代理透传头**（`security.ip.trust-proxy-headers=false`）。
  部署在反代后若需要按真实客户端 IP 绑定，显式设 `TRUST_PROXY_HEADERS=true`，
  并确认反代会覆写而非追加该头，否则等于允许伪造
- 登出后旧令牌立即失效：写入 `jwt:blacklist:{token}`，TTL 与令牌剩余有效期对齐
- 令牌校验含 `iss=music-backend` / `aud=music-clients`，同密钥在他服务签发的令牌会被拒

---

## 9. 429 / 503

- **429**：命中限流。10 个敏感端点均为 60 秒窗口——登录/注册/头像 5 次，
  改资料（自助与管理员侧）10 次，删用户 10 次，发评论 10 次，删评论 20 次，点赞 30 次。
  计数键按身份（登录用户优先，退回 IP），不拼 URI，因此换 URL 变体绕不过去。
  阈值清单见 [02-RATE-LIMITING.md](02-RATE-LIMITING.md)
- **503**：Redis 不可达且该端点是拒绝型限流（`failOpen=false`，10 处全部如此）。
  这是刻意行为——先修 Redis，不要靠改注解放行
- 全局开关 `rate-limit.enabled`：dev 与 prod yml **均显式设为 `true`**（代码默认值也是 `true`）。
  注意 `src/main/resources/application-test.yml` 里的 `false` **不生效**（被 `src/test/resources/` 同名文件遮蔽），
  所以测试里限流同样是开启状态，靠 `BaseTest` 清 `rate:*` 计数器来隔离
- 计数器到期自动解除；测试基类按 `rate:*` 一次清零

---

## 10. 中文乱码

GBK 主机（如简体中文 Windows）上有三处独立成因，别只改一处：

| 位置 | 机制 |
|------|------|
| Maven 编译与测试控制台 | `.mvn/jvm.config` 已强制 5 个编码参数为 UTF-8；Surefire 另有三个 `-D` 强制子进程 UTF-8 |
| 应用控制台日志 | `logback-spring.xml` 用 `${consoleCharset:-UTF-8}`，可用 JVM 参数 `-DconsoleCharset=GBK` 适配本地终端 |
| 测试库中文种子 | `spring.sql.init.encoding: UTF-8` 必须保留，否则 `schema.sql` 按平台编码读取，歌手名/歌单标题变乱码，依赖中文的断言查不到数据 |

> 第三处历史上曾表现为"假绿"：库里查不到中文数据，但 Redis 有旧缓存命中，测试照样通过。
> 清缓存后才暴露。所以 `BaseTest` 每用例前清 `music:*` 是必要的，不要为了跑得快去掉。

---

## 11. 推了代码但 CI 一条 run 都没有

**真因通常不是 CI 失败，而是触发分支不匹配。** 两个 workflow 的 `on.push.branches` /
`on.pull_request.branches` 都是 `[master]`，仓库也只有 `master` 一个长期分支。
推到别的分支名（如 `main`）不会触发任何 run。

改分支策略时必须同步改 workflow 触发条件与 GitHub 默认分支设置。
CI 也不按 tag 触发、不发布 Release/Pages——打 tag 不会跑发布流水线。

---

## 12. 部署脚本相关

`deploy.sh` / `deploy.ps1` 功能等价，子命令：`start` / `stop` / `restart` / `build` / `logs` / `status` / `clean`。

> ⚠️ **`clean` 会执行 `docker-compose down -v --rmi all`，删除数据卷与镜像**（脚本内有 y/n 确认）。
> 数据库数据在卷里，执行前务必先 `mysqldump` 备份。

脚本只用默认 `docker-compose.yml`，**不引用 prod 文件**；生产部署请直接
`docker-compose -f docker-compose.prod.yml up -d`。脚本打印的 Swagger 地址在默认 prod 档下 404（见第 4 条）。

### prod compose 的 JAVA_OPTS 不生效

`docker-compose.prod.yml` 给后端设了 `JAVA_OPTS`（`-Xms512m -Xmx1024m` + G1GC 等），
但 `Music-backend/Dockerfile` 的 `ENTRYPOINT` 是 **exec 形式且写死了 JVM 参数**：

```dockerfile
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]
```

exec 形式不经过 shell，不会展开 `$JAVA_OPTS`，所以容器实际堆上限是 **512m**，不是 compose 里写的 1024m。
要调堆需改 Dockerfile 的 ENTRYPOINT（或改成 shell 形式引用变量）后重新构建镜像。

---

## 📖 相关文档

- [DEVELOPMENT.md](DEVELOPMENT.md) — 端口、构建链路、环境变量、测试跑法
- [DOCKER_QUICK_START.md](DOCKER_QUICK_START.md) / [DOCKER_DEPLOYMENT.md](DOCKER_DEPLOYMENT.md) — 部署
- [01-AUTHENTICATION.md](01-AUTHENTICATION.md) / [02-RATE-LIMITING.md](02-RATE-LIMITING.md) — 认证与限流
- [API 开发文档.md](../API%20开发文档.md) — 接口契约
- [FIX-LOG-2026-11.md](FIX-LOG-2026-11.md) — 当前真实口径

*最后更新：2026-11-16（按代码事实核对）*