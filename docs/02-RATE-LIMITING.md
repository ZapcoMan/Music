# API 限流配置与使用

> ⚠️ **口径声明**：本文早期版本描述的是"IP + URI 维度、滑动窗口"的旧实现，已与代码不符。
> 现状为：固定窗口 + Redis 原子 INCR、限流键按身份（登录用户优先，退回 IP）且不拼 URI、
> Redis 故障按 `failOpen` 降级。关键口径见 [FIX-LOG-2026-11.md](FIX-LOG-2026-11.md)、
> [CHANGELOG.md](../CHANGELOG.md)，实现见 `RateLimitInterceptor` / `RateLimit`。

## 📋 概述

项目已实现基于 Redis 的 **API 限流系统**，包括：
- ✅ 注解式限流配置（`@RateLimit`，当前全项目 10 处使用）
- ✅ 固定窗口计数，Redis `INCR` 原子增减（并发下不会同时读到空值各自重置计数）
- ✅ 按身份维度限流：已登录用 `user:{userId}`，匿名退回 `ip:{ip}`
  ——只按 IP 计数时，同一 NAT 出口（校园网/公司网）下全楼用户共用一个计数器
- ✅ 全局开关控制（`rate-limit.enabled`，默认 true，dev/test/prod 各自配置）
- ✅ Redis 不可用时的显式降级策略（`failOpen`，默认拒绝并返回 503）

---

## 🔧 核心功能

### 1. 限流实现
- 限流键 = `rate:` 统一命名空间 + 注解 `key` 前缀 + 身份维度，**不拼 URI**：
  注解前缀本身已按接口划分；拼 URI 反而让大小写、尾部斜杠、百分号编码等
  URL 变体各自建键，攻击者由此绕过限流（URI 仍保留在日志里供排查）。
  统一 `rate:` 前缀让测试基类能用一次 `rate:*` 清空全部计数器，
  避免限流计数跨用例累计造成"假绿/假红"。
- 判定与计数原子：`INCR` 返回值即当前次数；仅窗口内首次请求（count==1）设置过期，
  保证计数与时间窗口同生命周期
- 超限返回 HTTP **429**，响应体为 `R.error(429, message)`

### 2. Redis 故障降级（failOpen）
- 注解默认 `failOpen = false`：Redis 不可达时拒绝请求并返回 **503**
  （"服务暂时不可用"），而不是冒成 500，也不是伪装成 429 的"你请求太快"
- 挂在登录/注册/改资料/删账号上的限流是爆破与滥用的唯一闸门，缓存层故障期间
  放开它等于在最需要防护的时候解除防护；纯读取类接口若想维持可用，显式写 `failOpen = true`
- 早期注释声称的"不阻断业务"从未成立：连接失败时 spring-data-redis 抛
  `DataAccessException`，实际行为是异常冒到全局处理变 500

### 3. 全局开关
- `rate-limit.enabled`：dev=true，test=false（避免单测打满窗口），prod=true
- 只拦截标注了 `@RateLimit` 的方法/类；开关关闭时近乎零开销

### 4. 注解式配置
```java
@RateLimit(key = "login:admin:", timeWindow = 60, maxRequests = 5,
           message = "登录尝试次数过多，请60秒后再试")
```

---

## 🛠️ 技术实现

### 核心文件

| 文件 | 路径 | 说明 |
|------|------|------|
| `RateLimit.java` | `annotation/` | 限流注解定义 |
| `RateLimitInterceptor.java` | `interceptor/` | 限流拦截器实现 |
| `WebMvcConfig.java` | `config/` | 拦截器注册配置 |

### 限流注解参数（与 `RateLimit.java` 一致）

```java
@RateLimit(
    key = "rate_limit:",      // 限流key前缀（默认值；实际使用时都应显式按业务命名，如 login:admin:）
    timeWindow = 60,          // 时间窗口（秒）
    maxRequests = 100,        // 最大请求次数
    message = "访问过于频繁,请稍后再试",  // 超限提示信息
    failOpen = false          // Redis 不可用时是否放行，默认拒绝（503）
)
```

---

## 📝 使用方法

### 1. 为接口添加限流（以下两段为通用示意，非现行代码）

#### Controller 方法级别
```java
@RestController
@RequestMapping("/api/v1/examples")
public class ExampleController {

    /**
     * 限制: 60秒内最多访问100次
     */
    @GetMapping("/data")
    @RateLimit(timeWindow = 60, maxRequests = 100)
    public R<DataVo> getData() {
        return R.ok(data);
    }

    /**
     * 限制: 30秒内最多访问10次
     */
    @PostMapping("/submit")
    @RateLimit(key = "custom_key:", timeWindow = 30, maxRequests = 10,
               message = "提交过于频繁")
    public R<Void> submitData() {
        return R.ok();
    }
}
```

