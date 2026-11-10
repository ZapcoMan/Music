# JWT 认证与 Token 统一管理

> ⚠️ **口径声明**：本文成文于 RESTful 改造之前，其中的接口路径（如 `/admin/login/status`、
> `/song/allSong`）、响应示例（`code: 200` / `msg` 字段）与“密钥可带默认值”的配置写法均已过期。
> 现状：全部业务接口在 `/api/v1` 下；统一返回体 `R<T>`（`success`/`code` 20000/20001/20002/`message`/`data`，无 `dataMap`）；
> prod 的 `JWT_SECRET` 无默认值，缺失即启动失败。关键口径见本文标注“现状”的小节与
> [FIX-LOG-2026-11.md](FIX-LOG-2026-11.md)、[CHANGELOG.md](../CHANGELOG.md)。

## 📋 概述

项目已实现完整的 **JWT Token 认证系统**，包括：
- ✅ JWT Token 生成与验证（基于 jjwt 0.12.6，含签发方 `iss` 与受众 `aud` 校验）
- ✅ IP 地址绑定防止 Token 盗用（透传头默认不采信）
- ✅ Token 黑名单机制支持主动登出（登出真正吊销令牌，TTL 与令牌剩余有效期对齐）
- ✅ 角色默认拒绝（`@RequireUserType`，未声明的端点一律 403）
- ✅ 前后端统一使用 JWT 认证（移除 Session）

---

## 🔐 核心功能

### 1. JWT Token 认证
- Token 包含：用户ID、用户名、用户类型、IP 地址，另含签发方/受众/签发与过期时间
- Token 有效期：24 小时（`jwt.expiration` 可配置；出参 `expiresIn` 由 `getExpirationSeconds()` 派生，单一来源）
- 签名算法：由密钥长度决定（jjwt 自动选择；本项目密钥强度 ≥ 448bit，实际 HMAC-SHA384）
- 校验链：签名 + 有效期 + `requireIssuer("music-backend")` + `requireAudience("music-clients")`，
  防同一密钥在他服务签发的令牌串用进来

### 2. IP 地址绑定
- 登录时记录客户端真实 IP
- 每次请求验证 IP 是否匹配
- 代理透传头（X-Forwarded-For、X-Real-IP 等）提取**默认关闭**，由
  `security.ip.trust-proxy-headers` 控制：这类头客户端可随意伪造，无条件采信会让
  IP 绑定校验被一个请求头绕过、审计与限流身份失真。只有确认客户端无法直连后端时才开启。

### 3. 统一认证架构
- **管理员**：使用 JWT Token（已移除 Session）
- **前台用户**：使用 JWT Token
- 优势：无状态、支持分布式部署、跨域友好

---

## 🛠️ 技术实现

### 核心文件

| 文件 | 路径 | 说明 |
|------|------|------|
| `JwtUtils.java` | `utils/` | JWT 生成与验证工具类 |
| `IpUtils.java` | `utils/` | IP 地址提取工具类 |
| `JwtAuthenticationInterceptor.java` | `interceptor/` | JWT 认证拦截器 |
| `WebMvcConfig.java` | `config/` | 拦截器注册配置 |

### Token 结构

```json
{
  "sub": "admin",           // 用户名
  "iss": "music-backend",   // 签发方（固定常量，防同密钥他服务令牌串用）
  "aud": "music-clients",   // 受众（同上）
  "userId": 1,              // 用户ID
  "userType": "admin",      // 用户类型 (admin/consumer)
  "ip": "192.168.1.100",    // 绑定的IP地址
  "iat": 1715097600,        // 签发时间
  "exp": 1715184000         // 过期时间
}
```

---

## 📝 使用方法

### 1. 登录接口（现状：会话资源式路径）

#### 管理员登录
```bash
POST http://localhost:9171/api/v1/admin/sessions
Content-Type: application/x-www-form-urlencoded

name=admin&password=***
```

**返回结果（统一 `R<T>` 形状，`data` 为 `AdminLoginVo`）：**
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

