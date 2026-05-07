# Logback 日志配置说明

## 📋 概述

项目已配置完整的 Logback 日志系统,支持:
- ✅ 彩色控制台输出
- ✅ 分级日志文件(INFO/WARN/ERROR/ALL)
- ✅ 按日期和大小的滚动策略
- ✅ 异步日志输出(提升性能)
- ✅ MyBatis SQL 日志
- ✅ 第三方框架日志控制

---

## 📁 日志文件结构

```
logs/
├── music-backend-info.log          # INFO 级别日志
├── music-backend-info.2026-05-07.0.log
├── music-backend-warn.log          # WARN 级别日志
├── music-backend-warn.2026-05-07.0.log
├── music-backend-error.log         # ERROR 级别日志
├── music-backend-error.2026-05-07.0.log
├── music-backend-all.log           # 所有级别完整日志
└── music-backend-all.2026-05-07.0.log
```

---

## ⚙️ 配置详解

### 1. 日志级别

| 级别 | 说明 | 使用场景 |
|------|------|---------|
| **DEBUG** | 调试信息 | 开发环境,SQL 语句 |
| **INFO** | 一般信息 | 业务逻辑,关键操作 |
| **WARN** | 警告信息 | 潜在问题,不影响运行 |
| **ERROR** | 错误信息 | 异常,失败操作 |

### 2. 日志格式

#### 控制台彩色格式
```
2026-05-07 20:30:45.123  INFO 12345 --- [nio-9171-exec-1] c.s.m.controller.SingerController      : 查询所有歌手
```

颜色说明:
- 🔵 时间戳: 淡色
- 🟢 日志级别: 根据级别着色
- 🟣 PID: 洋红色
- 🔵 线程名: 淡色
- 🔵 Logger 名称: 青色

#### 文件格式
```
2026-05-07 20:30:45.123 [http-nio-9171-exec-1] INFO  c.s.m.controller.SingerController - 查询所有歌手
```

### 3. 滚动策略

#### 文件大小限制
- 单个文件最大: **100MB**
- 超过后自动创建新文件(后缀 .0, .1, .2...)

#### 保留策略
| 日志类型 | 保留天数 | 总大小上限 |
|---------|---------|-----------|
| INFO | 30 天 | 10GB |
| WARN | 60 天 | 15GB |
| ERROR | 90 天 | 20GB |
| ALL | 15 天 | 5GB |

### 4. 异步输出

使用 `AsyncAppender` 提升性能:
- **队列大小**: 512 (INFO/WARN), 1024 (ALL)
- **不丢弃日志**: `discardingThreshold=0`
- **优势**: 减少 I/O 阻塞,提升响应速度

---

## 🔧 自定义配置

### 修改日志级别

在 `application.yml` 中覆盖:

```yaml
logging:
  level:
    com.shanzhu.music: DEBUG  # 开发时查看详细日志
    com.shanzhu.music.mapper: DEBUG  # SQL 日志
```

### 修改日志路径

编辑 `logback-spring.xml`:

```xml
<property name="LOG_PATH" value="/var/logs/music"/>
```

### 启用特定包的 DEBUG 日志

```xml
<!-- 例如:启用 JWT 工具类的详细日志 -->
<logger name="com.shanzhu.music.utils.JwtUtils" level="DEBUG"/>
```

---

## 📊 各模块日志配置

### 1. 业务代码 (com.shanzhu.music)
- **级别**: INFO
- **输出**: 控制台 + 所有文件
- **说明**: 记录关键业务操作

### 2. MyBatis Mapper (com.shanzhu.music.mapper)
- **级别**: DEBUG
- **输出**: 控制台 + ALL 文件
- **说明**: 显示执行的 SQL 语句和参数

### 3. Spring Framework
- **级别**: WARN
- **输出**: 控制台 + WARN 文件
- **说明**: 只记录警告和错误

### 4. Spring Security
- **级别**: INFO
- **输出**: 控制台 + INFO 文件
- **说明**: 记录认证和授权事件

### 5. Druid 数据源
- **级别**: WARN
- **输出**: 控制台 + WARN 文件
- **说明**: 连接池警告信息

### 6. Redis
- **级别**: WARN
- **输出**: 控制台
- **说明**: 缓存操作警告

