# 参数校验最佳实践

> ⚠️ **口径声明**：本文为历史快照。其"❌ 不应该添加 @Valid"一节中 `@GetMapping("/delete")`
> 等动词式/GET 删除示例已废除（v3.0.0 全面 RESTful 化，删除走 `DELETE /api/v1/...`）；
> 返回值也已泛型化为 `R<T>`。
>
> **2026-11-16 按源码修正**：入参 DTO 是 **6 个**（不是 9 个）；`@Valid` 只有 **7 处、
> 分布在 4 个 Controller**（不是"8 个 Controller 全部添加"）；校验失败的业务 code 是
> **20002**、字段名是 `message`（不是 `code:400` / `msg`）。
>
> 分页另有 `common/PageLimits.MAX_PAGE_SIZE = 100`，在 **ServiceImpl 层**调
> `PageHelper.startPage` 之前 `clamp`（song/songList/singer/consumer 四处入口全部覆盖）——
> pagehelper 的 `max-page-size` 配置只挂在 `Page.setPageSize()` 上，夹不住显式 `startPage`。
> 校验思路（Bean Validation + @Valid + 全局异常处理器）仍与现状一致。总体口径见
> [FIX-LOG-2026-11.md](FIX-LOG-2026-11.md)、[CHANGELOG.md](../CHANGELOG.md)。

## 📋 概述

项目已实现基于 **Bean Validation** 的参数校验系统，包括：
- ✅ `entity/dto/` 下 **6 个入参 Request 类**承载校验注解（另有 `RecallResult`/`SongFeature`
  两个内部计算载体，不接请求）
- ⚠️ 注解式自动校验（`@Valid`）**只标注在 7 处入参上，不是全部增删改接口**——
  逐接口清单见 [API 开发文档](../API%20开发文档.md) 的"校验规则"节
- ✅ 统一错误提示格式（`R<T>`，字段名 `message`）
- ✅ 全局异常处理器

---

## 🔧 核心功能

### 1. DTO 类设计

已创建以下DTO类用于参数校验：

| DTO 类 | 用途 | 主要校验规则 |
|--------|------|------------|
| `AdminLoginRequest` | 管理员登录 | @NotBlank, @Size |
| `ConsumerRequest` | 用户注册/更新 | @NotBlank, @Size, @Email, @Pattern |
| `SingerRequest` | 歌手增删改 | @NotBlank, @NotNull, @Size |
| `SongRequest` | 歌曲增删改 | @NotBlank, @NotNull, @Size |
| `SongListRequest` | 歌单增删改 | @NotBlank, @Size |
| `CommentRequest` | 评论增删改 | @NotBlank, @NotNull, @Size |
| `CollectRequest` | 收藏操作 | @NotNull |
| `ListSongRequest` | 歌单歌曲关联 | @NotNull |
| `RankRequest` | 评分操作 | @NotNull, @Min, @Max |

### 2. 常用校验注解

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

## 📝 使用方法

### 1. 定义 DTO 类

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

### 2. Controller 中使用 @Valid

#### 修改前（手动校验）
```java
@PostMapping("/add")
public R addConsumer(String username, String password, String phoneNum) {
    // 手动校验每个参数
    if (username == null || username.isEmpty()) {
        return R.error("用户名不能为空");
    }
    if (password == null || password.length() < 6) {
        return R.error("密码长度至少6位");
    }
    // ... 大量重复代码
    
    // 手动组装对象
    Consumer consumer = new Consumer();
    consumer.setUsername(username);
    consumer.setPassword(password);
    // ...
}
```

#### 修改后（自动校验）
```java
@PostMapping("/add")
public R addConsumer(@Valid @RequestBody ConsumerRequest request) {
    // 自动校验，失败由全局异常处理器处理
    
    // 简洁组装
    Consumer consumer = new Consumer();
    consumer.setUsername(request.getUsername());
    consumer.setPassword(request.getPassword());
    // ...
}
```

**优势**：
- ✅ 代码量减少 60%+
- ✅ 校验逻辑集中管理
- ✅ 错误提示统一格式
- ✅ 易于维护和扩展

---

## 🎯 实施原则

### ✅ 应该添加 @Valid 的情况

1. **POST/PUT 请求** - 增删改操作
2. **@RequestBody 参数** - JSON格式数据
3. **复杂对象** - 多个字段需要校验

```java
@PostMapping("/add")
public R addSinger(@Valid @RequestBody SingerRequest request) {
    // 自动校验，失败由全局异常处理器处理
}
```

### ❌ 不应该添加 @Valid 的情况

1. **GET 请求** - 查询操作
2. **@RequestParam 参数** - 简单参数
3. **文件上传** - MultipartFile，手动校验
4. **路径变量** - @PathVariable

```java
// ✅ 查询接口 - 不需要校验
@GetMapping("/selectByPrimaryKey")
public Singer selectByPrimaryKey(Integer id) {
    return singerService.selectByPrimaryKey(id);
}

// ✅ 删除接口 - GET + @RequestParam，暂不修改
@GetMapping("/delete")
public Boolean deleteSinger(Integer id) {
    return singerService.delete(id);
}

// ✅ 文件上传 - 手动校验
@PostMapping("/updateSingerPic")
public R updateSingerPic(@RequestParam("file") MultipartFile file, 
                         @RequestParam("id") Integer id) {
    if (file.isEmpty()) {
        return R.error("文件不能为空");
    }
    // ...
}
```

---

## 💡 最佳实践

### 1. DTO 设计原则

✅ **推荐**：
```java
@Data
public class SingerRequest {
    @NotBlank(message = "名称不能为空")
    private String name;
    
    @NotNull(message = "性别不能为空")
    private Byte sex;
    
    @Size(max = 500, message = "简介不能超过500字")
    private String introduction;
}
```

