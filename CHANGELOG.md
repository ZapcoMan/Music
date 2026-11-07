# 更新日志

本文件是**对外版本记录的唯一出处**：每个版本在此列一条概要，细节一律指向 [docs/FIX-LOG-2026-11.md](docs/FIX-LOG-2026-11.md) 与 `docs/` 下的专题文档，不在此重复。

版本号规则：相对上一个 tag 有新增用户可见能力 → `minor`；纯修复/文档/样式 → `patch`。

---

## 未发布（v2.0.0 预备）

属破坏性变更，故下一版为 major：返回契约与权限模型都改了，旧调用方式不再成立。

### 变更（破坏性）

- **返回结构统一为 `R<T>`**：`Object` 返回 5 处、裸 `List<PO>`/`Boolean`/`PO` 23 处、raw `R` 17 处全部消除，`R` 删除 `dataMap` 第二条载体，`R.success(T)` 补上漏掉的 `setSuccess`。前端 27 处取值点随之改为 `res.data`，Home/SongPage 里"兼容两种形态"的双分支兜底一并删除。
- **权限模型改为默认拒绝**：37 个写接口全部显式声明角色域（`@RequireUserType` 由 22 处增至 38 处，注解 `value` 改为数组以表达 `{consumer, admin}` 这类集合）；拦截器对未声明角色的写接口直接 403；新增守卫测试扫描全部写接口，漏标注即变红。`/comment/allComment` 补 admin——此前任何消费者令牌可一次拉走全站评论。
- **`/song` 业务接口下沉至 `/song/manage/*`**，白名单由 `/song/**` 收窄为 `/song/*`；静态音频仍匿名可取。

### 安全修复

- **凭据不再随查询串落盘**：令牌只认 `Authorization` 头（三处 `getParameter("token")` 回落全部关闭），查询串进 INFO 日志前按键名脱敏。此前二者叠加会把完整 JWT 写入保留 30 天的日志文件——拿到日志等于拿到 24 小时会话。
- **来源 IP 默认不采信可伪造的透传头**：新增 `security.ip.trust-proxy-headers`，默认 false 只用容器给出的对端地址；同时修掉原兜底分支判空对象写错（查 `ipAddresses` 而非 `ip`）的问题。

### 可靠性与性能

- 延时双删由"每次 new 一个线程 sleep 三秒"改为共享调度池，并把第二次删除挂到事务 `afterCommit`——原先事务超过三秒时二次删除跑在提交之前，双删等于没删。
- 限流 key 不再拼接原始 URI（URL 变体可各建一个计数器从而绕过限流）；URI 仍留在日志中用于定位。
- prod `multipart` 上限由 1024MB 收紧为 100MB/110MB，与 `FileStorageService` 的应用层上限对齐。
- 删除 `getSongsBySimilarTags` / `getSongsByStyle` 及其 XML：两处 `ORDER BY RAND()` 全仓无任何调用点，属不可达死代码。

### 前端工程

- `.eslintignore` 原以 `/*` 结尾，导致 lint 一行源码都没检查过；现只忽略产物目录，档位升到 `plugin:vue/strongly-recommended`，存量 9500+ 项按"排版 warn / 真缺陷分级"处理，两端 0 error。
- `build` 链路的 webpack3 不读 `.env`，使 `VUE_APP_API_BASE_URL` 恒为 undefined、Dockerfile 的 ARG 全部失效；新增零依赖 `config/load-env.js` 接通该链路，实测构建产物内无任何硬编码地址。
- `:key="index"` 换稳定业务 id 共 12 处；`TheAside`/`TheHeader` 匿名监听器改为具名并在 `beforeDestroy` 解除；`LoginIn`/`SignUp` 的跳转定时器补 `clearTimeout`。
- 根 `.gitignore` 的 `.env` 规则会吞掉新建设置文件，已逐文件精确放行（不通配，避免连带放行后端口令文件）。

### 测试

用例 142 → **215**：管理接口鉴权矩阵 44 项（22 端点 × 匿名 401 / 消费者 403）、消费者端点矩阵 20 项、写接口角色守卫 1 项、令牌载体回归 5 项、`R` 单一载体与 `success` 标志 2 项；删除 3 项测试已不存在的 `dataMap` 链式 API 的用例。

---

## v1.1.0 — 2026-11-07

安全收口批次：鉴权覆盖面、身份来源、凭据暴露、明文密钥。定版于仓库转公开之后、对外可访问之前。

### 新增能力

- **角色隔离覆盖管理写接口**：`@RequireUserType("admin")` 从 7 处增至 22 处，补齐歌曲、歌手、歌单、歌单关联各自的 add / update / delete 与四类图片音频上传。admin 与 consumer 共用同一签名密钥，缺这层校验时任何已登录用户都能增删改曲库。只读接口未加角色限制——用户端本就依赖它们。

### 修复

