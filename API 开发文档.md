# API 开发文档

> ⚠️ **口径声明（2026-11-16 按源码逐项核对）**：全部业务接口挂 `/api/v1` 资源式路径
> （静态资源 `/song/*`、`/img/**`、`/userImages/**` 例外）；登录/登出为
> `POST/DELETE /api/v1/{admin|consumers}/sessions`；注册为 `POST /api/v1/consumers/registrations`；
> `R<T>` 已删除 `dataMap`；读接口不再匿名可调（角色默认拒绝，公开入口仅登录/注册）。
>
> 本轮修正的旧口径：更新接口是 **PATCH 而非 PUT**（仅标签关联用 PUT）；`@Valid` **并非**
> 覆盖全部增删改接口，实际只有 7 处；校验失败的业务 code 是 **20002**（不是 400）；
> 缓存表里的"命中率 95%+"与"性能提升 5-10 倍"无实测出处，已删除。
> 接口全量清单以 Swagger UI（**仅 dev 档**）为准，配置口径见
> [docs/01](docs/01-AUTHENTICATION.md)/[02](docs/02-RATE-LIMITING.md)/
> [05](docs/05-PARAM-VALIDATION.md)，开发跑法见 [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md)。

## 📋 概述

本文档提供音乐网站系统的完整 API 开发指南，包括接口规范、认证方式、参数校验、日志审计等内容。

---

## 🔗 快速访问

### Swagger UI（在线文档，**仅 dev 档**）
- **地址**: http://localhost:9171/swagger-ui.html
- **说明**: 可视化 API 文档，支持在线测试
- **档位限制**: `application-prod.yml` 把 `springdoc.api-docs.enabled` 与
  `swagger-ui.enabled` 都设为 `false`，**prod 档下这两个地址一律 404**。
  两个 compose 文件的后端默认档位都是 prod，本地联调需在 `.env` 设 `SPRING_PROFILES_ACTIVE=dev`。

### OpenAPI JSON（同样仅 dev 档）
- **地址**: http://localhost:9171/v3/api-docs
- **说明**: OpenAPI 3.0 规范的 JSON 格式文档
- 扫描范围由 `springdoc.group-configs.packages-to-scan: com.shanzhu.music.controller` 限定

> 端口 9171 在 dev/prod/test 三个档位一致；档位由 Maven profile 过滤
> `application.yml` 的 `@spring.profiles.active@` 决定，默认 `dev`。

📖 SpringDoc 配置详情：[SPRINGDOC_OPENAPI_CONFIG.md](docs/SPRINGDOC_OPENAPI_CONFIG.md)

---

## 🔐 认证与授权

### JWT Token 认证

所有需要认证的接口需在 Header 中携带 JWT Token：

```http
Authorization: eyJhbGciOiJIUzI1NiJ9...
```

### Token 获取

#### 管理员登录
```bash
POST http://localhost:9171/api/v1/admin/sessions
Content-Type: application/x-www-form-urlencoded

name=admin&password=***
```

**响应：**
```json
{
  "success": true,
  "code": 20000,
  "message": "登录成功",
  "data": {
    "name": "admin",
    "token": "eyJhbGciOiJIUzM4NCJ9...",
    "expiresIn": 86400
  }
}
```

#### 前台用户登录
```bash
POST http://localhost:9171/api/v1/consumers/sessions
Content-Type: application/x-www-form-urlencoded

username=testuser&password=***
```

**响应：**
```json
{
  "success": true,
  "code": 20000,
  "message": "登录成功",
  "data": {
    "userMsg": {
      "id": 1,
      "username": "testuser"
    },
    "token": "eyJhbGciOiJIUzM4NCJ9...",
    "expiresIn": 86400
  }
}
```

### 安全特性

- ✅ **IP 地址绑定**：Token 与客户端 IP 绑定，防止盗用
- ✅ **Token 有效期**：24 小时自动过期
- ✅ **Token 黑名单**：支持主动登出（Redis 实现）
- ✅ **API 限流**：全项目共 **10 个** `@RateLimit` 标注，窗口一律 60 秒，
  键按"有令牌用用户、无令牌用 IP"：

