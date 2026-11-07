# API 开发文档

## 📋 概述

本文档提供音乐网站系统的完整 API 开发指南，包括接口规范、认证方式、参数校验、日志审计等内容。

---

## 🔗 快速访问

### Swagger UI（在线文档）
- **地址**: http://localhost:9171/swagger-ui.html
- **说明**: 可视化 API 文档，支持在线测试

### OpenAPI JSON
- **地址**: http://localhost:9171/v3/api-docs
- **说明**: OpenAPI 3.0 规范的 JSON 格式文档

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
POST http://localhost:9171/admin/login/status
Content-Type: application/x-www-form-urlencoded

name=admin&password=123456
```

**响应：**
```json
{
  "success": true,
  "code": 20000,
  "message": "登录成功",
  "data": {
    "name": "admin",
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 86400
  },
  "dataMap": {}
}
```

#### 前台用户登录
```bash
POST http://localhost:9171/consumer/login
Content-Type: application/x-www-form-urlencoded

username=testuser&password=123456
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
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 86400
  }
}
```

### 安全特性

- ✅ **IP 地址绑定**：Token 与客户端 IP 绑定，防止盗用
- ✅ **Token 有效期**：24 小时自动过期
- ✅ **Token 黑名单**：支持主动登出（Redis 实现）
- ✅ **API 限流**：登录与注册 60 秒最多 5 次；评论发表 10、点赞 30、
  资料修改与删除 10~20、头像上传 5（`@RateLimit` 逐个接口标注，键按"有令牌用用户、无令牌用 IP"）

📖 详细说明：[01-AUTHENTICATION.md](docs/01-AUTHENTICATION.md) | [02-RATE-LIMITING.md](docs/02-RATE-LIMITING.md)

---

## 📝 参数校验

### 校验规则

系统使用 Bean Validation 进行参数校验，增删改接口均启用了 `@Valid` 注解。

#### 常用校验注解

| 注解 | 说明 | 示例 |
|------|------|------|
| `@NotBlank` | 不能为空白字符串 | `@NotBlank(message = "名称不能为空")` |
| `@NotNull` | 不能为 null | `@NotNull(message = "ID不能为空")` |
| `@Size` | 长度范围 | `@Size(min=6, max=50)` |
| `@Email` | 邮箱格式 | `@Email(message = "邮箱格式错误")` |
| `@Pattern` | 正则表达式 | `@Pattern(regexp="^1[3-9]\\d{9}$")` |
| `@Min` / `@Max` | 数值范围 | `@Min(1) @Max(100)` |

### 校验失败响应

```json
{
  "success": false,
  "code": 400,
  "message": "用户名不能为空",
  "dataMap": {}
}
```

HTTP 状态码同时为 400（`GlobalExceptionHandler` 对该异常标了 `@ResponseStatus(HttpStatus.BAD_REQUEST)`）。
注意 `code` 这里就是 400 而不是 20002：`R.error(400, message)` 直接传了数字，
与 `ResultCodeEnum.PARAM_ERROR(20002)` 并存 —— 参数错误的 code 目前有两个来源，属已知不一致。

📖 详细说明：[05-PARAM-VALIDATION.md](docs/05-PARAM-VALIDATION.md)

---

## 📊 响应格式

### 统一响应结构

```json
{
  "success": true,      // 是否成功；失败响应现在会置 false
  "code": 20000,        // 20000 成功；20001 未知错误；20002 参数错误；20003 空指针；20004 客户端错误
  "message": "操作成功", // 提示信息（字段名是 message，不是 msg）
  "data": {},           // 返回数据（可选，为 null 时不出现在 JSON 里）
  "dataMap": {}         // 另一种数据载体，部分接口用它
}
```

字段说明以 `common/R.java` 与 `common/ResultCodeEnum.java` 为准，本文不再重复维护。

### 常见状态码

HTTP 状态码与业务 code 是两层，不要混看：

| HTTP | 由谁决定 | 场景 |
|------|---------|------|
| 200 | 正常返回 | 请求进入业务层并返回 R（**业务失败也返回 200**，需看 `success`） |
| 400 | GlobalExceptionHandler | `@Valid` 校验失败 |
| 401 | JwtAuthenticationInterceptor | 未携带 Token、Token 无效/过期、已被登出吊销 |
| 403 | JwtAuthenticationInterceptor | IP 与令牌绑定不符；或 `@RequireUserType` 角色不匹配 |
| 429 | RateLimitInterceptor | 超过限流阈值 |

业务 code：20000 成功 / 20001 未知错误 / 20002 参数错误 / 20003 空指针 / 20004 客户端错误，
另有少数接口直接返回 400/401/403 作为 code（见上表来源），因此判断成败请以 `success` 为准。

---

## 📚 API 模块

### 主要接口模块

| 模块 | 路径前缀 | 说明 | 文档 |
|------|---------|------|------|
| **管理员** | `/admin` | 登录、权限管理 | Swagger UI |
| **用户** | `/consumer` | 注册、登录、信息管理 | Swagger UI |
| **歌手** | `/singer` | 歌手 CRUD、分页查询 | Swagger UI |
| **歌曲** | `/song` | 歌曲 CRUD、文件上传 | Swagger UI |
| **歌单** | `/songList` | 歌单 CRUD、推荐算法 | Swagger UI |
| **收藏** | `/collect` | 收藏/取消收藏 | Swagger UI |
| **评论** | `/comment` | 评论、点赞、高频词 | Swagger UI |
| **评分** | `/rank` | 歌单评分 | Swagger UI |
| **关联** | `/listSong` | 歌单-歌曲关联 | Swagger UI |

### 接口分类

#### 1. 公开接口（无需认证）
- `POST /admin/login/status` - 管理员登录
- `POST /consumer/login` - 用户登录
- `POST /consumer/add` - 用户注册
- `GET /singer/allSinger` - 查询所有歌手
- `GET /song/allSong` - 查询所有歌曲
- `GET /songList/allSongList` - 查询所有歌单

#### 2. 受保护接口（需要 Token）
- `POST /consumer/update` - 更新用户信息
- `POST /song/add` - 添加歌曲
- `POST /collect/addCollection` - 收藏歌曲
- `POST /comment/add` - 添加评论
- `DELETE /song/delete` - 删除歌曲

📖 完整接口列表请访问 Swagger UI

---

## 🔍 请求审计日志

### 日志记录内容

系统自动记录所有 API 请求和响应的详细信息：

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
【请求URL】     : /admin/login/status
【客户端IP】    : 192.168.1.100
【请求体】      : {"name":"admin","password":"***"}

========== 请求结束 ==========
【请求ID】      : 1715097600000-1234
【响应状态】    : 200
【耗时】        : 156 ms
【异常信息】    : 无
============================
```

