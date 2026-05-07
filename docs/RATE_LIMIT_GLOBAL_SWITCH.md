# API 限流全局开关配置说明

## 📋 概述

项目已添加 **API 限流全局开关**,可以灵活控制是否启用限流功能,便于不同环境下的测试和部署。

---

## ✅ 配置内容

### 1. 配置文件

#### 开发环境 (application-dev.yml)
```yaml
rate-limit:
  enabled: true  # 开发环境开启限流
```

#### 生产环境 (application-prod.yml)
```yaml
rate-limit:
  enabled: true  # 生产环境开启限流
```

#### 测试环境 (application-test.yml)
```yaml
rate-limit:
  enabled: false  # 测试环境关闭限流,避免影响单元测试
```

### 2. 拦截器实现

位置: `RateLimitInterceptor.java`

```java
// API限流全局开关
@Value("${rate-limit.enabled:true}")
private boolean rateLimitEnabled;

@Override
public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    // 检查全局开关是否开启
    if (!rateLimitEnabled) {
        log.debug("API限流已全局关闭");
        return true;
    }
    
    // ... 限流逻辑
}
```

---

## 🎯 使用场景

### 1. 开发环境

**配置**: `enabled: true`

**原因**: 
- ✅ 模拟真实环境行为
- ✅ 及时发现限流相关问题
- ✅ 调试限流逻辑

### 2. 测试环境

**配置**: `enabled: false`

**原因**:
- ❌ 避免影响单元测试
- ❌ 防止并发测试被限流
- ✅ 提高测试执行速度

### 3. 生产环境

**配置**: `enabled: true`

**原因**:
- ✅ 保护系统免受恶意攻击
- ✅ 防止暴力破解
- ✅ 保障服务稳定性

---

## 🔧 动态调整

### 方式一: 修改配置文件

编辑对应环境的 `application-{profile}.yml`:

```yaml
rate-limit:
  enabled: false  # 临时关闭限流
```

重启应用生效。

### 方式二: JVM 参数

启动时通过命令行参数覆盖:

```bash
java -jar app.jar --rate-limit.enabled=false
```

### 方式三: 环境变量

```bash
export RATE_LIMIT_ENABLED=false
java -jar app.jar
```

---

## 📊 各环境对比

| 环境 | 限流状态 | 说明 |
|------|---------|------|
| **dev** | ✅ 开启 | 开发调试,模拟真实环境 |
| **test** | ❌ 关闭 | 避免影响单元测试 |
| **prod** | ✅ 开启 | 生产保护,防止攻击 |

---

## 💡 最佳实践

### 1. 开发阶段

保持限流开启,但可以适当放宽限制:

```java
@RateLimit(key = "login:", timeWindow = 60, maxRequests = 20)  // 放宽到20次/分钟
```

### 2. 测试阶段

关闭限流,确保测试不受影响:

```yaml
# application-test.yml
rate-limit:
  enabled: false
```

### 3. 生产阶段

严格开启限流,保护系统安全:

```java
@RateLimit(key = "login:", timeWindow = 60, maxRequests = 5)  // 严格限制5次/分钟
```

### 4. 紧急情况

如果遭遇大规模攻击,可以临时调整:

```bash
# 紧急关闭限流(不推荐)
java -jar app.jar --rate-limit.enabled=false

# 或更推荐: 调整限流阈值
# 修改代码中的 maxRequests 值并重新部署
```

---

## 🔍 验证方法

### 1. 查看日志

启动应用后,观察日志输出:

```
# 限流开启时
DEBUG c.s.m.i.RateLimitInterceptor - API限流 - 首次访问: IP=192.168.1.100, URI=/admin/login/status

# 限流关闭时
DEBUG c.s.m.i.RateLimitInterceptor - API限流已全局关闭
```

### 2. 测试接口

快速多次调用限流接口:

```bash
# 限流开启时,第6次会返回429
for i in {1..10}; do
  curl http://localhost:9171/admin/login/status -X POST -d '{"name":"admin","password":"123"}'
done

# 限流关闭时,所有请求都正常
```

### 3. 检查配置

```bash
# 查看当前激活的配置
curl http://localhost:9171/actuator/env | grep rate-limit
```

---

## ⚙️ 配置优先级

Spring Boot 配置加载优先级(从高到低):

1. **命令行参数**: `--rate-limit.enabled=false`
2. **JVM 系统属性**: `-Dratelimit.enabled=false`
3. **环境变量**: `RATE_LIMIT_ENABLED=false`
4. **application-{profile}.yml**: 环境特定配置
5. **application.yml**: 默认配置

---

## 🚨 注意事项

### 1. 不要在生产环境关闭限流

❌ **错误做法**:
```yaml
# application-prod.yml
rate-limit:
  enabled: false  # 危险!
```

✅ **正确做法**:
```yaml
# application-prod.yml
rate-limit:
  enabled: true  # 必须开启
```

### 2. 测试环境务必关闭限流

避免单元测试因限流而失败:

```java
@Test
void testLogin() {
    // 如果限流开启,快速调用可能失败
    for (int i = 0; i < 10; i++) {
        loginService.login("admin", "123");
    }
}
```

### 3. 监控限流状态

建议在 Actuator 中添加自定义端点:

```java
@Component
public class RateLimitEndpoint {
    
    @Value("${rate-limit.enabled:true}")
    private boolean enabled;
    
    @Bean
    public Endpoint<Map<String, Object>> rateLimitStatus() {
        return () -> Map.of("enabled", enabled);
    }
}
```

---

## 📁 相关文件

| 文件 | 路径 | 说明 |
|------|------|------|
| `RateLimitInterceptor.java` | `src/main/java/.../interceptor/` | 限流拦截器(已更新) |
| `application-dev.yml` | `src/main/resources/` | 开发环境配置(已更新) |
| `application-test.yml` | `src/main/resources/` | 测试环境配置(已更新) |
| `application-prod.yml` | `src/main/resources/` | 生产环境配置(已更新) |

---

## 🎯 总结

✅ **已完成**:
- 添加 `rate-limit.enabled` 配置项
- 在三个环境中分别配置
- 修改拦截器支持全局开关
- 默认值为 `true`(开启)

✅ **优势**:
- 灵活控制限流功能
- 测试环境不受影响
- 生产环境保护到位
- 支持动态调整

📖 **参考**: [Spring Boot Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