| 接口 | 限流键前缀 | 60 秒内上限 |
|------|-----------|-----------|
| `POST /api/v1/admin/sessions` | `login:admin:` | 5 |
| `POST /api/v1/consumers/sessions` | `login:consumer:` | 5 |
| `POST /api/v1/consumers/registrations` | `register:consumer:` | 5 |
| `PATCH /api/v1/consumers/profile` | `update:consumer:` | 10 |
| `PATCH /api/v1/consumers/{id}` | `update:consumer:admin:` | 10 |
| `DELETE /api/v1/consumers/{id}` | `delete:consumer:` | 10 |
| `POST /api/v1/consumers/{id}/avatar` | `upload:avatar:` | 5 |
| `POST /api/v1/comments` | `comment:add:` | 10 |
| `DELETE /api/v1/comments/{id}` | `comment:delete:` | 20 |
| `POST /api/v1/comments/{id}/likes` | `comment:like:` | 30 |

> 未标注 `@RateLimit` 的接口不受限流拦截器约束（它只对标注生效）。
> 歌曲/歌单/歌手的写接口目前都没有标注。

📖 详细说明：[01-AUTHENTICATION.md](docs/01-AUTHENTICATION.md) | [02-RATE-LIMITING.md](docs/02-RATE-LIMITING.md)

---

## 📝 参数校验

### 入参形态（三类，发错 Content-Type 会直接 400/415）

| 形态 | 适用接口 | 提交方式 |
|------|---------|---------|
| **JSON `@RequestBody`** | 注册 `POST /consumers/registrations`、改资料 `PATCH /consumers/profile`、管理员改用户 `PATCH /consumers/{id}`、歌手 `POST /singers` 与 `PATCH /singers/{id}` | `Content-Type: application/json` |
| **JSON 数组体** | 标签关联 `PUT /songs/{songId}/tags`、`PUT /singers/{singerId}/tags`（`List<String>`，可为空）、批量建标签 `POST /tags`（`List<TagSaveRequest>`） | `application/json`，体是数组不是对象 |
| **表单 / 查询参数** | 两个登录与登出、`POST /comments`、`POST /ranks`、歌曲与歌单的全部写接口、收藏、歌单-歌曲关联、搜索历史 | `application/x-www-form-urlencoded` 或查询串 |
| **multipart 上传** | `POST /songs`（带 `file`）、`POST /songs/{id}/picture`、`POST /songs/{id}/audio`、`POST /singers/{id}/picture`、`POST /song-lists/{id}/picture`、`POST /consumers/{id}/avatar` | `multipart/form-data`，文件字段名固定 `file` |

> 登录接口是 `@RequestParam` 绑定，**发 JSON 体会因参数绑不上而 400**；
> 两端登录页均以 `URLSearchParams` 发表单。歌曲创建虽然路径是 `POST /songs`，
> 但它同时收元数据与音频文件，属 multipart，不是 JSON。

**改资料是"不填即不改"**：`PATCH /consumers/profile` 与 `PATCH /consumers/{id}` 收
`ConsumerProfileRequest`（字段 `password`/`sex`/`phoneNum`/`email`/`birth`/`introduction`/`location`），
刻意**不用 `@NotBlank`**——`@Size`/`@Pattern`/`@Email` 对 null 一律放行。
前端留空的字段不要发送，后端收到 null 就保持原值；`password` 为 null 时原密码不变。
它与注册用的 `ConsumerRequest` 是两个 DTO：注册必须有用户名与口令，改资料不动登录凭据。

### 校验规则

项目使用 Bean Validation，但 **`@Valid` 只标注在 7 处入参上，并非覆盖全部增删改接口**：

| 接口 | 入参 | 绑定方式 | 触发的异常分支 |
|------|------|---------|--------------|
| `POST /api/v1/consumers/registrations` | `ConsumerRequest` | `@RequestBody` | `MethodArgumentNotValidException` |
| `PATCH /api/v1/consumers/profile` | `ConsumerProfileRequest` | `@RequestBody` | 同上 |
| `PATCH /api/v1/consumers/{id}` | `ConsumerProfileRequest` | `@RequestBody` | 同上 |
| `POST /api/v1/singers` | `SingerRequest` | `@RequestBody` | 同上 |
| `PATCH /api/v1/singers/{id}` | `SingerRequest` | `@RequestBody` | 同上 |
| `POST /api/v1/comments` | `CommentRequest` | 表单绑定 | `BindException` |
| `POST /api/v1/ranks` | `RankRequest` | 表单绑定 | `BindException` |

约束集中在 `entity/dto/` 下的 6 个 Request 类，实际用到的注解与真实取值：

