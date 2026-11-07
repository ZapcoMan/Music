# SpringDoc OpenAPI 配置说明

## 概述

项目已成功集成 SpringDoc OpenAPI,用于生成和管理 RESTful API 文档。

## 访问地址

### Swagger UI (可视化界面)
- **地址**: http://localhost:9171/swagger-ui.html
- **说明**: 提供可视化的API文档界面,可以在线测试接口

### OpenAPI JSON (原始文档)
- **地址**: http://localhost:9171/v3/api-docs
- **说明**: 返回OpenAPI 3.0规范的JSON格式文档

## 已添加的依赖

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.5</version>
</dependency>
```

## 配置说明

### application.yml 配置

```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
    enabled: true
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
    tags-sorter: alpha          # 按字母顺序排序标签
    operations-sorter: alpha    # 按字母顺序排序操作
  group-configs:
    - group: 'default'
      paths-to-match: '/**'
      packages-to-scan: com.shanzhu.music.controller
```

### OpenApiConfig 配置类

位置: `com.shanzhu.music.config.OpenApiConfig`

配置了API文档的基本信息:
- 标题: 音乐网站 API 文档
- 描述: 音乐网站后台管理系统 RESTful API 接口文档
- 版本: 取自 `info.app.version`（与 actuator `/info` 同一真源，发布时只需改 application.yml 与 pom、Dockerfile）
- 联系人: ShanZhu
- 许可证: Apache 2.0

## 已添加注解的Controller

### 1. AdminController (管理员管理)
- `@Tag`: 管理员管理
- 主要接口:
  - POST `/admin/login/status` - 管理员登录
  - GET `/manage` - 管理页面

### 2. ConsumerController (前台用户管理)
- `@Tag`: 前台用户管理
- 主要接口:
  - POST `/consumer/add` - 注册用户
  - GET `/consumer/selectByPrimaryKey` - 查询用户详情
  - GET `/consumer/allConsumer` - 查询所有用户
  - GET `/consumer/allConsumerByPage` - 分页查询用户

## 如何使用注解

### 1. Controller级别注解

```java
@Tag(name = "模块名称", description = "模块描述")
@RestController
@RequestMapping("/xxx")
public class XxxController {
    // ...
}
```

### 2. 方法级别注解

```java
@Operation(
    summary = "接口简短描述", 
    description = "接口详细描述"
)
@PostMapping("/add")
public R addXxx(...) {
    // ...
}
```

### 3. 参数级别注解

```java
public R addXxx(
    @Parameter(
        description = "参数描述", 
        required = true, 
        example = "示例值"
    ) String paramName
) {
    // ...
}
```

## 常用注解说明

### @Tag
- **作用**: 对Controller进行分类和分组
- **属性**:
  - `name`: 标签名称
  - `description`: 标签描述

### @Operation
- **作用**: 描述接口的功能和用途
- **属性**:
  - `summary`: 简短描述
  - `description`: 详细描述
  - `tags`: 所属标签(可选)

### @Parameter
- **作用**: 描述接口参数
- **属性**:
  - `description`: 参数描述
  - `required`: 是否必填
  - `example`: 示例值

### @Schema (用于实体类)
- **作用**: 描述数据模型
- **使用位置**: 实体类的字段上

```java
@Schema(description = "用户ID", example = "1")
private Integer id;
```

## 后续工作建议

### 1. 为其他Controller添加注解

需要添加注解的Controller:
- ✅ AdminController
- ✅ ConsumerController  
- ⏳ SingerController (歌手管理)
- ⏳ SongController (歌曲管理)
- ⏳ SongListController (歌单管理)
- ⏳ CollectController (收藏管理)
- ⏳ CommentController (评论管理)
- ⏳ ListSongController (歌单歌曲管理)
- ⏳ RankController (评分管理)

### 2. 为实体类添加@Schema注解

示例:
```java
@Data
@Schema(description = "用户实体")
public class Consumer {
    @Schema(description = "用户ID", example = "1")
    private Integer id;
    
    @Schema(description = "用户名", example = "testuser")
    private String username;
    
    // ...
}
```

### 3. 添加安全配置

如果API需要认证,可以配置:
```yaml
springdoc:
  swagger-ui:
    oauth:
      client-id: your-client-id
```

## 注意事项

1. **生产环境**: 建议在 production profile 中禁用 Swagger UI
   ```yaml
   springdoc:
     swagger-ui:
       enabled: false
     api-docs:
       enabled: false
   ```

2. **性能影响**: OpenAPI文档生成会有一定的性能开销,建议仅在开发和测试环境启用

3. **安全性**: Swagger UI暴露了所有API接口,生产环境应该禁用或添加访问控制

## 参考资源

- [SpringDoc官方文档](https://springdoc.org/)
- [OpenAPI 3.0规范](https://swagger.io/specification/)
- [Swagger UI文档](https://swagger.io/docs/open-source-tools/swagger-ui/)
