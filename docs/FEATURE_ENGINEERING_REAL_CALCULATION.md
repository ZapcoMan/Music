# 特征工程真实计算功能实现说明

> ⚠️ **口径声明**：本文为历史快照，"特征真实计算"方向仍成立，但细节以现状为准：
> 相关代码已重组到 `service/ranking`（FeatureEngineering/GBDT/NewSongBoost）与
> `service/recall` 包；无评分历史时的中性兜底不再是硬编码 4.0，而是配置项
> `recommendation.neutral-rating: 5.0`（1-10 分制取量纲中点，归一化上界为
> `rating-max: 10.0`，SQL 里的 COALESCE(...,4.0) 写法已过时）；特征权重、FM 交叉项、
> 六路召回权重均改为 `application.yml` 的 `recommendation.*` 配置驱动。
> 口径出处见 [RECALL_AND_SEARCH_HISTORY_FEATURE.md](RECALL_AND_SEARCH_HISTORY_FEATURE.md)、
> [FIX-LOG-2026-11.md](FIX-LOG-2026-11.md)。

## 📋 功能概述

将推荐系统排序层中的特征默认值改为**真实计算**，提升推荐算法的准确性和个性化程度。

---

## 🎯 改进内容

### 改进前（使用默认值）

```java
// ❌ 用户平均评分 - 硬编码为4.0
stats.avgRating = 4.0; // 默认值

// ❌ 歌曲被收藏次数 - 硬编码为0
stats.collectCount = 0; // TODO: 实现统计逻辑

// ❌ 歌曲平均评分 - 硬编码为4.0  
stats.avgRating = 4.0; // 默认值

// ❌ 召回策略权重 - 硬编码为0.3
feature.setRecallStrategyWeight(0.3);
feature.setRecallRank(recallRank);
```

### 改进后（真实计算）

```java
// ✅ 用户平均评分 - 从数据库查询
Double avgRating = rankMapper.selectAvgRatingByUserId(userId);
stats.avgRating = avgRating != null ? avgRating : 4.0;

// ✅ 歌曲被收藏次数 - 实时统计
Integer collectCount = collectMapper.countCollectsBySongId(songId);
stats.collectCount = collectCount != null ? collectCount : 0;

// ✅ 歌曲平均评分 - 关联查询
Double avgRating = rankMapper.selectAvgRatingBySongId(songId);
stats.avgRating = avgRating != null ? avgRating : 4.0;

// ✅ 召回策略权重 - 从召回融合服务传递真实值
feature.setRecallStrategyWeight(recallInfo.getRecallWeight());
feature.setRecallRank(recallInfo.getRecallRank());
```

---

## 🔧 技术实现

### 1. 数据库层（Mapper）

#### CollectMapper.java - 新增方法

```java
/**
 * 统计歌曲被收藏的次数
 */
Integer countCollectsBySongId(@Param("songId") Integer songId);

/**
 * 统计歌手的所有歌曲被某用户收藏的总次数
 */
Integer countCollectsBySingerId(@Param("singerId") Integer singerId);
```

#### CollectMapper.xml - SQL实现

```xml
<!-- 统计歌曲被收藏的次数 -->
<select id="countCollectsBySongId" resultType="java.lang.Integer">
    SELECT COUNT(*) FROM collect WHERE song_id = #{songId}
</select>

<!-- 统计歌手的所有歌曲被某用户收藏的总次数 -->
<select id="countCollectsBySingerId" resultType="java.lang.Integer">
    SELECT COUNT(DISTINCT c.id)
    FROM collect c
    JOIN song s ON c.song_id = s.id
    WHERE s.singer_id = #{singerId}
</select>
```

#### RankMapper.java - 新增方法

```java
/**
 * 查询用户的平均评分
 */
Double selectAvgRatingByUserId(@Param("userId") Integer userId);

/**
 * 查询歌曲的平均评分（通过歌单关联）
 */
Double selectAvgRatingBySongId(@Param("songId") Integer songId);
```

#### RankMapper.xml - SQL实现