| 注解 | 项目内实际用例 |
|------|--------------|
| `@Size`（13 处） | `@Size(min = 2, max = 50, message = "用户名长度必须在2-50之间")`、`@Size(max = 255, message = "评论内容不能超过255字")` |
| `@NotBlank`（7 处） | 用户名、密码等必填文本 |
| `@NotNull`（5 处） | 必填 ID 与数值 |
| `@Pattern`（3 处） | `@Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")` |
| `@Min` / `@Max`（各 3 处） | 评分 `@Min(1) @Max(10)`；评论类型 `@Min(0) @Max(1)` |
| `@Email`（3 处） | `@Email(message = "邮箱格式不正确")` |
| `@AssertTrue`（1 处） | `CommentRequest`：歌曲评论要带 songId，歌单评论要带 songListId |

> **没有 `@Valid` 的写接口**（歌曲、歌单、收藏、歌单-歌曲关联、搜索历史、标签）
> 不走 Bean Validation：上传类由 `FileStorageService` 做校验并抛 `BusinessException`，
> 其余依赖 Service 层判断与数据库约束。给这些接口发非法值不一定得到 400。

### 校验失败响应

```json
{
  "success": false,
  "code": 20002,
  "message": "用户名长度必须在2-50之间"
}
```

HTTP 状态码同时为 **400**（三个校验异常处理方法都标了 `@ResponseStatus(HttpStatus.BAD_REQUEST)`），
业务 code 统一取 `ResultCodeEnum.PARAM_ERROR.getCode()` = **20002**。
`MethodArgumentNotValidException`、`BindException`、`ConstraintViolationException`
三个分支返回的 code 一致，不存在"参数错误有两个 code 来源"的情况。

📖 详细说明：[05-PARAM-VALIDATION.md](docs/05-PARAM-VALIDATION.md)

---

## 📊 响应格式

### 统一响应结构

```json
{
  "success": true,      // 是否成功；失败响应现在会置 false
  "code": 20000,        // 20000 成功；20001 未知错误；20002 参数错误；20003 空指针；20004 客户端错误
  "message": "操作成功", // 提示信息（字段名是 message，不是 msg）
  "data": {}            // 返回数据（可选，为 null 时不出现在 JSON 里）
}
```

> `dataMap` 字段已删除，所有数据一律走泛型 `R<T>` 的 `data`（多字段用专门 Vo）。

字段说明以 `common/R.java` 与 `common/ResultCodeEnum.java` 为准，本文不再重复维护。

### 常见状态码

HTTP 状态码与业务 code 是两层，不要混看：

| HTTP | 由谁决定 | 场景 | 响应体 code |
|------|---------|------|-----------|
| 200 | 正常返回 | 请求进入业务层并返回 R（**业务失败也返回 200**，需看 `success`） | 20000 或失败码 |
| 200 | GlobalExceptionHandler | `BusinessException`（对象不存在、越权、用户名重复、上传文件非法等） | **400**（默认）或 **500**（存储不可用） |
| 400 | GlobalExceptionHandler | `@Valid` 校验失败、缺少必填参数、请求体不可读 | 20002 |
| 401 | JwtAuthenticationInterceptor | 未携带 Token、Token 无效/过期、iss/aud 不符、已被登出吊销 | 401 |
| 403 | JwtAuthenticationInterceptor | IP 与令牌绑定不符；`@RequireUserType` 角色不匹配；端点漏标注解（fail-closed） | 403 |
| 404 | GlobalExceptionHandler | 资源不存在 / 接口不存在 | 404 |
| 405 | GlobalExceptionHandler | 请求方法不支持 | 405 |
| 413 | GlobalExceptionHandler | 上传文件超过大小上限 | 413 |
| 415 | GlobalExceptionHandler | 不支持的 Content-Type | 415 |
| 429 | RateLimitInterceptor | 超过限流阈值 | 429 |
| 503 | RateLimitInterceptor | **Redis 不可用时限流 fail-closed**，直接拒绝请求 | 503 |
| 500 | GlobalExceptionHandler | 空指针（20003）/ 其他未捕获异常（20001） | 20003 或 20001 |

业务 code 只有五个枚举值：20000 成功 / 20001 未知错误 / 20002 参数错误 / 20003 空指针 / 20004 客户端错误。
但**拦截器拒绝与 `BusinessException` 会把 HTTP 状态码直接当 code 用**（401/403/404/405/413/415/429/503/400/500），
所以 code 的取值域不止那五个。判断成败一律以 `success` 为准。

> ⚠️ 两个 Redis 依赖点的降级方向**相反**：JWT 黑名单检查在 Redis 异常时 **fail-open 放行**
> （记 error 日志便于告警），限流拦截器在 Redis 异常时 **fail-closed 返回 503**。
> 因此 Redis 挂掉的表现是"已登出的令牌又能用了，但被限流的接口全部 503"。
>
> `BusinessException` 刻意保持 HTTP 200：两套前端判成功只看 `res.code == 20000 || res.success === true`，
> 写成 4xx 会让 axios 走 reject 分支而看不到 message。

