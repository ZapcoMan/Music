# 请求/响应日志快速指南

## 🚀 快速开始

### 1. 启动应用

```bash
cd Music-backend
mvn spring-boot:run
```

### 2. 发起请求

访问任意 API 接口,例如:
```bash
curl http://localhost:9171/admin/login/status \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"name":"admin","password":"123456"}'
```

### 3. 查看日志

```bash
# 实时查看控制台
# (启动时即可看到)

# 查看日志文件
tail -f logs/music-backend-info.log

# 搜索特定请求
grep "请求ID" logs/music-backend-all.log
```

---

## 📊 日志示例

### 正常请求

```
========== 请求开始 ==========
【请求ID】      : 1715097600000-1234
【请求时间】    : 2026-05-07T20:45:30.123
【请求方法】    : POST
【请求URL】     : /admin/login/status
【查询参数】    : 
【客户端IP】    : 192.168.1.100
【User-Agent】  : Mozilla/5.0...
【Content-Type】: application/json
【请求头】      : {Accept=application/json, Authorization=Bearer eyJhbGci***}
【请求体】      : {"name":"admin","password":"***"}

========== 请求结束 ==========
【请求ID】      : 1715097600000-1234
【响应状态】    : 200
【耗时】        : 156 ms
【异常信息】    : 无
============================
```

### 慢请求

```
⚠️ 慢请求警告 - URL: /song/allSong, 耗时: 4523 ms
```

### 异常请求

```
========== 请求结束 ==========
【请求ID】      : 1715097600000-5678
【响应状态】    : 500
【耗时】        : 234 ms
【异常信息】    : NullPointerException: ...
============================
```

---

## 🔍 常用查询

### 1. 查找特定请求

```bash
# 通过请求ID
grep "1715097600000-1234" logs/music-backend-all.log

# 通过URL
grep "/admin/login" logs/music-backend-all.log

# 通过IP
grep "192.168.1.100" logs/music-backend-all.log
```

### 2. 性能分析

```bash
# 查找慢请求
grep "慢请求警告" logs/music-backend-warn.log

# 统计平均响应时间
grep "耗时" logs/music-backend-info.log
```

### 3. 错误排查

```bash
# 查看所有错误
grep "响应状态.*: 5" logs/music-backend-error.log

# 查看异常详情
grep "请求处理异常" logs/music-backend-error.log -A 10
```

### 4. 安全审计

```bash
# 统计各IP请求次数
grep "客户端IP" logs/music-backend-all.log | sort | uniq -c | sort -rn

# 查看登录尝试
grep "login" logs/music-backend-all.log
```

---

## ⚙️ 配置调整

### 排除更多路径

编辑 `WebMvcConfig.java`:

```java
.excludePathPatterns(
    "/actuator/**",
    "/swagger-ui/**",
    "/v3/api-docs/**",
    "/error",
    "/static/**",        // 新增: 静态资源
    "/favicon.ico"       // 新增: 图标
)
```

### 修改慢请求阈值

编辑 `RequestResponseLoggingInterceptor.java`:

```java
if (duration > 5000) {  // 改为5秒
    log.warn("⚠️ 慢请求警告 - URL: {}, 耗时: {} ms", ...);
}
```

### 添加脱敏规则

编辑 `maskSensitiveBody()` 方法:

```java
body = body.replaceAll("\"idCard\"\\s*:\\s*\"[^\"]*\"", "\"idCard\":\"***\"");
body = body.replaceAll("\"phone\"\\s*:\\s*\"[^\"]*\"", "\"phone\":\"***\"");
```

### 关闭日志

```yaml
# application.yml
logging:
  level:
    com.shanzhu.music.interceptor.RequestResponseLoggingInterceptor: OFF
```

---

## 💡 提示

- ✅ 每个请求有唯一ID,便于追踪
- ✅ 密码、Token自动脱敏
- ✅ 支持代理场景的真实IP获取
- ✅ 超过3秒的请求会发出警告
- ✅ 请求体最大记录10KB

---

📖 详细文档: [REQUEST_LOGGING_CONFIG.md](REQUEST_LOGGING_CONFIG.md)