- **`/song` 写接口不再匿名可调**：为放行静态音频而排除的 `/song/**` 与业务接口同前缀，导致增删改与文件上传处于未认证状态。静态文件与业务接口同为单层，无法用路径 pattern 区分，也不能按扩展名收窄（库里 50+ 条历史 URL 形如 `/song/1715868205989` 没有后缀），故把业务接口整体下沉一层到 `/song/manage/*`，白名单相应收窄为 `/song/*`。前端调用点同步，其中 `addSong` 走原生 XHR 且原先不带任何 header，一并补上 `Authorization`。
- **评分人身份改自令牌**：`POST /rank/add` 原先直接采用入参 `consumerId`，任何登录用户可替任意账号打分；评分同时是协同过滤、内容召回与精排特征的直接输入，污染它等于污染推荐结果。这是曲库里唯一一处"身份由前端决定"的写接口。
- **移除源码与 Swagger 中的默认管理员凭据**：`Login.vue` 的表单预填与 `AdminController` 的 `@Parameter(example=...)` 两处同源暴露（Swagger UI 在鉴权白名单内，等于公开预填登录表单）。实测种子哈希对 123456、password、admin、admin123 等七个候选全部 `checkpw=false`，那行预填从来没真正可用。
- **dev 配置去明文密钥**：JWT 密钥改为 `${JWT_SECRET:<明确占位串>}`，dev 的 DB / Redis / Druid 口令一律可环境变量覆盖（本地默认值保留，免配置仍能起）；prod 的 Druid `admin/admin` 改为不给任何"看起来可用"的默认值。
- **CI**：新增 `.github/workflows/ci.yml`，push/PR 到 `main` 时跑与本地完全相同的
  `mvn -B clean verify`（JaCoCo 四项门禁），JDK 17 + Maven 缓存 + 20 分钟超时，
  失败时上传 surefire 报告。Redis 以 step 内 `docker run redis:7 redis-server --requirepass admin`
  提供（`services.options` 的写法会让 docker 收不到容器参数、首跑直接 exit 125），
  就绪判定用 `redis-cli ping` 断言 `PONG` 而非只探端口。**已跑通转绿。**
- **测试隔离（CI 抓出来的两个假绿）**：
  `SongListControllerTest` 原先硬编码 `id=1` 修改并删除 `schema.sql` 的种子歌单行，
  使任何 `INNER JOIN song_list` 的用例随测试类顺序时红时绿——CI（Linux）红、本地（Windows）绿；
  现改为只操作本类自己新增的那一行，并断言种子行未被伤及。
  `application-test.yml` 补 `spring.sql.init.encoding: UTF-8`：此前 H2 按平台编码 GBK 读
  UTF-8 的 `schema.sql`，中文种子全部变乱码，依赖中文的断言实际查不到数据，
  能过纯粹是靠 Redis 历史缓存命中绕过了数据库。
- **许可证**：补 Apache License 2.0 的 `LICENSE` 文件（官方原文逐字未改），
  README 新增「许可证」章节，使已存在的许可徽章有出处。

### 测试与质量

- 用例从 142 增至 **146**，补齐此前**完全缺失**的一类断言：未登录必须 401、角色不符必须 403、
  伪造的身份入参必须不生效。基类 `BaseTest` 预置管理员令牌，"鉴权缺失"这类缺陷不会让任何
  既有用例变红——这四条断言才是本轮的实质产出，没有它们，修复本身不可验证。
- 冷缓存 + 强制反序 + `mvn -B clean verify` 三跑闭环：146 项全绿，0 失败 0 错误 0 跳过，
  JaCoCo 四项门禁达标。

### 需运维侧跟进（本轮代码无法替代）

- **历史 commit 里那串旧 JWT 密钥无法靠改文件消除**。生产若复用过同一串，必须轮换
  `JWT_SECRET`（顺带 DB / Redis 口令）才能让旧串作废——旧串对任何能读 git 历史的人始终有效，
  可离线自签含 admin 角色在内的合法令牌。
- 部署需注入的环境变量：`JWT_SECRET`（prod 无默认值，漏配即启动失败）、`DB_USERNAME`、
  `DB_PASSWORD`、`REDIS_PASSWORD`；prod 若要开启 Druid 监控台，还须显式给
  `DRUID_LOGIN_USERNAME` / `DRUID_LOGIN_PASSWORD`。
- 管理员口令：本轮未改种子哈希（换口令需要你定新值，不该把某个具体口令写进公开种子）。
  种子哈希对所有用该文件部署的实例取值相同且 salt 公开，可离线爆破，建议部署后立即改。
- 游客浏览策略：本轮歌曲详情 / 列表 / 搜索一律需登录。"游客可搜可看列表但不可听"依赖
  搜索响应裁剪——必须去掉 `url`/`pic`/`lyric`，因为音频文件本身匿名可取，
  任何匿名返回 `url` 的接口都等于匿名开放完整听歌能力。留作后续版本。