---

## 📚 API 模块

### 主要接口模块

| 模块 | 路径前缀 | 实际能力 |
|------|---------|---------|
| **管理员会话** | `/api/v1/admin/sessions` | 登录 / 登出（无其他管理端点） |
| **用户** | `/api/v1/consumers` | 注册、登录/登出、资料读写、分页、头像上传 |
| **歌手** | `/api/v1/singers` | 增删改查、全量、分页、按 name/sex 查、歌手下歌曲、图片上传 |
| **歌曲** | `/api/v1/songs` | 创建（含音频）、改/删、图片与音频上传、按 id/songName/keyword 查、全量、分页 |
| **歌单** | `/api/v1/song-lists` | 增删改查、全量、分页、按 title/keyword/style 查、图片上传、个性化推荐 |
| **收藏** | `/api/v1/collections` | 收藏/取消（歌曲与歌单两类）、我的收藏、收藏的歌单、全量（admin） |
| **评论** | `/api/v1/comments` | 发表、删除、点赞、按 id 查、列表（admin）；子资源见下 |
| **评分** | `/api/v1/ranks` | 仅歌单评分（1-10，重复提交即改分）；查询在歌单子资源下 |
| **标签** | `/api/v1/tags` | 热门标签、按标签查歌曲、批量新建；**无标签本身的改/删/全量列表** |
| **搜索历史** | `/api/v1/search-history` | 记录、查询、热词、删除、清理过期 |
| **统计** | `/api/v1/statistics` | 仅 `GET /overview` 一个端点 |

> 评论、评分、标签、歌单-歌曲关联都有一部分端点挂在**别的资源前缀**下（子资源风格），
> 因此 `CommentController`/`RankController`/`TagController`/`ListSongController`
> 不声明类级 `@RequestMapping`，逐方法写绝对路径。

### 公开入口（按"方法 + 路径"成对放行）

`JwtAuthenticationInterceptor.ANONYMOUS_ENTRIES` 只有三条：

- `POST /api/v1/admin/sessions` - 管理员登录
- `POST /api/v1/consumers/sessions` - 用户登录
- `POST /api/v1/consumers/registrations` - 用户注册

`WebMvcConfig.excludePathPatterns` 另外排除**与 HTTP 方法无关**的入口：
`/img/**`、`/song/*`（**只单层**）、`/userImages/**`、`/v3/api-docs/**`、
`/swagger-ui/**`、`/swagger-ui.html`、`/actuator/health`、`/error`。

> 为什么登录不放进 `excludePathPatterns`：它认路径不认方法，放行 `POST /sessions`
> 会把 `DELETE /sessions`（登出，必须带令牌）一起放成匿名可调。
>
> 为什么音频只放行单层 `/song/*`：落盘路径就是 `/song/{时间戳-随机名}`，全部单层且多数无扩展名。
> 整体放行 `/song/**` 会把歌曲写接口一起放成匿名；按扩展名收窄又会让库里 50+ 条无后缀历史 URL 全 404。
> 这些前缀必须保持裸路径——`<audio>`/`<img>` 标签发不出 Authorization 头。
>
> 早期文档中 `GET /singer/allSinger`、`GET /song/allSong` 等"免登录查询"已不存在。
> `/actuator/health` 之外的 actuator 端点不在排除名单内，因此同样要过 JWT 拦截器。

### 端点全量清单（按源码核对，角色列为 `@RequireUserType` 生效值）

"类级"表示该方法未标注、继承 Controller 类级声明；写接口的入参形态见上文"入参形态"。

#### 会话与用户

| 方法 | 路径 | 角色 |
|------|------|------|
| POST | `/api/v1/admin/sessions` | 匿名（限流 5/60s） |
| DELETE | `/api/v1/admin/sessions` | admin |
| POST | `/api/v1/consumers/sessions` | 匿名（限流 5/60s） |
| DELETE | `/api/v1/consumers/sessions` | consumer + admin |
| POST | `/api/v1/consumers/registrations` | 匿名（限流 5/60s） |
| GET | `/api/v1/consumers/profile` | 类级 consumer + admin |
| PATCH | `/api/v1/consumers/profile` | consumer（限流 10/60s） |
| POST | `/api/v1/consumers/{id}/avatar` | consumer（限流 5/60s） |
| GET | `/api/v1/consumers/{id}` | 类级 consumer + admin |
| PATCH | `/api/v1/consumers/{id}` | admin（限流 10/60s） |
| DELETE | `/api/v1/consumers/{id}` | admin（限流 10/60s） |
| GET | `/api/v1/consumers/page` | admin |