---

## 💡 最佳实践

### 1. 合理使用日志级别

```java
// ✅ 正确示例
log.info("用户登录成功 - userId={}, username={}", userId, username);
log.warn("Redis 连接超时,使用本地缓存");
log.error("数据库连接失败", exception);

// ❌ 避免
log.info("进入方法");  // 太频繁,用 DEBUG
log.debug("用户ID: " + userId);  // 应该用占位符
```

### 2. 使用占位符而非字符串拼接

```java
// ✅ 推荐
log.info("订单创建成功 - orderId={}, amount={}", orderId, amount);

// ❌ 不推荐
log.info("订单创建成功 - orderId=" + orderId + ", amount=" + amount);
```

### 3. 异常日志包含堆栈

```java
// ✅ 正确
try {
    // ...
} catch (Exception e) {
    log.error("处理失败 - orderId={}", orderId, e);  // 传入异常对象
}

// ❌ 错误
log.error("处理失败: " + e.getMessage());  // 丢失堆栈信息
```

### 4. 敏感信息脱敏

```java
// ✅ 脱敏
log.info("用户登录 - username={}", maskUsername(username));

// ❌ 泄露密码
log.info("用户登录 - password={}", password);
```

---

## 🔍 日志查看技巧

### 1. 实时查看控制台日志

```bash
# 启动应用
mvn spring-boot:run

# 或使用 Docker
docker-compose logs -f backend
```

### 2. 查看特定级别日志

```bash
# 只看错误日志
tail -f logs/music-backend-error.log

# 只看警告日志
tail -f logs/music-backend-warn.log
```

### 3. 搜索特定内容

```bash
# 搜索某个用户的操作
grep "userId=123" logs/music-backend-all.log

# 搜索 SQL 语句
grep "SELECT" logs/music-backend-all.log

# 统计错误数量
grep -c "ERROR" logs/music-backend-error.log
```

### 4. 查看今天的日志

```bash
# Linux/Mac
cat logs/music-backend-info.log

# Windows PowerShell
Get-Content logs/music-backend-info.log -Tail 100
```

---

## 🐛 常见问题

### Q1: 日志文件太大怎么办?

**A**: 配置已自动滚动,但可以调整:
```xml
<maxFileSize>50MB</maxFileSize>  <!-- 减小单文件大小 -->
<maxHistory>15</maxHistory>      <!-- 减少保留天数 -->
```

### Q2: 如何关闭 SQL 日志?

**A**: 修改 `application.yml`:
```yaml
logging:
  level:
    com.shanzhu.music.mapper: WARN  # 改为 WARN 或 ERROR
```

### Q3: 生产环境需要哪些日志?

**A**: 建议配置:
- ✅ ERROR 日志(永久保留)
- ✅ INFO 日志(保留 30 天)
- ❌ DEBUG 日志(关闭)
- ❌ 控制台输出(可选)

### Q4: 日志不输出怎么办?

**A**: 检查:
1. `logback-spring.xml` 是否在 `src/main/resources/`
2. 是否有其他日志配置冲突
3. 日志级别是否设置正确

---

## 📈 性能优化

### 1. 异步日志
已配置 `AsyncAppender`,无需额外优化。

### 2. 减少不必要的日志
```java
// ❌ 避免在循环中打印
for (Item item : items) {
    log.info("处理项: {}", item);  // 太频繁!
}

// ✅ 汇总打印
log.info("批量处理完成 - 总数={}, 成功={}", total, success);
```

### 3. 条件日志
```java
// 只在 DEBUG 级别时才计算
if (log.isDebugEnabled()) {
    log.debug("复杂计算结果: {}", expensiveOperation());
}
```

---

## 🎯 总结

✅ **已完成配置**:
- 彩色控制台输出
- 4 种日志文件(INFO/WARN/ERROR/ALL)
- 按日期和大小滚动
- 异步输出提升性能
- MyBatis SQL 日志
- 第三方框架日志控制

✅ **日志位置**: `./logs/` 目录

✅ **配置文件**: 
- `logback-spring.xml` - 主要配置
- `application.yml` - 日志级别覆盖

📖 **参考**: [Spring Boot Logging](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.logging)