```xml
<!-- 查询用户的平均评分 -->
<select id="selectAvgRatingByUserId" resultType="java.lang.Double">
    SELECT COALESCE(AVG(score), 4.0)
    FROM ranks
    WHERE consumer_id = #{userId}
</select>

<!-- 查询歌曲的平均评分（通过歌单关联） -->
<select id="selectAvgRatingBySongId" resultType="java.lang.Double">
    SELECT COALESCE(AVG(r.score), 4.0)
    FROM ranks r
    JOIN list_song ls ON r.song_list_id = ls.song_list_id
    WHERE ls.song_id = #{songId}
</select>
```

---

### 2. 召回融合层（RecallFusionService）

#### 新增数据结构

```java
/**
 * 带召回信息的歌曲（用于特征工程）
 */
@Data
@AllArgsConstructor
public static class SongWithRecallInfo {
    private Song song;
    private String recallStrategy;  // 召回策略名称
    private Integer recallRank;      // 召回排名
    private Double recallWeight;     // 召回权重
}
```

#### 新增方法

```java
/**
 * 执行多路召回并融合（带召回信息）
 * 用于特征工程提取
 */
public List<SongWithRecallInfo> fuseRecallWithInfo(Integer userId, int topN) {
    // 1. 检查冷启动用户
    // 2. 执行各路召回策略
    // 3. 融合排序并保留召回信息
    return fusionRankingWithInfo(recallResults, topN);
}

/**
 * 融合排序算法（保留召回信息）
 */
private List<SongWithRecallInfo> fusionRankingWithInfo(
    List<RecallResult> recallResults, int topN) {
    // 记录每首歌曲的最佳召回策略、排名和权重
    // 返回带召回信息的歌曲列表
}
```

---

### 3. 特征工程层（FeatureEngineeringService）

#### 更新calculateUserStats方法

```java
private UserStats calculateUserStats(Integer userId) {
    UserStats stats = new UserStats();
    
    // 用户收藏数量
    List<Collect> collects = collectMapper.collectOfUserId(userId);
    stats.collectCount = collects != null ? collects.size() : 0;
    
    // ✅ 用户平均评分（从ranks表查询）
    Double avgRating = rankMapper.selectAvgRatingByUserId(userId);
    stats.avgRating = avgRating != null ? avgRating : 4.0;
    
    return stats;
}
```

#### 更新calculateSongStats方法

```java
private SongStats calculateSongStats(Integer songId) {
    SongStats stats = new SongStats();
    
    // ✅ 歌曲被收藏次数（实时统计）
    Integer collectCount = collectMapper.countCollectsBySongId(songId);
    stats.collectCount = collectCount != null ? collectCount : 0;
    
    // ✅ 歌曲平均评分（关联查询）
    Double avgRating = rankMapper.selectAvgRatingBySongId(songId);
    stats.avgRating = avgRating != null ? avgRating : 4.0;
    
    return stats;
}
```

#### 新增extractFeaturesWithRecallInfo方法

```java
/**
 * 为召回的歌曲列表提取特征（带召回信息）
 */
public List<SongWithFeature> extractFeaturesWithRecallInfo(
        Integer userId, 
        List<RecallFusionService.SongWithRecallInfo> recallSongsWithInfo) {
    // 预计算用户统计特征
    UserStats userStats = calculateUserStats(userId);
    
    // 为每首歌曲提取特征
    for (RecallFusionService.SongWithRecallInfo swri : recallSongsWithInfo) {
        SongFeature feature = extractSingleSongFeatureWithRecallInfo(
            userId, swri.getSong(), userStats, swri);
        songsWithFeatures.add(new SongWithFeature(swri.getSong(), feature));
    }
    
    return songsWithFeatures;
}
```

#### 新增extractSingleSongFeatureWithRecallInfo方法