#### 歌手 / 歌曲 / 歌单

| 方法 | 路径 | 角色 |
|------|------|------|
| POST | `/api/v1/singers` | admin |
| GET | `/api/v1/singers`、`/page`、`/{id}`、`/{id}/songs` | 类级 consumer + admin |
| GET | `/api/v1/singers?name=`、`?sex=` | 类级 consumer + admin |
| PATCH | `/api/v1/singers/{id}` | admin |
| DELETE | `/api/v1/singers/{id}` | admin |
| POST | `/api/v1/singers/{id}/picture` | admin |
| POST | `/api/v1/songs` | admin |
| GET | `/api/v1/songs`、`/page`、`/{id}` | 类级 consumer + admin |
| GET | `/api/v1/songs?songName=`、`?keyword=` | 类级 consumer + admin |
| PATCH | `/api/v1/songs/{id}` | admin |
| DELETE | `/api/v1/songs/{id}` | admin |
| POST | `/api/v1/songs/{id}/picture`、`/{id}/audio` | admin |
| POST | `/api/v1/song-lists` | admin |
| GET | `/api/v1/song-lists`、`/page`、`/{id}` | 类级 consumer + admin |
| GET | `/api/v1/song-lists?title=`、`?keyword=`、`?style=` | 类级 consumer + admin |
| GET | `/api/v1/song-lists/recommended` | **仅 consumer**（admin 令牌会 403） |
| PATCH | `/api/v1/song-lists/{id}` | admin |
| DELETE | `/api/v1/song-lists/{id}` | admin |
| POST | `/api/v1/song-lists/{id}/picture` | admin |

#### 互动（收藏 / 评论 / 评分 / 关联）

| 方法 | 路径 | 角色 |
|------|------|------|
| POST | `/api/v1/collections` | consumer（`type` 0=歌曲 1=歌单） |
| DELETE | `/api/v1/collections` | consumer（按 songId 或 songListId 定位） |
| GET | `/api/v1/collections/mine`、`/song-lists` | 类级 consumer + admin |
| GET | `/api/v1/collections` | admin |
| POST | `/api/v1/comments` | consumer（限流 10/60s） |
| DELETE | `/api/v1/comments/{id}` | consumer + admin（限流 20/60s） |
| POST | `/api/v1/comments/{id}/likes` | consumer（限流 30/60s） |
| GET | `/api/v1/comments/{id}` | 类级 consumer + admin |
| GET | `/api/v1/comments` | admin |
| GET | `/api/v1/songs/{songId}/comments` | 类级 consumer + admin |
| GET | `/api/v1/song-lists/{songListId}/comments` | 类级 consumer + admin |
| GET | `/api/v1/song-lists/{songListId}/frequent-words` | 类级 consumer + admin |
| POST | `/api/v1/ranks` | consumer |
| GET | `/api/v1/song-lists/{songListId}/rank` | 类级 consumer + admin |
| GET | `/api/v1/song-lists/{songListId}/songs` | 类级 consumer + admin |
| POST | `/api/v1/song-lists/{songListId}/songs` | admin |
| DELETE | `/api/v1/song-lists/{songListId}/songs/{songId}` | admin |

#### 标签 / 搜索历史 / 统计

| 方法 | 路径 | 角色 |
|------|------|------|
| GET | `/api/v1/tags/hot`、`/api/v1/tags/songs` | 类级 consumer + admin |
| POST | `/api/v1/tags` | admin |
| GET | `/api/v1/songs/{songId}/tags` | 类级 consumer + admin |
| PUT | `/api/v1/songs/{songId}/tags` | admin |
| GET | `/api/v1/singers/{singerId}/tags` | 类级 consumer + admin |
| PUT | `/api/v1/singers/{singerId}/tags` | admin |
| POST / GET / DELETE | `/api/v1/search-history` | consumer |
| GET | `/api/v1/search-history/keywords` | consumer |
| DELETE | `/api/v1/search-history/expired` | consumer |
| GET | `/api/v1/statistics/overview` | admin |

> **PUT 只出现在标签关联的两处**，其余更新一律 PATCH。
> 未声明 `@RequireUserType` 的 Controller 方法会被 fail-closed 守卫直接 403 并记 error 日志；
> 该守卫只作用于 `HandlerMethod`，静态资源与 404 兜底不受影响。

