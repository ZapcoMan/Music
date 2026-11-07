# API 限流配置与使用

## 📋 概述

项目已实现基于 Redis 的 **API 限流系统**，包括：
- ✅ 注解式限流配置（@RateLimit）
- ✅ 滑动窗口算法实现
- ✅ 全局开关控制（开发/测试/生产环境灵活配置）
- ✅ 登录接口默认保护（60秒最多5次尝试）

---

## 🔧 核心功能

### 1. 限流实现
- 基于 Redis 实现滑动窗口限流
- 支持自定义时间窗口和最大请求数
- 按 IP + URI 维度进行限流

### 2. 全局开关
- 可通过配置文件动态开启/关闭限流
- 不同环境使用不同配置
- 默认值：true（安全优先）

### 3. 注解式配置
```java
@RateLimit(timeWindow = 60, maxRequests = 5)
```

---

## 🛠️ 技术实现

### 核心文件

| 文件 | 路径 | 说明 |
|------|------|------|
| `RateLimit.java` | `annotation/` | 限流注解定义 |
| `RateLimitInterceptor.java` | `interceptor/` | 限流拦截器实现 |
| `WebMvcConfig.java` | `config/` | 拦截器注册配置 |

### 限流注解参数

```java
@RateLimit(
    key = "rate_limit:",      // 限流key前缀
    timeWindow = 60,          // 时间窗口（秒）
    maxRequests = 100,        // 最大请求次数
    message = "访问过于频繁"   // 超限提示信息
)
```

---

## 📝 使用方法

### 1. 为接口添加限流

#### Controller 方法级别
```java
@RestController
@RequestMapping("/api")
public class ApiController {

    /**
     * 限制: 60秒内最多访问100次
     */
    @GetMapping("/data")
    @RateLimit(timeWindow = 60, maxRequests = 100)
    public R getData() {
        return R.ok(data);
    }

    /**
     * 限制: 30秒内最多访问10次
     */
    @PostMapping("/submit")
    @RateLimit(key = "custom_key:", timeWindow = 30, maxRequests = 10, 
               message = "提交过于频繁")
    public R submitData() {
        return R.ok();
    }
}
```

#### Controller 类级别（所有方法生效）
```java
@RestController
@RequestMapping("/admin")
@RateLimit(timeWindow = 60, maxRequests = 50)
public class AdminController {
    // 所有接口都受限于 60秒/50次
}
```

### 2. 限流策略建议

| 接口类型 | 时间窗口 | 最大请求数 | 示例 |
|---------|---------|-----------|------|
| 登录接口 | 60秒 | 5次 | `@RateLimit(timeWindow=60, maxRequests=5)` |
| 数据查询 | 60秒 | 100次 | `@RateLimit(timeWindow=60, maxRequests=100)` |
| 数据提交 | 30秒 | 10次 | `@RateLimit(timeWindow=30, maxRequests=10)` |
| 敏感操作 | 60秒 | 3次 | `@RateLimit(timeWindow=60, maxRequests=3)` |

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

### 限流流程
```
1. RateLimitInterceptor拦截请求
2. 检查方法/类是否有@RateLimit注解
3. 获取客户端IP地址
4. 构建限流key: rate_limit:{ip}:{uri}
5. 查询Redis中当前访问次数
6. 如果超过限制，返回429错误
7. 否则访问次数+1，放行请求
```

### 执行顺序
```
Request → [Logging:0] → [JWT:1] → [RateLimit:2] → Controller → Response
```

---

## 💡 最佳实践

### 1. 开发阶段
保持限流开启，但适当放宽限制：
```java
@RateLimit(key = "login:", timeWindow = 60, maxRequests = 20)
```

### 2. 测试阶段
完全关闭限流：
```yaml
# application-test.yml
rate-limit:
  enabled: false
```

### 3. 生产阶段
严格开启限流：
```java
@RateLimit(key = "login:", timeWindow = 60, maxRequests = 5)
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
# 查看限流计数
redis-cli keys "rate_limit:*"

# 查看某个IP的限流状态
redis-cli get "rate_limit:192.168.1.100:/admin/login/status"

# 清空所有限流数据
redis-cli keys "rate_limit:*" | xargs redis-cli del
```

### 验证限流状态
```bash
# 快速调用10次测试
for i in {1..10}; do
  curl http://localhost:9171/admin/login/status \
    -X POST \
    -H "Content-Type: application/json" \
    -d '{"name":"admin","password":"123"}'
done

# 限流开启: 第6次返回 429
# 限流关闭: 所有请求都正常
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
   @RateLimit(key = "login:", timeWindow = 60, maxRequests = 10)
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

### 1. 基于用户ID的限流
```java
// 修改RateLimitInterceptor
String userId = request.getAttribute("userId");
if (userId != null) {
    key = rateLimit.key() + "user:" + userId;
} else {
    key = rateLimit.key() + "ip:" + ip;
}
```

### 2. 自定义限流Key生成策略
```java
@RateLimit(key = "api:search:", timeWindow = 60, maxRequests = 30)
@GetMapping("/search")
public R search(@RequestParam String keyword) {
    // 按搜索关键词限流
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
✅ 基于 Redis 的滑动窗口限流  
✅ 注解式配置（@RateLimit）  
✅ 全局开关控制（rate-limit.enabled）  
✅ 多环境配置（dev/test/prod）  
✅ 登录接口默认保护  

### 优势
🚀 防止暴力破解 - 登录限流  
🚀 防止接口滥用 - API限流  
🚀 灵活控制 - 全局开关  
🚀 易于使用 - 注解式配置  

---

## 📖 参考文档

- [Redis Rate Limiting](https://redis.io/docs/manual/patterns/rate-limiter/)
- [Spring Interceptors](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-config/interceptors.html)
- [滑动窗口算法](https://en.wikipedia.org/wiki/Sliding_window_protocol)
