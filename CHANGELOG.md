# 更新日志

本文件是**对外版本记录的唯一出处**：每个版本在此列一条概要，细节一律指向 [docs/FIX-LOG-2026-11.md](docs/FIX-LOG-2026-11.md) 与 `docs/` 下的专题文档，不在此重复。

版本号规则：相对上一个 tag 有新增用户可见能力 → `minor`；纯修复/文档/样式 → `patch`。

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
