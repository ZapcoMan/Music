# 请求/响应审计日志

> ⚠️ **口径声明**：本文为历史快照。示例中的 `/admin/login/status`、`/song/allSong` 等
> 动词式旧路径已 RESTful 化为 `/api/v1/*`；本项目 `Authorization` 头传的是**裸 JWT、
> 不带 `Bearer ` 前缀**；登录/改资料走表单提交，表单体同样含明文凭令，脱敏已覆盖
> form 体；另新增操作审计（OperationLogService 登录/登出/重操作落库）。拦截器
> 顺序 Logging:0 → JWT:1 → RateLimit:2 仍与现状一致。总体口径见
> [FIX-LOG-2026-11.md](FIX-LOG-2026-11.md)、[CHANGELOG.md](../CHANGELOG.md)。

## 📋 概述

项目已配置 **RequestResponseLoggingInterceptor**，用于记录所有 API 请求和响应的详细信息，便于：
- 🔍 问题排查和调试
- 📊 性能监控和分析
- 🛡️ 安全审计和追溯
- 📈 接口调用统计

---

## ✅ 核心功能

### 1. 请求日志记录

记录以下信息：
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

### 2. 响应日志记录

记录以下信息：
- ✅ 请求ID（与请求对应）
- ✅ 响应状态码
- ✅ 耗时（毫秒）
- ✅ 异常信息

### 3. 智能特性

- 🔐 **敏感信息脱敏**：自动隐藏密码、Token、Cookie
- ⚡ **性能监控**：超过3秒的慢请求会发出警告
- 🎯 **请求追踪**：每个请求有唯一ID，便于链路追踪
- 🌐 **真实IP获取**：支持多层代理场景
- 📦 **大内容截断**：请求体超过10KB自动截断

---

## 📊 日志输出示例

### 请求开始日志

```
========== 请求开始 ==========
【请求ID】      : 1715097600000-1234
【请求时间】    : 2026-05-07T20:45:30.123
【请求方法】    : POST
【请求URL】     : /admin/login/status
【查询参数】    : 
【客户端IP】    : 192.168.1.100
【User-Agent】  : Mozilla/5.0 (Windows NT 10.0; Win64; x64)...
【Content-Type】: application/json
【请求头】      : {Accept=application/json, Authorization=Bearer eyJhbGci***, ...}
【请求体】      : {"name":"admin","password":"***"}
```

### 请求结束日志

```
========== 请求结束 ==========
【请求ID】      : 1715097600000-1234
【响应状态】    : 200
【耗时】        : 156 ms
【异常信息】    : 无
============================
```

### 慢请求警告

```
⚠️ 慢请求警告 - URL: /song/allSong, 耗时: 4523 ms
```

### 异常日志

```
========== 请求结束 ==========
【请求ID】      : 1715097600000-5678
【响应状态】    : 500
【耗时】        : 234 ms
【异常信息】    : NullPointerException: Cannot invoke method on null object
============================

ERROR c.s.m.interceptor.RequestResponseLoggingInterceptor - 请求处理异常
java.lang.NullPointerException: Cannot invoke method on null object
    at com.shanzhu.music.service.SongService.getDetail(SongService.java:45)
    ...
```

---

## 🔧 配置说明

### 1. 拦截器注册

位置：`WebMvcConfig.java`

```java
registry.addInterceptor(requestResponseLoggingInterceptor)
    .addPathPatterns("/**")
    .excludePathPatterns(
        "/actuator/**",      // 排除监控端点
        "/swagger-ui/**",    // 排除Swagger UI
        "/v3/api-docs/**",   // 排除API文档
        "/error"             // 排除错误页面
    )
    .order(0);  // 最先执行
```

### 2. 排除路径

以下路径不会被记录：
- `/actuator/**` - Spring Boot Actuator 监控端点
- `/swagger-ui/**` - Swagger UI 静态资源
- `/v3/api-docs/**` - OpenAPI 文档接口
- `/error` - 错误页面

可根据需要添加更多排除路径。

### 3. 执行顺序

| 顺序 | 拦截器 | 作用 |
|------|--------|------|
| 0 | RequestResponseLoggingInterceptor | 记录请求/响应日志 |
| 1 | JwtAuthenticationInterceptor | JWT 认证 |
| 2 | RateLimitInterceptor | API 限流 |

---

## 🔐 敏感信息脱敏

### 自动脱敏的字段

#### 请求头
- `Authorization`: `Bearer eyJhbGci***`（只显示前15字符）
- `Cookie`: `***`（完全隐藏）

#### 请求体（JSON格式）
- `password`: `"***"`
- `token`: `"***"`
- `secret`: `"***"`

### 自定义脱敏规则

编辑 `RequestResponseLoggingInterceptor.java`：

```java
private String maskSensitiveBody(String body, String contentType) {
    // 添加更多脱敏规则
    body = body.replaceAll("\"idCard\"\\s*:\\s*\"[^\"]*\"", "\"idCard\":\"***\"");
    body = body.replaceAll("\"phone\"\\s*:\\s*\"[^\"]*\"", "\"phone\":\"***\"");
    return body;
}
```

---

## 📈 性能监控

### 慢请求阈值

当前配置：**3000ms（3秒）**

修改阈值：
```java
if (duration > 5000) {  // 改为5秒
    log.warn("⚠️ 慢请求警告 - URL: {}, 耗时: {} ms", request.getRequestURI(), duration);
}
```