**返回结果（`data` 为 `ConsumerLoginVo`）：**
```json
{
  "success": true,
  "code": 20000,
  "message": "登录成功",
  "data": {
    "userMsg": {
      "id": 1,
      "username": "testuser",
      "avator": "/userImages/xxx.jpg"
    },
    "token": "eyJhbGciOiJIUzM4NCJ9...",
    "expiresIn": 86400
  }
}
```

> **Content-Type 口径说明**：两个登录接口在 Controller 侧仍是 `@RequestParam` 绑定，
> 需按表单（或查询参数）提交；v3.0.0 改为 JSON（`@RequestBody`）提交的是歌手/歌曲/
> 歌单/用户等 **CRUD 写接口**，给登录接口发 JSON 体反而会因参数绑定不上而 400。
> 两端登录页（`Login.vue` / `LoginIn.vue`）均以 `URLSearchParams` 发表单。
> 出参已收敛为 `AdminLoginVo`/`ConsumerLoginVo`，不再返回裸 HashMap；
> `expiresIn` 由 `jwtUtils.getExpirationSeconds()` 派生，与 `jwt.expiration` 单一来源。

### 2. 携带 Token 访问接口

令牌**只能通过 `Authorization` 头**传递，不支持 `?token=` 查询参数：
早期文档曾示范 URL 形式，实际后果是查询串会随 INFO 日志留档 30 天，
等于把完整 JWT 写进磁盘；该回落已连同日志脱敏一并关闭（见 `TokenTransportTest`）。

```bash
curl -H "Authorization: eyJhbGciOiJIUzM4NCJ9..." \
     http://localhost:9171/api/v1/songs
```

---

## 🔄 工作流程

### 登录流程
```
1. 前端以表单提交用户名和密码（URLSearchParams）
2. 后端验证用户名密码（BCrypt，强度因子 12，校验始终问数据库）
3. IpUtils 获取客户端真实IP地址（透传头默认不采信）
4. 生成JWT Token（claims: userId/username/userType/ip + iss/aud/iat/exp）
5. 返回 R<AdminLoginVo/ConsumerLoginVo>（token + expiresIn）
6. 前端保存Token到localStorage（adminToken / userToken）
```

### 认证流程（JwtAuthenticationInterceptor.preHandle，按实际顺序）
```
1. 命中 ANONYMOUS_ENTRIES（方法+路径成对）的公开入口直接放行
2. 只从 Authorization 头取Token，缺失一律 401（不存在"无Token放行"）
3. validateToken：签名 + 有效期 + iss=music-backend + aud=music-clients
4. IP绑定验证：Token内IP与当前请求IP不一致 → 403
5. 黑名单检查 jwt:blacklist:{token}：命中 → 401；Redis异常 → fail-open放行（error日志，便于告警）
6. 角色域校验：有 @RequireUserType 且令牌userType不命中 → 403；
   未声明 @RequireUserType 的Controller方法 → fail-closed 一律 403（默认拒绝）
7. 通过后将 userId/userType/username/token 写入request属性，放行到Controller
```

### 登出流程（现状：真正吊销令牌）
```javascript
// 前端：先调后端登出接口，成功后再清本地存储
DELETE /api/v1/admin/sessions        // 管理端 adminLogout()
DELETE /api/v1/consumers/sessions    // 用户端 consumerLogout()
// 请求必须携带 Authorization 头（登出不是匿名入口）
localStorage.removeItem('adminToken');  // 或 'userToken'
this.$router.push("/");
```
后端由 `TokenBlacklistService.revoke(token)` 把令牌写入 `jwt:blacklist:{token}`，
TTL 用 `jwtUtils.getRemainingSeconds(token)` 与令牌剩余有效期对齐，避免黑名单永驻；
Redis 不可用时登出接口仍返回成功、但会如实提示"服务端暂时无法吊销该令牌"。

---

## ⚙️ 配置说明（现状）

