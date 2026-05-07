# 参数校验与审计日志配置方案

## 📋 问题现状

### 1. 参数校验 ❌
- **现状**: 没有任何 `@Valid` 或 `@Validated` 注解
- **影响**: 
  - 增删改接口没有参数校验
  - 非法数据可能进入业务层
  - 缺少统一的参数验证机制

### 2. 审计日志 ❌  
- **现状**: 登录/删除/修改等操作没有记录
- **影响**:
  - 无法追溯用户操作
  - 安全问题难以排查
  - 不符合合规要求

---

## ✅ 解决方案

### 一、参数校验实现

#### 1. 创建 DTO 类

已创建以下DTO类用于参数校验:

**① AdminLoginRequest.java** - 管理员登录
```java
@Data
public class AdminLoginRequest {
    @NotBlank(message = "管理员账号不能为空")
    @Size(min = 1, max = 50, message = "账号长度必须在1-50之间")
    private String name;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度必须在6-50之间")
    private String password;
}
```

**② ConsumerRequest.java** - 用户注册/更新
```java
@Data
public class ConsumerRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 50, message = "用户名长度必须在2-50之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度必须在6-50之间")
    private String password;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phoneNum;

    @Email(message = "邮箱格式不正确")
    private String email;
    
    // ... 其他字段
}
```

#### 2. Controller 中使用 @Valid

**修改前**:
```java
@PostMapping("/admin/login/status")
public R<Map<String, Object>> loginStatus(String name, String password) {
    // 手动校验
    if (StrUtil.isBlank(name) || StrUtil.isBlank(password)) {
        return R.error("用户名或者密码不能为空");
    }
    // ...
}
```

**修改后**:
```java
@PostMapping("/admin/login/status")
public R<Map<String, Object>> loginStatus(@Valid @RequestBody AdminLoginRequest request) {
    // 自动校验,失败抛出 MethodArgumentNotValidException
    // 由全局异常处理器统一处理
    // ...
}
```

#### 3. 常用校验注解

| 注解 | 说明 | 示例 |
|------|------|------|
| `@NotNull` | 不能为null | `@NotNull(message = "ID不能为空")` |
| `@NotBlank` | 不能为空白字符串 | `@NotBlank(message = "名称不能为空")` |
| `@NotEmpty` | 不能为空集合/数组 | `@NotEmpty(message = "列表不能为空")` |
| `@Size` | 长度范围 | `@Size(min=6, max=50)` |
| `@Min` / `@Max` | 数值范围 | `@Min(1) @Max(100)` |
| `@Email` | 邮箱格式 | `@Email(message = "邮箱格式错误")` |
| `@Pattern` | 正则表达式 | `@Pattern(regexp="^1[3-9]\\d{9}$")` |
| `@Past` / `@Future` | 日期校验 | `@Past(message = "生日必须是过去")` |

---

### 二、审计日志实现

#### 1. 数据库表设计

创建 `operation_log` 表:

```sql
CREATE TABLE `operation_log` (
    `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `user_id` INT COMMENT '操作用户ID',
    `username` VARCHAR(50) COMMENT '操作用户名',
    `user_type` VARCHAR(20) COMMENT '用户类型(admin/consumer)',
    `operation_type` VARCHAR(20) COMMENT '操作类型(LOGIN/LOGOUT/ADD/UPDATE/DELETE/QUERY)',
    `module` VARCHAR(50) COMMENT '操作模块',
    `description` VARCHAR(255) COMMENT '操作描述',
    `request_url` VARCHAR(255) COMMENT '请求URL',
    `request_method` VARCHAR(10) COMMENT '请求方法',
    `request_params` TEXT COMMENT '请求参数',
    `response_result` TEXT COMMENT '响应结果',
    `ip_address` VARCHAR(50) COMMENT 'IP地址',
    `duration` BIGINT COMMENT '操作耗时(ms)',
    `status` VARCHAR(20) COMMENT '状态(SUCCESS/FAILED)',
    `error_msg` TEXT COMMENT '错误信息',
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    INDEX idx_user_id (`user_id`),
    INDEX idx_operation_type (`operation_type`),
    INDEX idx_create_time (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志';
```

#### 2. 核心组件

**① OperationLog 实体类**
- 位置: `entity/po/OperationLog.java`
- 包含所有日志字段

**② OperationLogMapper**
- 位置: `mapper/OperationLogMapper.java`
- 提供CRUD操作

**③ OperationLogService**
- 位置: `service/OperationLogService.java`
- 提供便捷的日志记录方法

**④ OperationLogServiceImpl**
- 位置: `service/impl/OperationLogServiceImpl.java`
- 使用 `@Async` 异步记录,不影响主业务流程

#### 3. 使用方法

**记录登录日志**:
```java
@Autowired
private OperationLogService operationLogService;

// 登录成功
operationLogService.logLogin(userId, username, "admin", ip, true);

// 登录失败
operationLogService.logLogin(null, username, "admin", ip, false);
```

**记录删除操作**:
```java
operationLogService.logDelete(
    userId, 
    username, 
    "admin",
    "歌曲管理",
    "删除歌曲: " + songName,
    ip
);
```

**记录更新操作**:
```java
operationLogService.logUpdate(
    userId,
    username,
    "consumer",
    "个人信息",
    "修改用户资料",
    ip
);
```

#### 4. 日志记录时机

| 操作 | 记录时机 | 示例 |
|------|---------|------|
| **登录** | 认证成功/失败后 | `logLogin()` |
| **登出** | 退出登录后 | `logLogout()` |
| **添加** | 数据插入成功后 | `logAdd()` |
| **更新** | 数据更新成功后 | `logUpdate()` |
| **删除** | 数据删除成功后 | `logDelete()` |
| **查询** | 敏感数据查询 | 手动调用 `logOperation()` |

---

## 🔧 实施步骤

### Step 1: 创建数据库表

执行 Flyway 迁移脚本:
```sql
-- V3__Create_Operation_Log_Table.sql
CREATE TABLE `operation_log` (...);
```

### Step 2: 在 Controller 中添加 @Valid

**AdminController.java**:
```java
@PostMapping("/admin/login/status")
public R<Map<String, Object>> loginStatus(
    @Valid @RequestBody AdminLoginRequest request,
    HttpServletRequest httpRequest
) {
    // 获取IP
    String ip = ipUtils.getIpAddress(httpRequest);
    
    // 验证密码
    if (!adminService.verifyPassword(request.getName(), request.getPassword())) {
        // 记录失败日志
        operationLogService.logLogin(null, request.getName(), "admin", ip, false);
        return R.error("用户名或密码错误");
    }
    
    // 登录成功,记录日志
    Admin admin = adminService.getByName(request.getName());
    operationLogService.logLogin(admin.getId(), admin.getName(), "admin", ip, true);
    
    // 生成Token...
}
```

**ConsumerController.java**:
```java
@PostMapping("/update")
public R updateConsumer(@Valid @RequestBody ConsumerRequest request) {
    // 自动校验,失败由全局异常处理器处理
    
    Integer userId = getUserIdFromToken(); // 从Token获取
    String ip = ipUtils.getIpAddress(request);
    
    // 执行更新
    boolean success = consumerService.update(consumer);
    
    // 记录日志
    if (success) {
        operationLogService.logUpdate(
            userId,
            request.getUsername(),
            "consumer",
            "个人信息",
            "修改用户资料",
            ip
        );
    }
    
    return success ? R.ok("修改成功") : R.error("修改失败");
}

@GetMapping("/delete")
public R deleteConsumer(Integer id) {
    Integer userId = getUserIdFromToken();
    String ip = ipUtils.getIpAddress(request);
    
    Consumer consumer = consumerService.selectByPrimaryKey(id);
    boolean success = consumerService.delete(id);
    
    if (success) {
        operationLogService.logDelete(
            userId,
            null,
            "admin",
            "用户管理",
            "删除用户: " + consumer.getUsername(),
            ip
        );
    }
    
    return success ? R.ok("删除成功") : R.error("删除失败");
}
```

### Step 3: 启用异步支持

在启动类添加 `@EnableAsync`:

```java
@SpringBootApplication
@EnableAsync  // 启用异步支持
public class MusicBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(MusicBackendApplication.class, args);
    }
}
```

### Step 4: 配置线程池(可选)

在 `application.yml` 中配置:

```yaml
spring:
  task:
    execution:
      pool:
        core-size: 5
        max-size: 10
        queue-capacity: 100
        keep-alive: 60s
      thread-name-prefix: async-log-
