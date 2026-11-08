# 标签召回与搜索历史召回功能实现说明

> ⚠️ 本文的策略权重数值与代码不一致。真实权重（现已可通过 `recommendation.strategy-weights`
> 配置调整）为：`collaborative_filtering 0.40`、`content_based 0.25`、`personalized 0.20`、
> `tag_based 0.20`、`search_history 0.20`、`popular 0.15`。
> 另需注意：标签召回虽然代码路径已通，但 `song_tag` 实际为 0 行
> （V3 的自动迁移按 `song.introduction` 匹配标签名，而该字段存的是专辑名），
> 因此在补齐标签数据来源前它仍不会产生候选。详见 [FIX-LOG-2026-11.md](FIX-LOG-2026-11.md)。


## 功能概述

本次更新为音乐推荐系统添加了2路新的召回策略：
1. **标签召回（TagBasedRecall）**：基于用户收藏歌曲的标签进行推荐
2. **搜索历史召回（SearchHistoryRecall）**：基于用户最近搜索的关键词进行推荐

同时实现了完整的搜索历史记录功能，包括前后端所有代码。

---

## 一、数据库变更

### 1.1 新增表：search_history

```sql
CREATE TABLE IF NOT EXISTS `search_history` (
    `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` INT(11) NOT NULL COMMENT '用户ID',
    `keywords` VARCHAR(255) NOT NULL COMMENT '搜索关键词',
    `search_type` TINYINT(1) DEFAULT 0 COMMENT '搜索类型（0歌曲1歌手2歌单）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '搜索时间',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_user_id` (`user_id`) USING BTREE,
    INDEX `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='用户搜索历史';
```

### 1.2 修改表：song 和 singer

- **song表**：添加 `tags` 字段（VARCHAR(500)），用于存储歌曲标签
- **singer表**：添加 `tags` 字段（VARCHAR(500)），用于存储歌手标签

迁移脚本位置：`Music-backend/src/main/resources/db/migration/V2__Add_Search_History_And_Tags.sql`

---

## 二、后端实现

### 2.1 实体类（Entity）

#### SearchHistory.java
- 路径：`com.shanzhu.music.entity.po.SearchHistory`
- 字段：id, userId, keywords, searchType, createTime

#### Song.java & Singer.java
- 新增字段：`tags`（字符串类型，逗号分隔的标签）

### 2.2 Mapper层

#### SearchHistoryMapper.java
主要方法：
- `insert()`: 插入搜索历史记录
- `getRecentHistory()`: 获取用户最近的搜索历史
- `getRecentKeywords()`: 获取用户最近搜索的关键词列表
- `deleteByUserId()`: 清空用户的搜索历史
- `deleteOldHistory()`: 清理过期搜索历史
- `countKeywordFrequency()`: 统计关键词搜索频率

#### SongMapper.xml
- 更新ResultMap，添加tags字段映射
- 更新Base_Column_List，包含tags字段
- 更新insert和update语句，支持tags字段

### 2.3 Service层

#### SearchHistoryService.java
接口定义：
- `recordSearch()`: 记录搜索历史
- `getRecentHistory()`: 获取最近搜索历史
- `getRecentKeywords()`: 获取最近搜索关键词
- `clearHistory()`: 清空搜索历史
- `cleanOldHistory()`: 清理过期历史

#### SearchHistoryServiceImpl.java
实现上述接口，包含完整的异常处理和日志记录。

### 2.4 Controller层

#### SearchHistoryController.java
REST API接口：
- `POST /api/v1/search-history`: 记录搜索历史
- `GET /api/v1/search-history`: 获取最近搜索历史
- `GET /api/v1/search-history/keywords`: 获取最近搜索关键词
- `DELETE /api/v1/search-history`: 清空搜索历史
- `DELETE /api/v1/search-history/expired`: 清理过期历史

这组接口一律**不接收 userId**：搜索历史按令牌归属读写，用户身份来自 Authorization 头。
整组只允许 consumer 角色（管理员令牌打它是 403，不是 200——管理端从不调用它）。