#### Controller 类级别（所有方法生效，当前项目中暂无类级别使用）
```java
@RestController
@RequestMapping("/api/v1/example-admin")
@RateLimit(timeWindow = 60, maxRequests = 50)
public class ExampleAdminController {
    // 所有接口都受限于 60秒/50次
}
```

### 2. 实际已覆盖的 10 个端点（现状清单，均为 60 秒窗口）

| 限流 key | 端点 | 次数上限 | 说明 |
|---------|------|---------|------|
| `login:admin:` | `POST /api/v1/admin/sessions` | 5 | 管理员登录，防爆破 |
| `login:consumer:` | `POST /api/v1/consumers/sessions` | 5 | 用户登录，防爆破 |
| `register:consumer:` | `POST /api/v1/consumers/registrations` | 5 | 注册防批量滥用 |
| `update:consumer:` | `PATCH /api/v1/consumers/profile` | 10 | 改自己资料 |
| `update:consumer:admin:` | `PATCH /api/v1/consumers/{id}` | 10 | 管理员改用户资料，防滥改 |
| `delete:consumer:` | `DELETE /api/v1/consumers/{id}` | 10 | 删用户（高危） |
| `upload:avatar:` | `POST /api/v1/consumers/{id}/avatar` | 5 | 头像上传防磁盘滥用 |
| `comment:add:` | `POST /api/v1/comments` | 10 | 防刷屏 |
| `comment:delete:` | `DELETE /api/v1/comments/{id}` | 20 | 防批量删 |
| `comment:like:` | `POST /api/v1/comments/{id}/likes` | 30 | 防刷赞 |

> 以上爆破/滥用闸门端点均保持默认 `failOpen = false`：Redis 故障时宁可 503，
> 不在最需要防护时解除防护。

---

## ⚙️ 环境配置

### 开发环境（application-dev.yml）
```yaml
rate-limit:
  enabled: true  # 开发环境开启限流，模拟真实环境
```

### 测试环境（application-test.yml）
```yaml
rate-limit:
  enabled: false  # 测试环境关闭限流，避免影响单元测试
```

### 生产环境（application-prod.yml）
```yaml
rate-limit:
  enabled: true  # 生产环境必须开启限流
```

---

## 🔧 动态调整

### 方式一：修改配置文件
```yaml
rate-limit:
  enabled: false  # 临时关闭限流
```
重启应用生效。

### 方式二：JVM 参数
```bash
java -jar app.jar --rate-limit.enabled=false
```

### 方式三：环境变量
```bash
export RATE_LIMIT_ENABLED=false
java -jar app.jar
```

---

## 📊 工作流程

### 限流流程（与 `RateLimitInterceptor.preHandle` 一致）
```
1. 检查全局开关 rate-limit.enabled，关闭则直接放行
2. 非 HandlerMethod（静态资源等）直接放行
3. 取方法上的 @RateLimit，没有再取类上的；都没有则放行
4. 构建限流 key：rate: + 注解key + (user:{userId} 或 ip:{ip})
   （userId 由前序 JWT 拦截器写入 request 属性）
5. Redis INCR 原子计数；异常/空值 → 按 failOpen 降级（放行或 503）
6. count==1 时设置 timeWindow 过期，开启新窗口
7. count > maxRequests → 429 + R.error(429, message)，拒绝
8. 否则放行；URI 不进了 key，但仍打进日志便于排查触发入口
```

### 执行顺序
```
Request → [Logging:0] → [JWT:1] → [RateLimit:2] → Controller → Response
```
限流排在 JWT 之后，才能拿到已验证的 userId 做身份维度；
拦截器注册顺序见 `WebMvcConfig`。

---

## 💡 最佳实践

### 1. 开发阶段
保持限流开启，但适当放宽限制：
```java
@RateLimit(key = "login:consumer:", timeWindow = 60, maxRequests = 20)
```

### 2. 测试阶段
完全关闭限流：
```yaml
# application-test.yml
rate-limit:
  enabled: false
```

### 3. 生产阶段
严格开启限流（现状取值）：
```java
@RateLimit(key = "login:admin:", timeWindow = 60, maxRequests = 5, message = "登录尝试次数过多，请60秒后再试")
```

### 4. 紧急情况
```bash
# 快速关闭（临时方案，不推荐）
java -jar app.jar --rate-limit.enabled=false

# 或调整阈值（推荐方案）
# 修改代码中的 maxRequests 值并重新部署
```

---

## 🔍 监控与调试

### 查看日志
```bash
# 查看限流日志
grep "API限流" logs/music-backend-all.log

# 查看超限警告
grep "访问超限" logs/music-backend-warn.log

# 统计限流触发次数
grep "访问超限" logs/music-backend-warn.log | wc -l
```