### application-dev.yml（本地开发）
```yaml
jwt:
  # 本地开发专用占位串，刻意写成一眼是占位的形态，不再是"看起来像真实密钥"的明文
  # 长度 ≥ 448bit 是为满足 HMAC-SHA384 对密钥强度的下限
  secret: ${JWT_SECRET:dev-only-placeholder-not-a-real-key-change-me-now-please}
  expiration: 86400000  # 24小时
```

### application-prod.yml（生产）
```yaml
jwt:
  # 不提供默认值：漏配 JWT_SECRET 直接启动失败，而不是静默带着占位密钥上线
  secret: ${JWT_SECRET}
  expiration: ${JWT_EXPIRATION:86400000}
```

> 历史背景：早期版本的 dev 配置里写死过真实密钥形态的明文，且已进入公开仓库与
> git 历史——对任何能读源码的人那串密钥永久有效，可用它自签任意角色（含 admin）
> 的合法令牌。因此真正的止血动作是**轮换**：prod 侧更换 `JWT_SECRET` 环境变量即可
> 让旧串作废；`docker-compose` 已用 `${JWT_SECRET:?...}` 把缺失变成部署期硬失败。

---

## 💡 前端集成

### 后台管理端（MusicAdmin-frontend）

**Login.vue - 保存Token**
```javascript
if (res.data && res.data.token) {
    localStorage.setItem('adminToken', res.data.token);
}
```

**http.js - 自动携带Token**
```javascript
axios.interceptors.request.use(config => {
    const token = localStorage.getItem('adminToken');
    if (token) {
        config.headers.Authorization = token;
    }
    return config;
});
```

**TheHeader.vue - 登出：先吊销再清本地（现状）**
```javascript
if(command == "logout"){
    // 先让后端把当前令牌放进黑名单，再清本地。
    // 吊销失败也照样清理并跳转，不能因后端不可达把用户困在已登录状态
    const cleanup = () => {
        localStorage.removeItem('adminToken');
        localStorage.removeItem('userName');
        this.$router.push("/");
    };
    adminLogout()            // DELETE /api/v1/admin/sessions
        .then(cleanup)
        .catch(cleanup);
}
```

### 前台用户端（MusicUser-frontend）

类似配置，使用 `userToken` 作为 localStorage key；登出调 `consumerLogout()`
（`DELETE /api/v1/consumers/sessions`）后再清本地。

---

## 🚨 错误码说明

| 错误码 | 说明 | 场景 |
|-------|------|------|
| 401 | Token无效或已过期 | 未携带Token、格式/签名错误、超过有效期、iss/aud 不匹配、命中黑名单 |
| 403 | IP地址不匹配 | Token中的IP与当前请求IP不一致 |
| 403 | 角色不匹配 | 令牌 userType 不在接口 `@RequireUserType` 声明范围内 |
| 403 | 未声明角色域，默认拒绝 | 接口漏标 `@RequireUserType`，fail-closed 直接 403 并记 error 日志提醒补注解 |
| 429 | 访问过于频繁 | 超过限流限制（配合限流功能，见 02-RATE-LIMITING.md） |

拒绝响应体统一为 `R.error(status, message)` 形状（HTTP 状态码原样返回，
区别于业务失败的 HTTP 200 + `success:false`），文案为中文，前端拦截器直接展示。

---

## 🔍 监控与调试

### 查看日志
```bash
# 查看登录日志
grep "登录" logs/music-backend-all.log

# 查看JWT认证日志
grep "JWT认证" logs/music-backend-all.log

# 查看特定请求
grep "请求ID" logs/music-backend-all.log
```

### Redis 监控
```bash
# 查看Token黑名单
redis-cli keys "jwt:blacklist:*"

# 清空黑名单
redis-cli keys "jwt:blacklist:*" | xargs redis-cli del
```

---

## ⚠️ 注意事项

### 1. 安全性
- ✅ prod 的 `JWT_SECRET` 为**硬性必填**（无默认值，缺失即启动失败），不再是"建议修改"
- ✅ 密钥长度需 ≥ 448bit（HMAC-SHA384 的下限，过短会被 jjwt 直接拒绝）
- ✅ 历史泄露过的明文密钥必须轮换才能作废，仅改代码不够
- ✅ HTTPS 环境下传输 Token