```

---

## 📊 优势对比

### 参数校验

| 方面 | 改造前 ❌ | 改造后 ✅ |
|------|----------|----------|
| 校验方式 | 手动if判断 | 注解自动校验 |
| 代码量 | 大量重复代码 | 简洁清晰 |
| 可维护性 | 分散在各处 | 集中在DTO |
| 错误提示 | 不统一 | 统一格式 |
| 可扩展性 | 困难 | 容易 |

### 审计日志

| 方面 | 改造前 ❌ | 改造后 ✅ |
|------|----------|----------|
| 操作追溯 | 无法追溯 | 完整记录 |
| 安全审计 | 无 | 支持 |
| 问题排查 | 困难 | 容易 |
| 性能影响 | 无 | 异步无影响 |
| 合规性 | 不符合 | 符合 |

---

## 💡 最佳实践

### 1. 参数校验

✅ **推荐做法**:
```java
// 使用DTO + @Valid
@PostMapping("/add")
public R addSong(@Valid @RequestBody SongRequest request) {
    // 业务逻辑
}
```

❌ **避免做法**:
```java
// 手动校验每个参数
@PostMapping("/add")
public R addSong(String name, String singer, ...) {
    if (name == null || name.isEmpty()) {
        return R.error("名称不能为空");
    }
    // ... 大量重复代码
}
```

### 2. 审计日志

✅ **推荐做法**:
```java
// 异步记录,不影响主流程
@Async
public void logOperation(OperationLog log) {
    operationLogMapper.insert(log);
}

// 关键操作必记录
operationLogService.logDelete(userId, username, "admin", "歌曲管理", "删除歌曲", ip);
```

❌ **避免做法**:
```java
// 同步记录,阻塞主流程
operationLogMapper.insert(log);  // 可能很慢

// 忘记记录重要操作
consumerService.delete(id);  // 没有日志
```

### 3. 日志清理

定期清理过期日志,避免数据膨胀:

```java
@Component
public class LogCleanupTask {
    
    @Autowired
    private OperationLogService logService;
    
    // 每天凌晨2点执行
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanExpiredLogs() {
        logService.cleanExpiredLogs(90);  // 保留90天
    }
}
```

---

## 📁 已创建的文件

| 文件 | 路径 | 说明 |
|------|------|------|
| `AdminLoginRequest.java` | `entity/dto/` | 管理员登录DTO |
| `ConsumerRequest.java` | `entity/dto/` | 用户请求DTO |
| `OperationLog.java` | `entity/po/` | 审计日志实体 |
| `OperationLogMapper.java` | `mapper/` | 日志Mapper接口 |
| `OperationLogService.java` | `service/` | 日志Service接口 |
| `OperationLogServiceImpl.java` | `service/impl/` | 日志Service实现 |
| `PARAM_VALIDATION_AND_AUDIT_LOG.md` | 本文件 | 配置方案文档 |

---

## 🎯 下一步工作

### 待完成:
1. ⏳ 创建 Flyway 迁移脚本 `V3__Create_Operation_Log_Table.sql`
2. ⏳ 创建 `OperationLogMapper.xml`
3. ⏳ 修改 Controller 添加 `@Valid` 注解
4. ⏳ 在登录/删除/修改接口中添加日志记录
5. ⏳ 启用 `@EnableAsync` 异步支持
6. ⏳ 添加日志清理定时任务
7. ⏳ 创建更多 DTO 类(SongRequest, SingerRequest等)

---

📖 **参考文档**:
- [Bean Validation](https://beanvalidation.org/)
- [Spring Validation](https://docs.spring.io/spring-framework/reference/core/validation/validator.html)
- [Spring Async](https://docs.spring.io/spring-framework/reference/integration/scheduling.html)