📖 运行时可视化清单请访问 Swagger UI（**仅 dev 档**，prod 档 404）

---

## 🔍 请求审计日志

### 日志记录内容

`RequestResponseLoggingInterceptor`（order=0，最先执行）记录请求与响应，
排除 `/actuator/**`、`/swagger-ui/**`、`/v3/api-docs/**`、`/error`。

> ⚠️ **档位差异**：请求/响应日志用的是 `log.info`，而 `application-prod.yml` 设了
> `logging.level.com.shanzhu.music.interceptor: WARN`（root 也是 WARN），
> 因此 **prod 档默认不落盘这两段审计日志**；慢请求（`log.warn`）与异常（`log.error`）仍会记录。
> dev 档该包为 DEBUG，审计日志完整输出。要在 prod 看审计，需显式把该 logger 调回 INFO。

#### 请求日志
- ✅ 请求ID（唯一标识）
- ✅ 请求时间
- ✅ 请求方法（GET/POST/PUT/DELETE）
- ✅ 请求URL
- ✅ 查询参数
- ✅ 客户端IP（支持代理）
- ✅ User-Agent
- ✅ Content-Type
- ✅ 请求头（脱敏处理）
- ✅ 请求体（POST/PUT/PATCH，脱敏处理）

#### 响应日志
- ✅ 请求ID（与请求对应）
- ✅ 响应状态码
- ✅ 耗时（毫秒）
- ✅ 异常信息

### 日志示例

```
========== 请求开始 ==========
【请求ID】      : 1715097600000-1234
【请求时间】    : 2026-05-07T20:45:30.123
【请求方法】    : POST
【请求URL】     : /api/v1/admin/sessions
【客户端IP】    : 192.168.1.100
【请求体】      : name=admin&password=***

========== 请求结束 ==========
【请求ID】      : 1715097600000-1234
【响应状态】    : 200
【耗时】        : 156 ms
【异常信息】    : 无
============================
```

### 敏感信息脱敏

三条通道各有脱敏函数，覆盖范围不同：

| 通道 | 函数 | 规则 |
|------|------|------|
| 请求头 | `maskSensitiveHeaders` | `Authorization` 保留前 **15** 个字符 + `***`；`Cookie` 整体 `***` |
| 请求体 | `maskSensitiveBody` | JSON 体替换 `"password"`/`"token"` 的值；form 体按 `(password\|passwd\|token\|secret)=` 替换 |
| 查询串 | `maskQueryString` | `(token\|password\|passwd\|secret\|authorization)=` 一律 `***`（大小写不敏感） |

> 查询串脱敏是补上的第三条通道：请求头与请求体早就脱敏，唯独 query string 原样输出，
> 而令牌曾支持从 `?token=` 进入，等于把完整 JWT 随 INFO 日志留档 30 天。
> 该回落入口已关闭（令牌只认 `Authorization` 头，且是**裸 JWT，不带 `Bearer ` 前缀**），
> 脱敏作为第二道防线保留。
>
> 请求体日志长度上限 `MAX_PAYLOAD_LENGTH = 10000` 字符，超出会截断后记录。

### 慢请求监控

超过 3 秒的请求会发出警告：
```
⚠️ 慢请求警告 - URL: /api/v1/songs, 耗时: 4523 ms
```

📖 详细说明：[04-REQUEST-AUDIT.md](docs/04-REQUEST-AUDIT.md)

---

## 🧪 测试与调试

### 使用 Swagger UI 测试

前提：后端跑在 **dev 档**（prod 档 SpringDoc 关闭，页面 404）。

1. 访问 http://localhost:9171/swagger-ui.html
2. 先调登录接口拿 Token（`POST /api/v1/admin/sessions` 或 `/api/v1/consumers/sessions`）
3. 选择要测试的接口，点击 "Try it out"
4. 在 Authorization 参数里填**裸 Token**（不要加 `Bearer ` 前缀，后端不解析该前缀，加了会 401）
5. 点击 "Execute" 执行请求
6. 查看响应结果

> Swagger 的 Authorization 是普通参数框而非 HTTP 认证方案：本项目令牌走自定义头校验，
> 未配置 OpenAPI securityScheme，因此不能指望右上角 "Authorize" 按钮全局注入。

### 使用 cURL 测试

#### 登录获取 Token（表单，不是 JSON）
```bash
curl -X POST http://localhost:9171/api/v1/admin/sessions \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "name=admin&password=***"
```

