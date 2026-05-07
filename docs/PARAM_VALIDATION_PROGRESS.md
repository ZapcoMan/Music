# 参数校验实施进度报告

## 📋 当前状态

### ✅ 已完成

#### 1. DTO 类创建
- [x] `AdminLoginRequest.java` - 管理员登录
- [x] `ConsumerRequest.java` - 用户注册/更新  
- [x] `SingerRequest.java` - 歌手增删改

#### 2. Controller 修改
- [x] `SingerController.java` 
  - ✅ `/add` - 添加 `@Valid @RequestBody SingerRequest`
  - ✅ `/update` - 添加 `@Valid @RequestBody SingerRequest`
  - ❌ `/delete` - GET请求,使用@RequestParam,暂不修改
  - ❌ 查询接口 - 无需校验(正确)

---

### ⏳ 待完成

#### 需要添加校验的 Controller

**1. ConsumerController.java**
- ⏳ `/add` - POST,需添加 `@Valid`
- ⏳ `/update` - POST,需添加 `@Valid` (已有ConsumerRequest)
- ⏳ `/login` - POST,需添加 `@Valid`
- ⏳ `/updateConsumerPic` - POST,文件上传,手动校验即可
- ✅ 查询接口 - 无需校验

**2. SongController.java**
- ⏳ `/add` - POST,需创建SongRequest并添加 `@Valid`
- ⏳ `/update` - POST,需添加 `@Valid`
- ⏳ `/delete` - GET,暂不修改
- ⏳ `/updateSongPic` - POST,文件上传
- ⏳ `/updateSongUrl` - POST,文件上传
- ✅ 查询接口 - 无需校验

**3. SongListController.java**
- ⏳ `/add` - POST,需创建SongListRequest
- ⏳ `/update` - POST,需添加 `@Valid`
- ⏳ `/delete` - GET,暂不修改
- ⏳ `/updateSongListPic` - POST,文件上传
- ✅ 查询接口 - 无需校验

**4. CommentController.java**
- ⏳ `/add` - POST,需创建CommentRequest
- ⏳ `/update` - POST,需添加 `@Valid`
- ⏳ `/delete` - GET,暂不修改
- ✅ 查询接口 - 无需校验

**5. CollectController.java**
- ⏳ `/addCollection` - POST,需创建CollectRequest
- ⏳ `/delete` - GET,暂不修改
- ✅ 查询接口 - 无需校验

**6. AdminController.java**
- ⏳ `/admin/login/status` - POST,需添加 `@Valid` (已有AdminLoginRequest)

**7. ListSongController.java**
- ⏳ `/add` - POST,需创建ListSongRequest
- ⏳ `/update` - POST,需添加 `@Valid`
- ⏳ `/delete` - GET,暂不修改

**8. RankController.java**
- ⏳ `/rank` - POST,需创建RankRequest

---

## 🎯 实施原则

### ✅ 应该添加 @Valid 的情况

1. **POST/PUT 请求** - 增删改操作
2. **@RequestBody 参数** - JSON格式数据
3. **复杂对象** - 多个字段需要校验

示例:
```java
@PostMapping("/add")
public R addSinger(@Valid @RequestBody SingerRequest request) {
    // 自动校验,失败由全局异常处理器处理
}
```

### ❌ 不应该添加 @Valid 的情况

1. **GET 请求** - 查询操作
2. **@RequestParam 参数** - 简单参数
3. **文件上传** - MultipartFile,手动校验
4. **路径变量** - @PathVariable

示例:
```java
// ✅ 查询接口 - 不需要校验
@GetMapping("/selectByPrimaryKey")
public Singer selectByPrimaryKey(Integer id) {
    return singerService.selectByPrimaryKey(id);
}

// ✅ 删除接口 - GET + @RequestParam,暂不修改
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

## 📝 还需要创建的 DTO 类

| DTO 类 | 用途 | 必填字段 |
|--------|------|---------|
| `SongRequest` | 歌曲增删改 | name, singerId, introduction |
| `SongListRequest` | 歌单增删改 | title, introduction, style |
| `CommentRequest` | 评论增删改 | userId, content, type |
| `CollectRequest` | 收藏操作 | userId, songId/songListId, type |
| `ListSongRequest` | 歌单歌曲关联 | songId, songListId |
| `RankRequest` | 评分操作 | songListId, consumerId, score |

---

## 🔧 下一步工作

### Step 1: 创建剩余 DTO 类
```bash
# 需要创建6个DTO类
SongRequest.java
SongListRequest.java
CommentRequest.java
CollectRequest.java
ListSongRequest.java
RankRequest.java
```

### Step 2: 修改剩余 Controller
按优先级修改:
1. **高优先级**: ConsumerController, SongController, SongListController
2. **中优先级**: CommentController, CollectController
3. **低优先级**: ListSongController, RankController

### Step 3: 测试验证
- 测试参数校验是否生效
- 测试错误提示是否正确
- 确保查询接口不受影响

---

## 💡 最佳实践

### 1. DTO 设计原则

✅ **推荐**:
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

❌ **避免**:
```java
// 不要在一个DTO中包含所有场景的字段
@Data
public class SingerRequest {
    // 添加时不需要id
    // 更新时必须id
    private Integer id;  // 混淆!
}
```

### 2. Controller 使用原则

✅ **推荐**:
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

❌ **避免**:
```java
// 不要在查询接口加 @Valid
@GetMapping("/select")
public Singer select(@Valid Integer id) {  // 无意义!
    return service.select(id);
}
```

---

## 📊 进度统计

| 项目 | 数量 | 完成 | 进度 |
|------|------|------|------|
| DTO 类 | 9 | 3 | 33% |
| Controller | 8 | 1 | 12.5% |
| 增删改接口 | ~20 | 2 | 10% |
| 查询接口 | ~30 | 0 | 0% (无需修改) |

---

## 🎯 目标

- ✅ **增删改接口**: 100% 添加 `@Valid` 校验
- ✅ **查询接口**: 保持现状,不添加校验
- ✅ **文件上传**: 保持手动校验
- ✅ **错误提示**: 统一由全局异常处理器处理

---

**更新时间**: 2026-05-07  
**已完成**: SingerController 增删改接口  
**下一步**: 创建剩余6个DTO类并修改ConsumerController