### 2. IP 绑定
- ⚠️ 移动网络IP可能频繁变化，导致需要重新登录
- ⚠️ 代理透传头默认不采信（可伪造）；确实需要时由 `security.ip.trust-proxy-headers` 显式开启

### 3. Token 刷新
- 当前实现无自动刷新，Token 过期后需重新登录
- v2.x 曾存在的续签端点已在 v3.0.0 **删除**：它只验旧签名不查黑名单，
  被吊销的令牌可以借此复活，与"登出真正吊销"的口径直接冲突
- 若将来引入 Refresh Token，必须与新令牌签发同事务吊销旧刷新令牌，并过黑名单

---

## 🚀 扩展功能

### 1. 刷新 Token（已删除，不要再加回来）
早期版本的 `GET /refresh` 续签端点已于 v3.0.0 移除：它只验旧令牌签名、
不查黑名单，被吊销（登出）的令牌可借它复活，破坏"登出即失效"的安全口径。
如确有续签需求，参照上方"注意事项 3"的安全约束重新设计。

### 2. 获取当前用户信息
```java
@GetMapping("/current")
public R<Map<String, Object>> getCurrentUser(HttpServletRequest request) {
    Integer userId = (Integer) request.getAttribute("userId");
    String userType = (String) request.getAttribute("userType");

    Map<String, Object> userInfo = new HashMap<>();
    userInfo.put("userId", userId);
    userInfo.put("userType", userType);

    return R.ok(userInfo);
}
```
> 仅作扩展示意：新增此类端点时必须标注 `@RequireUserType("admin")` 或
> `@RequireUserType("consumer")`，否则会被 fail-closed 守卫直接 403。

### 3. 主动登出（已实现，非扩展项）
现状代码见 `AdminController.logoutAdmin` / `ConsumerController.logout`：
`DELETE /api/v1/{admin|consumers}/sessions` → `TokenBlacklistService.revoke(token)`
写入 `CacheKeys.TOKEN_BLACKLIST + token`，TTL 用 `jwtUtils.getRemainingSeconds(token)`
与令牌剩余有效期对齐；Redis 异常不抛出（避免登出变 500），但如实告知调用方未吊销。

---

## 📊 性能影响

| 操作 | 耗时 | 影响 |
|------|------|------|
| JWT 验证 | ~1-2ms | 几乎无感知 |
| IP 提取 | <1ms | 忽略不计 |
| 黑名单检查 | ~2-5ms | 轻微，可接受 |

**总体性能损耗**: <10ms，对用户体验无明显影响

---

## 🎯 总结

### 已完成的功能
✅ JWT Token 生成与验证（含 iss/aud 校验）  
✅ IP 地址提取与绑定  
✅ Token 黑名单机制（登出真正吊销，TTL 与剩余有效期对齐）  
✅ 角色域守卫（`@RequireUserType`，未声明默认拒绝）  
✅ 前后端统一使用 JWT（移除 Session）  
✅ 登录/登出接口 RESTful 化（`/api/v1/*/sessions`）  
✅ 拦截器自动注册（Logging → JWT → RateLimit 固定顺序）  

### 安全保障
🔒 防止 Token 伪造 - HMAC 签名（密钥 ≥ 448bit，实际为 HMAC-SHA384）  
🔒 防止他服务令牌串用 - iss/aud 强校验  
🔒 防止 Token 盗用 - IP 地址绑定  
🔒 防止跨角色越权 - 角色默认拒绝（fail-closed）  
🔒 支持主动登出 - Token 黑名单  
🔒 无状态认证 - 支持分布式部署  

---

## 📖 参考文档

- [jjwt 官方文档](https://github.com/jwtk/jjwt)
- [Spring Interceptors](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-config/interceptors.html)
- [JWT 规范](https://jwt.io/introduction)