```java
/**
 * 提取单首歌曲的特征（带召回信息）
 */
private SongFeature extractSingleSongFeatureWithRecallInfo(
        Integer userId, Song song, UserStats userStats,
        RecallFusionService.SongWithRecallInfo recallInfo) {
    SongFeature feature = new SongFeature();
    
    // 用户特征
    feature.setUserId(userId);
    feature.setUserCollectCount(userStats.collectCount);
    feature.setUserAvgRating(userStats.avgRating);  // ✅ 真实值
    
    // 歌曲特征
    feature.setSongId(song.getId());
    feature.setSingerId(song.getSingerId());
    
    // 歌曲统计特征
    SongStats songStats = calculateSongStats(song.getId());
    feature.setSongCollectCount(songStats.collectCount);  // ✅ 真实值
    feature.setSongAvgRating(songStats.avgRating);        // ✅ 真实值
    
    // 歌曲年龄
    if (song.getCreateTime() != null) {
        LocalDate createDate = song.getCreateTime().toInstant()
            .atZone(ZoneId.systemDefault()).toLocalDate();
        long days = ChronoUnit.DAYS.between(createDate, LocalDate.now());
        feature.setSongAgeDays((int) days);
    } else {
        feature.setSongAgeDays(0);
    }
    
    // 交叉特征
    CrossFeatures crossFeatures = calculateCrossFeatures(userId, song);
    feature.setUserLikedSinger(crossFeatures.userLikedSinger);
    feature.setUserSingerCollectCount(crossFeatures.singerCollectCount);
    
    // ✅ 召回相关特征（使用真实值）
    feature.setRecallStrategyWeight(recallInfo.getRecallWeight());
    feature.setRecallRank(recallInfo.getRecallRank());
    
    log.debug("歌曲 {} 的召回策略: {}, 排名: {}, 权重: {}",
        song.getId(), recallInfo.getRecallStrategy(), 
        recallInfo.getRecallRank(), recallInfo.getRecallWeight());
    
    return feature;
}
```

---

### 4. 排序服务层（RankingService）

#### 更新recommend方法

```java
public List<Song> recommend(Integer userId, int topN) {
    // 第1步：多路召回（召回层）- 获取带召回信息的歌曲
    List<RecallFusionService.SongWithRecallInfo> recallSongsWithInfo = 
        recallFusionService.fuseRecallWithInfo(userId, topN * 3);
    
    // 第2步：特征工程 - 使用带召回信息的方法
    List<FeatureEngineeringService.SongWithFeature> songsWithFeatures = 
        featureEngineeringService.extractFeaturesWithRecallInfo(
            userId, recallSongsWithInfo);
    
    // 第3步：GBDT排序（排序层）
    List<Song> rankedSongs = gbdtRankingService.rank(songsWithFeatures);
    
    // 第4步：截取TopN
    return rankedSongs.stream().limit(topN).toList();
}
```

---

## 📊 特征对比表

| 特征名称 | 改进前 | 改进后 | 数据来源 |
|---------|-------|-------|---------|
| **userAvgRating** | 4.0（默认值） | 真实平均值 | `ranks`表AVG(score) |
| **songCollectCount** | 0（默认值） | 真实收藏数 | `collect`表COUNT(*) |
| **songAvgRating** | 4.0（默认值） | 真实平均值 | `ranks`+`list_song`关联查询 |
| **recallStrategyWeight** | 0.3（固定值） | 真实策略权重 | RecallStrategy.getWeight() |
| **recallRank** | 简单序号 | 真实召回排名 | 融合排序时的实际排名 |

---

## 🚀 性能优化

### 1. 预计算用户统计特征

避免在循环中重复查询用户特征：

```java
// ✅ 预先计算一次，所有歌曲共享
UserStats userStats = calculateUserStats(userId);

for (Song song : recallSongs) {
    // 直接使用预计算的结果
    feature.setUserCollectCount(userStats.collectCount);
    feature.setUserAvgRating(userStats.avgRating);
}
```

### 2. 数据库索引建议

```sql
-- collect表索引
CREATE INDEX idx_collect_song_id ON collect(song_id);
CREATE INDEX idx_collect_user_id ON collect(user_id);

-- ranks表索引
CREATE INDEX idx_ranks_consumer_id ON ranks(consumer_id);
CREATE INDEX idx_ranks_song_list_id ON ranks(song_list_id);

-- list_song表索引
CREATE INDEX idx_list_song_song_id ON list_song(song_id);
CREATE INDEX idx_list_song_song_list_id ON list_song(song_list_id);
```

### 3. 缓存策略（可选）

对于热门歌曲和用户，可以添加Redis缓存：