❌ **避免**：
```java
// 不要在一个DTO中包含所有场景的字段
@Data
public class SingerRequest {
    // 添加时不需要id
    // 更新时必须id
    private Integer id;  // 混淆！
}
```

### 2. Controller 使用原则

✅ **推荐**：
```java
// 增删改用 @Valid + @RequestBody
@PostMapping("/add")
public R add(@Valid @RequestBody SingerRequest request) {
    // 业务逻辑
}

// 查询用简单参数
@GetMapping("/select")
public Singer select(Integer id) {
    return service.select(id);
}
```

❌ **避免**：
```java
// 不要在查询接口加 @Valid
@GetMapping("/select")
public Singer select(@Valid Integer id) {  // 无意义！
    return service.select(id);
}
```

### 3. 错误处理

全局异常处理器自动捕获校验失败：

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public R handleValidationException(MethodArgumentNotValidException e) {
    String message = e.getBindingResult().getFieldError().getDefaultMessage();
    return R.error(message);
}
```

---

## 📊 统计汇总

### DTO 类
- **总数**：9个
- **已完成**：9个
- **完成率**：100% ✅

### Controller
- **总数**：8个
- **已完成**：8个（全部增删改接口）
- **完成率**：100% ✅

### 增删改接口
- **总数**：~20个
- **已完成**：~20个
- **完成率**：100% ✅

### 查询接口
- **总数**：~30个
- **需要修改**：0个
- **说明**：查询接口不需要校验，保持现状 ✅

---

## 🚨 常见问题

### Q1: 校验失败后如何获取错误信息？

**A**: 全局异常处理器统一处理，返回格式：
```json
{
  "success": false,
  "code": 20002,
  "message": "用户名长度必须在2-50之间"
}
```

HTTP 状态码同时为 **400**（三个校验异常处理方法都标了 `@ResponseStatus(HttpStatus.BAD_REQUEST)`），
业务 code 取 `ResultCodeEnum.PARAM_ERROR.getCode()` = **20002**。
字段名是 `message` 不是 `msg`。

`MethodArgumentNotValidException`（`@RequestBody` + `@Valid`）、`BindException`（表单绑定 + `@Valid`）、
`ConstraintViolationException` 三个分支返回的 code 一致，不存在"参数错误有两个 code 来源"的情况。

> 注意区分：`BusinessException` 走的是另一条路——HTTP **200** + code **400**（默认）
> 或 500（存储不可用），因为两套前端判成功只看 `res.code == 20000 || res.success === true`，
> 写成 4xx 会让 axios 走 reject 分支而看不到 message。

### Q2: 如何自定义校验规则？

**A**: 创建自定义注解：
```java
@Target({FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = PhoneValidator.class)
public @interface ValidPhone {
    String message() default "手机号格式不正确";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

### Q3: 分组校验如何使用？

**A**: 使用 groups 属性：
```java
public interface AddGroup {}
public interface UpdateGroup {}

@Data
public class SingerRequest {
    @NotNull(groups = UpdateGroup.class)
    private Integer id;
    
    @NotBlank(groups = {AddGroup.class, UpdateGroup.class})
    private String name;
}

// Controller
@PostMapping("/add")
public R add(@Validated(AddGroup.class) @RequestBody SingerRequest request) {
    // ...
}

@PutMapping("/update")
public R update(@Validated(UpdateGroup.class) @RequestBody SingerRequest request) {
    // ...
}
```

---

## 📁 已创建的文件

### DTO 类（9个）
```
entity/dto/
├── AdminLoginRequest.java      (21行)
├── ConsumerRequest.java        (39行)
├── SingerRequest.java          (31行)
├── SongRequest.java            (28行)
├── SongListRequest.java        (25行)
├── CommentRequest.java         (30行)
├── CollectRequest.java         (24行)
├── ListSongRequest.java        (20行)
└── RankRequest.java            (25行)
```

### 修改的 Controller（8个）
```
controller/
├── AdminController.java
├── ConsumerController.java
├── SingerController.java
├── SongController.java
├── SongListController.java
├── CommentController.java
├── CollectController.java
└── ListSongController.java
```

---

## 🎯 总结

### 现状（按源码核对）
✅ 6 个入参 Request DTO 承载校验注解  
⚠️ `@Valid` 实际只有 **7 处**，分布在 **4 个 Controller**：  
　　ConsumerController 3（registrations / profile / {id}，均 `@RequestBody`）  
　　SingerController 2（POST / PATCH，均 `@RequestBody`）  
　　CommentController 1（POST，表单绑定）  
　　RankController 1（POST，表单绑定）  
❌ SongController / SongListController / CollectController / ListSongController /
　　TagController / SearchHistoryController **无 `@Valid`**：上传类由 `FileStorageService`
　　校验并抛 `BusinessException`，其余依赖 Service 层判断与数据库约束  
✅ 统一错误提示格式（`R<T>`，code 20002 + HTTP 400）  
✅ 全局异常处理器覆盖校验、业务、404/405/413/415/500 等分支  

### 优势
🚀 校验逻辑集中在 DTO，不散落在 Controller 的 if 里  
🚀 错误提示统一格式，前端可直接展示 `message`  
🚀 约束即文档：`message` 文案就是给用户看的提示  

> "代码量减少 60%+" 一类量化收益没有实测出处，本文不给数字。

---

## 📖 参考文档

- [Bean Validation 规范](https://beanvalidation.org/)
- [Spring Validation](https://docs.spring.io/spring-framework/reference/core/validation/validator.html)
- [Jakarta Validation API](https://jakarta.ee/specifications/bean-validation/)
