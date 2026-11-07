# JWT 认证与 Token 统一管理

## 📋 概述

项目已实现完整的 **JWT Token 认证系统**，包括：
- ✅ JWT Token 生成与验证（基于 jjwt 0.12.6）
- ✅ IP 地址绑定防止 Token 盗用
- ✅ Token 黑名单机制支持主动登出
- ✅ 前后端统一使用 JWT 认证（移除 Session）

---

## 🔐 核心功能

### 1. JWT Token 认证
- Token 包含：用户ID、用户名、用户类型、IP地址
- Token 有效期：24 小时（可配置）
- 签名算法：HMAC-SHA256

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
  "userId": 1,              // 用户ID
  "userType": "admin",      // 用户类型 (admin/consumer)
  "ip": "192.168.1.100",    // 绑定的IP地址
  "iat": 1715097600,        // 签发时间
  "exp": 1715184000         // 过期时间
}
```

---

## 📝 使用方法

### 1. 登录接口

#### 管理员登录
```bash
POST http://localhost:9171/admin/login/status
Content-Type: application/x-www-form-urlencoded

name=admin&password=123456
```

**返回结果：**
```json
{
  "code": 200,
  "msg": "登录成功",
  "data": {
    "name": "admin",
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 86400
  }
}
```

#### 前台用户登录
```bash
POST http://localhost:9171/consumer/login
Content-Type: application/x-www-form-urlencoded

username=testuser&password=123456
```

**返回结果：**
```json
{
  "code": 200,
  "msg": "登录成功",
  "data": {
    "userMsg": {
      "id": 1,
      "username": "testuser",
      "avator": "/userImages/xxx.jpg"
    },
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 86400
  }
}
```

### 2. 携带 Token 访问接口

#### 方式一：Header（推荐）
```bash
GET http://localhost:9171/song/allSong
Authorization: eyJhbGciOiJIUzI1NiJ9...
```

#### 方式二：URL 参数
```bash
> 令牌只能通过 `Authorization` 头传递，**不支持 `?token=` 查询参数**。
> 早期文档曾示范 URL 形式，实际后果是查询串会随 INFO 日志留档 30 天，
> 等于把完整 JWT 写进磁盘；该回落已连同日志脱敏一并关闭（见 TokenTransportTest）。

```bash
curl -H "Authorization: eyJhbGciOiJIUzI1NiJ9..." \
     http://localhost:9171/song/manage/allSong
```
```

---

## 🔄 工作流程

### 登录流程
```
1. 用户提交用户名和密码
2. 后端验证用户名密码（BCrypt加密）
3. 获取客户端真实IP地址
4. 生成JWT Token（包含userId, username, userType, ip）
5. 返回Token给前端
6. 前端保存Token到localStorage
```

### 认证流程
```
1. 前端在请求Header中携带Token
2. JwtAuthenticationInterceptor拦截请求
3. 验证Token格式是否有效
4. 验证Token是否过期
5. 验证IP地址是否匹配
6. 检查Token是否在黑名单中
7. 验证通过，将用户信息放入request属性
8. 放行请求到Controller
```

### 登出流程
```javascript
// 前端登出
localStorage.removeItem('adminToken');  // 或 'userToken'
this.$router.push("/");

// 可选：后端将Token加入Redis黑名单
// POST /logout with Authorization header
```

---

## ⚙️ 配置说明

### application.yml
```yaml
# JWT配置
jwt:
  # JWT密钥（生产环境请使用环境变量）
  secret: MusicWebsiteSecretKey2026ForJWTTokenGenerationAndValidation
  # Token过期时间（毫秒），默认24小时
  expiration: 86400000
```

### 生产环境建议
```yaml
jwt:
  secret: ${JWT_SECRET:随机生成的长字符串}
  expiration: ${JWT_EXPIRATION:86400000}
```

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

**TheHeader.vue - 登出清除Token**
```javascript
handleCommand(command){
    if(command == "logout"){
        localStorage.removeItem('adminToken');
        localStorage.removeItem('userName');
        this.$router.push("/");
    }
}
```

### 前台用户端（MusicUser-frontend）

类似配置，使用 `userToken` 作为key。

---

## 🚨 错误码说明

| 错误码 | 说明 | 场景 |
|-------|------|------|
| 401 | Token无效或已过期 | Token格式错误或超过有效期 |
| 403 | IP地址不匹配 | Token中的IP与当前请求IP不一致 |
| 429 | 访问过于频繁 | 超过限流限制（配合限流功能） |

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
- ✅ 生产环境务必修改 JWT secret 为复杂字符串
- ✅ 建议将 secret 放在环境变量或配置中心
- ✅ HTTPS 环境下传输 Token

### 2. IP 绑定
- ⚠️ 移动网络IP可能频繁变化，导致需要重新登录
- ⚠️ 代理服务器可能导致IP提取不准确
- ✅ 可根据业务需求决定是否启用IP绑定

### 3. Token 刷新
- 当前实现未自动刷新Token
- Token 过期后需重新登录
- 可扩展：添加 Refresh Token 机制

---

## 🚀 扩展功能

### 1. 刷新 Token
```java
@GetMapping("/refresh")
public R<String> refreshToken(HttpServletRequest request) {
    String token = request.getHeader("Authorization");
    if (!StringUtils.hasText(token)) {
        return R.error("Token不能为空");
    }
    
    String newToken = jwtUtils.refreshToken(token);
    if (newToken == null) {
        return R.error("Token无效");
    }
    
    return R.ok("刷新成功", newToken);
}
```

### 2. 获取当前用户信息
```java
@GetMapping("/current")
public R getCurrentUser(HttpServletRequest request) {
    Integer userId = (Integer) request.getAttribute("userId");
    String userType = (String) request.getAttribute("userType");
    
    Map<String, Object> userInfo = new HashMap<>();
    userInfo.put("userId", userId);
    userInfo.put("userType", userType);
    
    return R.ok(userInfo);
}
```

### 3. 主动登出（Token黑名单）
```java
@PostMapping("/logout")
public R logout(HttpServletRequest request) {
    String token = request.getHeader("Authorization");
    if (StringUtils.hasText(token)) {
        // 将Token加入黑名单，过期时间设置为Token剩余有效期
        String redisKey = "jwt:blacklist:" + token;
        Long expire = jwtUtils.getExpireFromToken(token);
        redisUtils.setCacheObject(redisKey, true, expire, TimeUnit.SECONDS);
    }
    return R.ok("登出成功");
}
```

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
✅ JWT Token 生成与验证  
✅ IP 地址提取与绑定  
✅ Token 黑名单机制  
✅ 前后端统一使用 JWT（移除 Session）  
✅ 登录接口改造（管理员+前台用户）  
✅ 拦截器自动注册  

### 安全保障
🔒 防止 Token 伪造 - HMAC-SHA256 签名  
🔒 防止 Token 盗用 - IP 地址绑定  
🔒 支持主动登出 - Token 黑名单  
🔒 无状态认证 - 支持分布式部署  

---

## 📖 参考文档

- [jjwt 官方文档](https://github.com/jwtk/jjwt)
- [Spring Interceptors](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-config/interceptors.html)
- [JWT 规范](https://jwt.io/introduction)