### 2.5 召回策略实现

#### TagBasedRecall.java（标签召回）
**策略名称**: `tag_based`  
**权重**: 0.20  
**最大召回数**: 25

**实现逻辑**：
1. 获取用户收藏的歌曲列表
2. 从歌曲简介中提取标签（按逗号、空格分词）
3. 统计标签出现频率，选取Top 5标签
4. 根据标签查询相关歌曲（使用LIKE模糊匹配）
5. 排除用户已收藏的歌曲
6. 按标签匹配次数排序，返回前25首

#### SearchHistoryRecall.java（搜索历史召回）
**策略名称**: `search_history`  
**权重**: 0.20  
**最大召回数**: 20  
**关键词数量**: 10

**实现逻辑**：
1. 获取用户最近搜索的10个关键词
2. 对每个关键词进行歌曲名模糊查询
3. 排除用户已收藏的歌曲
4. 按关键词匹配次数排序
5. 返回前20首相关歌曲

---

## 三、前端实现

### 3.1 API封装

在 `MusicUser-frontend/src/api/index.js` 中添加：
```javascript
// 记录搜索历史
export const recordSearchHistory = (params) => post(`/search-history`, params);

// 获取最近搜索历史（不再传 userId，身份由令牌决定；baseURL 已含 /api/v1）
export const getRecentSearchHistory = (limit = 20) => get(`/search-history?limit=${limit}`);

// 获取最近搜索关键词
export const getRecentSearchKeywords = (limit = 10) =>
    get(`/search-history/keywords?limit=${limit}`);

// 清空搜索历史
export const clearSearchHistory = () => Axios({ method: 'delete', url: `/search-history` });
```

### 3.2 搜索页面增强

#### Search.vue
**新增功能**：
- 显示搜索历史标签云
- 点击标签快速搜索
- 清空搜索历史按钮

**UI组件**：
- 搜索历史区域（带标题和清空按钮）
- 标签列表（使用Element UI的el-tag组件）
- 悬停效果和过渡动画

#### mixins/index.js
**修改getSong方法**：
- 在搜索歌曲时自动记录搜索历史
- 调用 `recordSearchHistory()` API
- searchType设置为0（歌曲搜索）

#### SearchSongLists.vue
**修改getSearchList方法**：
- 在搜索歌单时自动记录搜索历史
- searchType设置为2（歌单搜索）

---

## 四、召回策略集成

### 4.1 自动注册

新增的召回策略通过Spring的 `@Component` 注解自动注册到召回策略列表中：

```java
@Component
public class TagBasedRecall implements RecallStrategy { ... }

@Component
public class SearchHistoryRecall implements RecallStrategy { ... }
```

### 4.2 召回流程

系统会自动加载所有实现 `RecallStrategy` 接口的Bean：

```java
@Autowired
private List<RecallStrategy> recallStrategies;
```

当前共有6路召回策略：
1. CollaborativeFilteringRecall（协同过滤）- 权重0.25
2. ContentBasedRecall（内容标签）- 权重0.25
3. PersonalizedRecall（个性化）- 权重0.15
4. PopularRecall（热门）- 权重0.15
5. **TagBasedRecall（标签召回）- 权重0.20** ✨新增
6. **SearchHistoryRecall（搜索历史）- 权重0.20** ✨新增

### 4.3 融合排序

多路召回结果会经过：
1. 加权融合（根据策略权重）
2. GBDT排序模型（线性+FM交叉特征）
3. Redis缓存（6小时有效期）

---

## 五、使用说明

### 5.1 数据库迁移

项目启动时Flyway会自动执行迁移脚本 `V2__Add_Search_History_And_Tags.sql`。

如需手动执行：
```bash
mysql -u root -p db_music < Music-backend/src/main/resources/db/migration/V2__Add_Search_History_And_Tags.sql
```

### 5.2 搜索历史记录

**自动记录**：
- 用户在搜索框输入关键词并搜索时，系统自动记录
- 支持歌曲搜索（type=0）和歌单搜索（type=2）