---

## v1.0.0 — 2026-11-07

首个版本锚。相比上游原始代码，完成三批共 65 次逐簇修复，后端与前端首次同时构建通过、测试全绿。

### 新增能力

- **标签系统落地**：`/tag/hot`、`/tag/ofSong`、`/tag/songs`、`/tag/ofSinger`、`/tag/song/update`、`/tag/singer/update`、`/tag/batchAdd` 七个接口；`song.tags` 逗号串与 `song_tag` 中间表双轨同步，歌手侧同理解析。
- **歌单收藏打通读写两端**：`collect.type=1` 此前只写不读；新增 `GET /collect/songList`，并在写路径补齐缓存失效。
- **主动登出**：`POST /consumer/logout`、`POST /admin/logout`，令牌进黑名单、TTL 与剩余有效期对齐（此前黑名单只读不写）。
- **私密资料专用接口** `GET /consumer/profile`：身份只取自令牌，不接受 `id` 参数；`/consumer/selectByPrimaryKey` 降为公开投影（仅 `id`/`username`/`avator`）。
- **操作审计**：`operation_log` 表（V5 迁移）+ `OperationLogMapper` 七条绑定语句 + 关键写操作注入点 + 每日 03:30 按保留期清理。
- **数据补齐迁移** V6–V8、V11：标签、搜索历史、空歌单歌曲、歌单收藏、孤儿引用与 `"null"` 字面量脏数据；字符集 utf8 → utf8mb4（V9/V10）。

### 安全修复

- 鉴权从"可选"变强制，角色隔离 `@RequireUserType` 生效，消费者令牌不再能调管理接口。
- 消除 IDOR：收藏、搜索历史、评论、资料修改等接口的身份统一从令牌取，不再接受客户端传入 userId。
- 机密去默认值：`JWT_SECRET`、`DB_PASSWORD`、`REDIS_PASSWORD` 缺失即启动失败；compose 改为 `${VAR:?}` 必填插值；mysql/redis 不再映射宿主端口。
- 口令不再外泄：`Consumer.password` 加 `@JsonIgnore`，修掉"返回哈希→回填表单→二次加密写库→账号再也登不上"的链路。
- 上传集中校验（扩展名白名单、服务端生成文件名、大小分级）；CORS 收敛为精确来源白名单；限流改 `INCR` 原子计数并扩展到写操作类接口。
- Redis 多态反序列化改用 `BasicPolymorphicTypeValidator` 白名单。

### 算法与正确性修复

- 六路召回全部有输出（此前内容/个性化召回恒返回 0 首、CF 只召回 ID 更大的用户、搜索历史关键词空转）。
- 评分量纲纠正为 10 分制、兜底改中性 5.0、新增 `ratingCount` 置信特征；新歌双重提权与 `null` 语义分离。
- `SELECT DISTINCT ... ORDER BY` 在 `ONLY_FULL_GROUP_BY` 下报 3065 的搜索历史查询改为 `GROUP BY + MAX()`。
- 静态 `LIMIT` 打断 PageHelper 分页的问题拆分修复；列表接口补齐缺失的 `tags` 列查询。

### 契约与错误码

- `R.error` 正确置 `success=false`；7 处删除接口改为真正的 `DELETE`。
- 405/415/404/缺参不再被全局异常处理吞成 500；参数类错误码统一为 `20002`。

### 性能

- 冷启动端到端 592/343/203ms，缓存命中 369/249/226ms；播放进度等高频瞬时状态不再每帧序列化整个歌单。
- 缓存键收敛到 `CacheKeys` 单一出处，延时双删与多键同时失效。

### 测试与质量

- 后端 `mvn -B clean verify`：**142 例全过，0 失败 0 错误 0 跳过**，含 JaCoCo 覆盖率门禁；16 个空壳用例中 10 个补真实断言、6 个因目标接口不存在而删除。
- 前端：用户端 + 管理端共 **24 例全过**；`sass-loader` 降到与 webpack 3 同期的 7.x 后**两端构建首次 `Build complete`**。
- 测试基类共享 Spring 上下文，单类耗时从约 41s 降至套件总计约 80s。

### 破坏性变更（升级时必读）

- 三个口令类环境变量与 `JWT_SECRET` 无默认值，部署时必须提供，否则启动失败。
- 删除类接口由 `GET` 改为 `DELETE`，前端两端已同批改造；第三方调用方需同步。
- 无 Token 调业务接口现在返回 401。

### 边界 / 未实测

- 数据库中的标签、搜索历史、歌单收藏等补齐数据为**模拟数据**，用于让功能可运行，不代表真实用户行为。
- `docker-compose` 的"缺变量即拒绝启动"行为未经实机验证（本机无 docker）。
- 用户 `consumer 33` 的口令是一次性随机 BCrypt，明文未记录在任何地方，该账号事实不可登录（V8 保留占位以维持引用完整）。