### 性能优化建议

1. **异步日志**：已配置 Logback AsyncAppender
2. **限制日志长度**：请求体最大10KB
3. **排除不必要路径**：减少无效日志
4. **生产环境调整**：可降低日志级别为 WARN

---

## 🎯 使用场景

### 1. 问题排查

**场景**：用户反馈某个接口报错

**操作**：
```bash
# 搜索特定请求ID
grep "1715097600000-1234" logs/music-backend-all.log

# 查看某个接口的所有请求
grep "/admin/login/status" logs/music-backend-all.log

# 查看所有错误请求
grep "响应状态.*: 5" logs/music-backend-error.log
```

### 2. 性能分析

**场景**：分析接口响应时间

**操作**：
```bash
# 查找慢请求
grep "慢请求警告" logs/music-backend-warn.log

# 统计平均响应时间
grep "耗时" logs/music-backend-info.log | awk '{sum+=$NF; count++} END {print "Average:", sum/count, "ms"}'
```

### 3. 安全审计

**场景**：检查是否有异常访问

**操作**：
```bash
# 查看某个IP的所有请求
grep "192.168.1.100" logs/music-backend-all.log

# 查看登录失败记录
grep "登录" logs/music-backend-all.log | grep "401"

# 统计各IP的请求次数
grep "客户端IP" logs/music-backend-all.log | sort | uniq -c | sort -rn
```

### 4. 接口调用统计

**场景**：统计接口调用频率

**操作**：
```bash
# 统计各接口的调用次数
grep "请求URL" logs/music-backend-info.log | sort | uniq -c | sort -rn

# 统计某天的请求总量
grep "2026-05-07" logs/music-backend-info.log | wc -l
```

---

## 💡 最佳实践

### 1. 开发环境

```yaml
# application-dev.yml
logging:
  level:
    com.shanzhu.music.interceptor.RequestResponseLoggingInterceptor: INFO
```

✅ 记录所有请求  
✅ 详细日志输出  
✅ 便于调试

### 2. 测试环境

```yaml
# application-test.yml
logging:
  level:
    com.shanzhu.music.interceptor.RequestResponseLoggingInterceptor: INFO
```

✅ 记录所有请求  
✅ 性能监控  
✅ 问题排查

### 3. 生产环境

```yaml
# application-prod.yml
logging:
  level:
    com.shanzhu.music.interceptor.RequestResponseLoggingInterceptor: WARN
```

✅ 只记录警告和错误  
✅ 减少日志量  
✅ 提升性能

---

## 🔍 常见问题

### Q1: 如何关闭请求日志？

**A**: 在 `application.yml` 中设置：
```yaml
logging:
  level:
    com.shanzhu.music.interceptor.RequestResponseLoggingInterceptor: OFF
```

或在 `WebMvcConfig` 中注释掉拦截器注册。

### Q2: 日志太多怎么办？

**A**: 
1. 排除不必要的路径
2. 降低日志级别为 WARN
3. 增加日志滚动策略的保留天数
4. 使用 ELK 等日志分析工具

### Q3: 如何查看特定用户的操作？

**A**: 
```bash
# 如果请求体包含userId
grep "userId=123" logs/music-backend-all.log

# 或通过IP追踪
grep "192.168.1.100" logs/music-backend-all.log
```

### Q4: 请求体为什么没有记录？

**A**: 
- 只有 POST/PUT/PATCH 方法会记录请求体
- GET/DELETE 方法没有请求体
- 请求体为空时不记录

### Q5: 如何添加更多脱敏规则？

**A**: 编辑 `maskSensitiveBody()` 方法：
```java
body = body.replaceAll("\"email\"\\s*:\\s*\"[^\"]*\"", "\"email\":\"***\"");
body = body.replaceAll("\"address\"\\s*:\\s*\"[^\"]*\"", "\"address\":\"***\"");
```

---

## 🚀 下一步优化

### 1. 集成链路追踪
- 使用 Sleuth + Zipkin
- 分布式追踪 ID
- 微服务调用链

### 2. 日志分析平台
- ELK Stack（Elasticsearch + Logstash + Kibana）
- Grafana Loki
- Splunk

### 3. 实时监控
- Prometheus + Grafana
- 接口QPS监控
- 响应时间告警

### 4. 审计日志持久化
- 存储到数据库
- 长期归档
- 合规审计

---

## 📁 文件清单

| 文件 | 路径 | 说明 |
|------|------|------|
| `RequestResponseLoggingInterceptor.java` | `interceptor/` | 拦截器实现（243行） |
| `WebMvcConfig.java` | `config/` | 拦截器注册（已更新） |

---

## 🎉 总结

### 已完成的功能
✅ 完整的请求/响应日志记录  
✅ 唯一请求ID追踪  
✅ 性能监控和慢请求警告  
✅ 安全审计支持  
✅ 敏感信息自动脱敏  
✅ 真实IP获取（支持代理）  

### 优势
🔍 易于问题排查  
📊 性能监控可视化  
🛡️ 安全审计完整  
🔐 敏感信息保护  

---

## 📖 参考文档

- [Spring Interceptors](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-config/interceptors.html)
- [ContentCachingRequestWrapper](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/util/ContentCachingRequestWrapper.html)
- [SLF4J Best Practices](https://www.slf4j.org/faq.html)