#### 携带 Token 访问读接口
```bash
curl -X GET http://localhost:9171/api/v1/songs \
  -H "Authorization: eyJhbGciOiJIUzM4NCJ9..."
```

#### 改资料（JSON + @Valid）
```bash
curl -X PATCH http://localhost:9171/api/v1/consumers/profile \
  -H "Authorization: eyJhbGciOiJIUzM4NCJ9..." \
  -H "Content-Type: application/json" \
  -d '{"sex":1,"birth":"2000-01-01","introduction":"你好","location":"北京"}'
```

#### 更新歌曲（PATCH + 表单，不是 PUT/JSON）
```bash
curl -X PATCH http://localhost:9171/api/v1/songs/1 \
  -H "Authorization: eyJhbGciOiJIUzM4NCJ9..." \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "name=新歌名&introduction=简介"
```

#### 上传歌曲音频（multipart，文件字段名固定 file）
```bash
curl -X POST http://localhost:9171/api/v1/songs/1/audio \
  -H "Authorization: eyJhbGciOiJIUzM4NCJ9..." \
  -F "file=@/path/to/song.mp3"
```

#### 登出（真正吊销令牌，必须带 Authorization）
```bash
curl -X DELETE http://localhost:9171/api/v1/consumers/sessions \
  -H "Authorization: eyJhbGciOiJIUzM4NCJ9..."
```

### 查看日志

日志目录由 `logback-spring.xml` 的 `LOG_PATH=./logs` 决定（相对启动时的工作目录），
文件名前缀 `APP_NAME=music-backend`，单文件 100MB 滚动：

| 文件 | 内容 | 保留天数 | 总量上限 |
|------|------|---------|---------|
| `music-backend-info.log` | INFO 级（含 dev 档审计日志） | 30 | 10GB |
| `music-backend-warn.log` | WARN 级（含慢请求警告） | 60 | 15GB |
| `music-backend-error.log` | ERROR 级 | 90 | 20GB |
| `music-backend-all.log` | 全量 | 15 | 5GB |

```bash
# 实时查看请求日志（dev 档才有审计内容）
tail -f logs/music-backend-all.log

# 搜索特定请求
grep "请求ID" logs/music-backend-all.log

# 查看慢请求
grep "慢请求警告" logs/music-backend-warn.log

# 统计接口调用次数
grep "请求URL" logs/music-backend-info.log | sort | uniq -c | sort -rn
```

> 四个 appender 都经 `AsyncAppender` 异步输出，`discardingThreshold=0`（不丢 INFO）、
> `queueSize=512`。日志文件名与保留策略的唯一出处是 `logback-spring.xml`，
> 详见 [03-LOGGING.md](docs/03-LOGGING.md)。

---

## ⚡ 性能优化

### Redis 缓存

键前缀的唯一出处是 `common/CacheKeys.java`（此前分散在 7 个 ServiceImpl 与 RecommendationCache
各自的私有常量里，任一侧改名都不会报错，只会静默退化为"缓存永远命中不了"）。
实际在用的键与 TTL：

| 键 | TTL | 写入方 |
|----|-----|-------|
| `music:song:{id}` | 24 小时 | `SongServiceImpl` |
| `music:song:all` | 24 小时 | 同上 |
| `music:song:singer:{singerId}` | 24 小时 | 同上 |
| `music:singer:{id}` / `music:singer:all` | 24 小时 | `SingerServiceImpl` |
| `music:songlist:{id}` / `music:songlist:all` | 24 小时 | `SongListServiceImpl` |
| `music:list_song:{songListId}` | 24 小时 | `ListSongServiceImpl` |
| `music:consumer:{id}` / `music:consumer:username:{username}` | 24 小时 | `ConsumerServiceImpl` |
| `music:comment:song:{songId}` | **2 小时** | `CommentServiceImpl` |
| `music:comment:songlist:{songListId}` | **2 小时** | 同上 |
| `music:collect:user:{userId}` | **12 小时** | `CollectServiceImpl` |
| `music:collect:songlist:user:{userId}` | **12 小时** | 同上 |
| `music:recommendation:user:{userId}` | **360 + 随机 0~30 分钟**；空结果 **5 分钟** | `RecommendationCache` |
| `jwt:blacklist:{token}` | 与令牌剩余有效期对齐 | `TokenBlacklistService` |