### Redis 监控
```bash
# 查看所有限流计数器（统一在 rate: 命名空间下）
redis-cli keys "rate:*"

# 查看某个身份的登录限流状态（key 形如 rate:login:admin:ip:192.168.1.100）
redis-cli get "rate:login:admin:ip:192.168.1.100"
redis-cli ttl "rate:login:admin:ip:192.168.1.100"

# 清空所有限流数据（测试基类就是这么隔离的）
redis-cli keys "rate:*" | xargs redis-cli del
```

### 验证限流状态
```bash
# 连续调用 10 次管理员登录（登录是表单提交，不是 JSON）
for i in {1..10}; do
  curl -X POST http://localhost:9171/api/v1/admin/sessions \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d 'name=admin&password=wrong'
done

# 限流开启: 第 6 次起返回 429（即使凭据错误也会计数，这正是爆破防护的意义）
# 限流关闭: 所有请求都正常返回业务错误
```

---

## 🚨 注意事项

### ❌ 禁止操作

1. **不要在生产环境关闭限流**
   ```yaml
   # 错误！
   rate-limit:
     enabled: false
   ```

2. **不要忘记测试环境的配置**
   ```yaml
   # 必须关闭，否则单元测试可能失败
   rate-limit:
     enabled: false
   ```

### ✅ 推荐操作

1. **定期检查限流日志**
   ```bash
   grep "API限流" logs/music-backend-warn.log
   ```

2. **根据实际流量调整阈值**
   ```java
   // 根据实际情况调整
   @RateLimit(key = "login:consumer:", timeWindow = 60, maxRequests = 10)
   ```

3. **监控限流触发频率**
   - 频繁触发可能需要调整阈值
   - 可能是遭受攻击的信号

---

## 📈 性能影响

| 操作 | 耗时 | 影响 |
|------|------|------|
| Redis 查询 | ~2-5ms | 轻微，可接受 |
| 开关检查 | <1ms | 忽略不计 |

**总体性能损耗**: <5ms，对用户体验无明显影响

### 开关关闭时
- 几乎无性能开销（只检查一个布尔值）
- 适合测试环境提高测试速度

---

## 🎯 扩展功能

### 1. 基于用户ID的限流（已实现，非扩展项）
现状逻辑见 `RateLimitInterceptor`：已登录请求用 `user:{userId}`，
匿名请求（如登录/注册本身）退回 `ip:{ip}`：
```java
Object userIdAttr = request.getAttribute("userId");
String identity = userIdAttr != null ? "user:" + userIdAttr : "ip:" + ip;
String key = RATE_KEY_NAMESPACE + rateLimit.key() + identity;
```

### 2. 自定义限流Key生成策略（扩展示意）
```java
@RateLimit(key = "search:", timeWindow = 60, maxRequests = 30)
@PostMapping("/api/v1/examples/search")
public R<SearchVo> search(@Valid @RequestBody SearchRequest request) {
    // 同一接口所有调用共享 key+身份 维度的计数器
}
```

---

## 📁 相关文件

| 文件 | 路径 | 说明 |
|------|------|------|
| `RateLimitInterceptor.java` | `interceptor/` | 限流拦截器实现 |
| `RateLimit.java` | `annotation/` | 限流注解定义 |
| `application-dev.yml` | `resources/` | 开发环境配置 |
| `application-test.yml` | `resources/` | 测试环境配置 |
| `application-prod.yml` | `resources/` | 生产环境配置 |

---

## 🎉 总结

### 已完成的功能
✅ 基于 Redis 原子 INCR 的固定窗口限流  
✅ 注解式配置（@RateLimit，10 个高危/滥用面端点已覆盖）  
✅ 身份维度（登录用户优先，退回 IP）  
✅ 统一 `rate:` 命名空间，测试可一次清零隔离  
✅ Redis 故障显式降级（failOpen，默认 503）  
✅ 全局开关控制（rate-limit.enabled）与多环境配置（dev/test/prod）  

### 优势
🚀 防止暴力破解 - 登录/注册限流  
🚀 防止接口滥用 - 评论/点赞/上传限流  
🚀 防绕过 - 限流键不拼 URI，URL 变体造不出新计数器  
🚀 故障语义清楚 - 429（你太快）与 503（服务端依赖故障）不混淆  
🚀 易于使用 - 注解式配置  

---

## 📖 参考文档

- [Redis Rate Limiting](https://redis.io/docs/manual/patterns/rate-limiter/)
- [Spring Interceptors](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-config/interceptors.html)
- 计数模型为固定窗口（INCR + 首次设 TTL），非滑动窗口日志/令牌桶；
  窗口边界存在短暂双峰宽容，对爆破防护类场景足够，详见 `RateLimitInterceptor` 注释