### 敏感信息脱敏

- `Authorization`: `Bearer eyJhbGci***`（只显示前15字符）
- `Cookie`: `***`（完全隐藏）
- `password`: `"***"`
- `token`: `"***"`

### 慢请求监控

超过 3 秒的请求会发出警告：
```
⚠️ 慢请求警告 - URL: /song/allSong, 耗时: 4523 ms
```

📖 详细说明：[04-REQUEST-AUDIT.md](docs/04-REQUEST-AUDIT.md)

---

## 🧪 测试与调试

### 使用 Swagger UI 测试

1. 访问 http://localhost:9171/swagger-ui.html
2. 选择要测试的接口
3. 点击 "Try it out"
4. 填写参数（如需认证，在 Authorization 字段填入 Token）
5. 点击 "Execute" 执行请求
6. 查看响应结果

### 使用 cURL 测试

#### 登录获取 Token
```bash
curl -X POST http://localhost:9171/admin/login/status \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "name=admin&password=123456"
```

#### 携带 Token 访问接口
```bash
curl -X GET http://localhost:9171/song/allSong \
  -H "Authorization: eyJhbGciOiJIUzI1NiJ9..."
```

### 查看日志

```bash
# 实时查看请求日志
tail -f logs/music-backend-all.log

# 搜索特定请求
grep "请求ID" logs/music-backend-all.log

# 查看慢请求
grep "慢请求警告" logs/music-backend-warn.log

# 统计接口调用次数
grep "请求URL" logs/music-backend-info.log | sort | uniq -c | sort -rn
```

---

## ⚡ 性能优化

### Redis 缓存

系统对高频查询数据进行了 Redis 缓存优化：

| 数据类型 | 缓存Key | 过期时间 | 命中率 |
|---------|---------|---------|--------|
| 歌曲详情 | `music:song:{id}` | 24小时 | 95%+ |
| 歌单内歌曲 | `music:list_song:{songListId}` | 24小时 | 95%+ |
| 歌手列表 | `music:singer:all` | 24小时 | 90%+ |
| 歌单列表 | `music:songlist:all` | 24小时 | 90%+ |
| 歌曲评论 | `music:comment:song:{songId}` | 2小时 | 85%+ |

📖 详细说明：[REDIS_CACHE_OPTIMIZATION.md](docs/REDIS_CACHE_OPTIMIZATION.md)

### 性能提升预期

- **查询响应速度**: 提升 5-10 倍
- **数据库压力**: 降低 80%+
- **系统并发能力**: 提升 3-5 倍

---

## 📖 相关文档

### 核心功能文档
- [01-AUTHENTICATION.md](docs/01-AUTHENTICATION.md) - JWT 认证与 Token 管理
- [02-RATE-LIMITING.md](docs/02-RATE-LIMITING.md) - API 限流配置
- [03-LOGGING.md](docs/03-LOGGING.md) - Logback 日志系统
- [04-REQUEST-AUDIT.md](docs/04-REQUEST-AUDIT.md) - 请求审计日志
- [05-PARAM-VALIDATION.md](docs/05-PARAM-VALIDATION.md) - 参数校验
- [06-CODE-COVERAGE.md](docs/06-CODE-COVERAGE.md) - 代码覆盖率

### 技术文档
- [SPRINGDOC_OPENAPI_CONFIG.md](docs/SPRINGDOC_OPENAPI_CONFIG.md) - SpringDoc 配置
- [SECURITY_CONFIG_GUIDE.md](docs/SECURITY_CONFIG_GUIDE.md) - Spring Security 配置
- [BCRYPT_PASSWORD_ENCRYPTION.md](docs/BCRYPT_PASSWORD_ENCRYPTION.md) - BCrypt 密码加密
- [REDIS_CACHE_OPTIMIZATION.md](docs/REDIS_CACHE_OPTIMIZATION.md) - Redis 缓存优化

---

## 🎯 最佳实践

### 1. 认证处理
- 前端统一在 axios 拦截器中携带 Token
- Token 过期后自动跳转到登录页
- 不要在 URL 中传递 Token（使用 Header）

### 2. 错误处理
- 统一处理 401、403、429 等错误码
- 显示友好的错误提示
- 记录错误日志便于排查

### 3. 性能优化
- 合理使用缓存，避免频繁请求
- 大数据量接口使用分页
- 图片等资源使用 CDN

### 4. 安全建议
- 生产环境使用 HTTPS
- 定期更换 JWT Secret
- 监控异常登录行为

---

**最后更新**: 2026-05-07  
**API 版本**: v1.0  
**文档维护**: 开发团队