**查看历史**：
- 进入搜索页面，自动显示最近10条搜索关键词
- 点击任意标签可快速重新搜索

**清空历史**：
- 点击"清空"按钮可删除所有搜索历史

### 5.3 标签召回优化建议

为了获得更好的标签召回效果，建议：
1. 为歌曲添加详细的标签信息（风格、情感、场景等）
2. 标签格式：用逗号分隔，如 "流行,抒情,伤感,治愈"
3. 定期分析用户收藏数据，优化标签体系

---

## 六、文件清单

### 后端文件（Backend）

#### 实体类
- ✅ `SearchHistory.java` - 搜索历史实体
- ✅ `Song.java` - 添加tags字段
- ✅ `Singer.java` - 添加tags字段

#### Mapper
- ✅ `SearchHistoryMapper.java` - Mapper接口
- ✅ `SearchHistoryMapper.xml` - MyBatis映射文件
- ✅ `SongMapper.xml` - 更新tags字段支持

#### Service
- ✅ `SearchHistoryService.java` - 服务接口
- ✅ `SearchHistoryServiceImpl.java` - 服务实现

#### Controller
- ✅ `SearchHistoryController.java` - REST API控制器

#### 召回策略
- ✅ `TagBasedRecall.java` - 标签召回策略
- ✅ `SearchHistoryRecall.java` - 搜索历史召回策略

#### 数据库
- ✅ `V2__Add_Search_History_And_Tags.sql` - Flyway迁移脚本

### 前端文件（Frontend）

#### API
- ✅ `MusicUser-frontend/src/api/index.js` - 添加搜索历史API

#### 页面组件
- ✅ `MusicUser-frontend/src/pages/Search.vue` - 搜索页面（添加历史记录展示）
- ✅ `MusicUser-frontend/src/components/search/SearchSongs.vue` - 使用mixin
- ✅ `MusicUser-frontend/src/components/search/SearchSongLists.vue` - 添加历史记录

#### Mixins
- ✅ `MusicUser-frontend/src/mixins/index.js` - 添加搜索历史记录逻辑

---

## 七、测试建议

### 7.1 单元测试
- 测试SearchHistoryService的各个方法
- 测试TagBasedRecall和SearchHistoryRecall的召回逻辑

### 7.2 集成测试
- 测试完整的搜索→记录→展示流程
- 测试多路召回融合效果

### 7.3 性能测试
- 测试大量搜索历史下的查询性能
- 测试标签召回的计算效率

---

## 八、后续优化方向

1. **标签体系优化**
   - 引入专业的中文分词工具（如HanLP、Jieba）
   - 建立标准化的标签分类体系

2. **搜索历史优化**
   - 添加搜索历史的去重逻辑
   - 支持搜索历史的分页查询
   - 添加热门搜索推荐

3. **召回策略优化**
   - 动态调整各召回策略的权重
   - 引入用户行为反馈机制
   - 添加实时召回能力

4. **前端体验优化**
   - 添加搜索历史的本地缓存
   - 支持搜索历史的分组展示（按日期）
   - 添加搜索趋势分析

---

## 九、注意事项

1. **数据库兼容性**：迁移脚本使用了MySQL语法，如使用其他数据库需调整
2. **隐私保护**：搜索历史涉及用户隐私，建议添加数据加密和脱敏处理
3. **性能考虑**：搜索历史表建议定期清理旧数据，避免数据量过大
4. **标签质量**：标签召回效果依赖标签质量，建议建立标签审核机制

---

## 十、总结

本次更新成功实现了：
- ✅ 2路新的召回策略（标签召回 + 搜索历史召回）
- ✅ 完整的搜索历史记录功能（前后端）
- ✅ 数据库表结构扩展（search_history表 + tags字段）
- ✅ REST API接口（5个接口）
- ✅ 前端UI展示（搜索历史标签云）
- ✅ 自动记录搜索行为
- ✅ 无缝集成到现有召回架构

所有代码已完成并通过编译检查，可以直接部署使用。