```java
@Cacheable(value = "songStats", key = "#songId")
public SongStats calculateSongStats(Integer songId) {
    // ...
}

@Cacheable(value = "userStats", key = "#userId")
public UserStats calculateUserStats(Integer userId) {
    // ...
}
```

---

## 🧪 测试建议

### 1. 单元测试

```java
@Test
public void testCalculateUserStats() {
    // 准备测试数据
    Integer userId = 1;
    
    // 执行测试
    UserStats stats = featureEngineeringService.calculateUserStats(userId);
    
    // 验证结果
    assertNotNull(stats);
    assertTrue(stats.collectCount >= 0);
    assertTrue(stats.avgRating >= 0 && stats.avgRating <= 5);
}

@Test
public void testCalculateSongStats() {
    Integer songId = 1;
    
    SongStats stats = featureEngineeringService.calculateSongStats(songId);
    
    assertNotNull(stats);
    assertTrue(stats.collectCount >= 0);
    assertTrue(stats.avgRating >= 0 && stats.avgRating <= 5);
}
```

### 2. 集成测试

```java
@Test
public void testRecommendWithRealFeatures() {
    Integer userId = 1;
    int topN = 10;
    
    List<Song> recommendations = rankingService.recommend(userId, topN);
    
    assertNotNull(recommendations);
    assertEquals(topN, recommendations.size());
    
    // 验证返回的歌曲有合理的多样性
    Set<Integer> singerIds = recommendations.stream()
        .map(Song::getSingerId)
        .collect(Collectors.toSet());
    assertTrue(singerIds.size() > 1); // 不应该全是同一歌手
}
```

---

## 📈 效果评估

### 预期改进

1. **推荐准确性提升**
   - 基于真实用户评分偏好
   - 考虑歌曲真实热度
   - 更精准的召回策略权重

2. **个性化程度增强**
   - 区分活跃用户和非活跃用户
   - 识别高质量歌曲和普通歌曲
   - 反映真实的用户-歌手关系

3. **排序质量优化**
   - GBDT模型使用真实特征训练
   - 特征重要性更符合实际
   - 减少噪声干扰

### 监控指标

- CTR（点击率）变化
- 用户停留时长
- 收藏转化率
- 推荐覆盖率
- 响应时间（确保性能可接受）

---

## ⚠️ 注意事项

### 1. 空值处理

所有数据库查询都做了null检查，提供合理的默认值：

```java
Double avgRating = rankMapper.selectAvgRatingByUserId(userId);
stats.avgRating = avgRating != null ? avgRating : 4.0; // 降级到默认值
```

### 2. 性能影响

- 每次推荐需要额外执行多次SQL查询
- 建议监控响应时间，必要时添加缓存
- 可以考虑批量查询优化

### 3. 数据一致性

- 确保`ranks`表和`collect`表的数据完整性
- 定期清理无效数据
- 考虑异步更新统计数据

---

## 📝 文件清单

### 后端修改文件

#### Mapper层
- ✅ `CollectMapper.java` - 新增2个统计方法
- ✅ `CollectMapper.xml` - 新增2个SQL查询
- ✅ `RankMapper.java` - 新增2个评分查询方法
- ✅ `RankMapper.xml` - 新增2个SQL查询

#### Service层
- ✅ `FeatureEngineeringService.java` - 更新特征计算逻辑
- ✅ `RecallFusionService.java` - 新增带召回信息的融合方法
- ✅ `RankingService.java` - 更新推荐流程

---

## 🎉 总结

本次更新成功将推荐系统排序层的**所有特征默认值**替换为**真实计算**：

- ✅ 用户平均评分 - 从数据库实时查询
- ✅ 歌曲收藏次数 - 实时统计
- ✅ 歌曲平均评分 - 关联查询计算
- ✅ 召回策略权重 - 从召回服务传递真实值
- ✅ 召回排名 - 记录融合排序的实际位置

所有代码已完成并通过编译检查，可以直接部署使用。

**下一步优化建议**：
1. 添加Redis缓存提升性能
2. 建立特征监控和告警机制
3. A/B测试验证推荐效果提升
4. 定期离线训练GBDT模型更新权重