> `songlist` 与 `list_song` 两种拼法是历史上就并存的不同域，如实保留；
> 改键前缀取值会让 Redis 中现有缓存整体失效，也会让写入方与未升级的读取方错键。
>
> 推荐缓存的 TTL 抖动是刻意的：避免同一批用户的缓存同时失效造成请求洪峰；
> 空结果也写入但用 5 分钟短 TTL 占位，既挡住反复空跑全链路，又不会让新行为长期不可见。
>
> 写操作普遍采用**延时双删**（`RedisUtils.deleteWithDelay`）而非单次删除。
> 命中率与"性能提升倍数"没有实测数据，本文不给数字。

📖 详细说明：[REDIS_CACHE_OPTIMIZATION.md](docs/REDIS_CACHE_OPTIMIZATION.md)

---

## 📖 相关文档

### 核心功能文档
- [01-AUTHENTICATION.md](docs/01-AUTHENTICATION.md) - JWT 认证与 Token 管理
- [02-RATE-LIMITING.md](docs/02-RATE-LIMITING.md) - API 限流配置
- [03-LOGGING.md](docs/03-LOGGING.md) - Logback 日志系统
- [04-REQUEST-AUDIT.md](docs/04-REQUEST-AUDIT.md) - 请求审计日志
- [05-PARAM-VALIDATION.md](docs/05-PARAM-VALIDATION.md) - 参数校验
- [06-CODE-COVERAGE.md](docs/06-CODE-COVERAGE.md) - 代码覆盖率

### 开发与运维
- [DEVELOPMENT.md](docs/DEVELOPMENT.md) - 本地开发、端口、构建链路与 CI 门禁
- [TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md) - 故障排查
- [DOCKER_QUICK_START.md](docs/DOCKER_QUICK_START.md) - Docker 快速开始
- [DOCKER_DEPLOYMENT.md](docs/DOCKER_DEPLOYMENT.md) - Docker 部署完整指南

### 技术文档
- [SPRINGDOC_OPENAPI_CONFIG.md](docs/SPRINGDOC_OPENAPI_CONFIG.md) - SpringDoc 配置
- [SECURITY_CONFIG_GUIDE.md](docs/SECURITY_CONFIG_GUIDE.md) - Spring Security 配置
- [BCRYPT_PASSWORD_ENCRYPTION.md](docs/BCRYPT_PASSWORD_ENCRYPTION.md) - BCrypt 密码加密
- [REDIS_CACHE_OPTIMIZATION.md](docs/REDIS_CACHE_OPTIMIZATION.md) - Redis 缓存优化

---

## 🎯 最佳实践

### 1. 认证处理
- 前端统一在 axios 请求拦截器中把 localStorage 里的令牌放进 `Authorization` 头（裸值，不加 `Bearer `）
- 令牌有效期 24 小时，**无自动续签**：v2.x 的 `/refresh` 端点已在 v3.0.0 删除
  （它只验旧签名不查黑名单，被吊销的令牌可借它复活）
- 登出要**先调 `DELETE .../sessions` 让后端吊销，再清本地存储**；
  吊销失败也照样清理并跳转，不能因后端不可达把用户困在已登录状态
- 不要在 URL 中传递 Token——该回落入口已关闭，查询串也会脱敏

### 2. 错误处理
- **判成功只看 `success`（或 `code == 20000`）**，不要只看 HTTP 状态码：
  业务失败与 `BusinessException` 都返回 HTTP 200
- 拦截器拒绝才是真 4xx/5xx：401 未登录/令牌失效、403 角色或 IP 不符、
  429 限流、**503 Redis 不可用导致限流 fail-closed**
- 401 清令牌跳登录页；403 提示无权访问（不要反复重试）；429/503 提示稍后再试
- 直接展示后端返回的中文 `message`，不要自行改写文案

### 3. 性能优化
- 列表一律走 `/page` 分页端点，不要用全量端点渲染长列表
- 写操作后后端会延时双删缓存，前端不必自行加时间戳绕缓存
- 个性化推荐 `GET /song-lists/recommended` 只接受 consumer 令牌，管理端调它会 403

### 4. 安全建议
- 生产环境使用 HTTPS
- prod 的 `JWT_SECRET` 无默认值，缺失即启动失败；密钥需 ≥ 448bit（HMAC-SHA384 下限）
- 历史泄露过的明文密钥必须**轮换**才能作废，仅改代码不够
- 代理透传头默认不采信，确需开启由 `security.ip.trust-proxy-headers` 显式控制
- 新增 Controller 方法必须标 `@RequireUserType`，否则 fail-closed 直接 403

---

**最后更新**: 2026-11-16（按 Controller / GlobalExceptionHandler / CacheKeys / logback 源码逐项核对）  
**API 版本**: /api/v1（RESTful）  
**文档维护**: 开发团队
