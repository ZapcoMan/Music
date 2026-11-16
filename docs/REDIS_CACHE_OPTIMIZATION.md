# Redis 缓存全面优化总结

> ⚠️ **口径声明**：本文为历史总结，各 Service 已接缓存这一结论仍成立，但正文代码
> 是旧实现：键前缀现已收敛到 `CacheKeys` 常量类（含 `music:` 业务缓存 8 个前缀与
> `jwt:blacklist:`）；高频写服务的失效策略升级为**延时双删**（共享调度池 + afterCommit，
> 见 DOUBLE_DELETE_STRATEGY.md）；登录口令校验始终问数据库，不依赖可能返回陈旧哈希的
> 缓存路径；测试基类按前缀隔离（清 `music:*`/`rate:*`，绝不动 `jwt:blacklist:*`）。
> 具体键名与 TTL 以代码为准；总体口径见 [FIX-LOG-2026-11.md](FIX-LOG-2026-11.md)、
> [CHANGELOG.md](../CHANGELOG.md)。

## 📊 高频数据分析与缓存策略

通过对项目代码的全面分析,已识别所有高频查询数据并统一添加 Redis 缓存。

---

## ✅ 已完成缓存优化的服务

### 1. **SingerService** (歌手服务) ⭐⭐⭐
**高频场景**: 首页展示、歌手列表页、歌手详情页

#### 缓存内容:
- ✅ **所有歌手列表** - `music:singer:all` (24小时)
- ✅ **歌手详情(按ID)** - `music:singer:{id}` (24小时)

#### 缓存策略:
```java
// 查询时: 先查缓存,未命中再查数据库并写入缓存
public List<Singer> allSinger() {
    Object cached = redisUtils.getCacheObject("music:singer:all");
    if (cached != null) return (List<Singer>) cached;
    
    List<Singer> singers = singerMapper.allSinger();
    redisUtils.setCacheObject("music:singer:all", singers, 24, HOURS);
    return singers;
}

// 增删改时: 清除相关缓存
public Boolean update(Singer singer) {
    Boolean result = singerMapper.update(singer) > 0;
    if (result) {
        redisUtils.deleteObject("music:singer:all");
        redisUtils.deleteObject("music:singer:" + singer.getId());
    }
    return result;
}
```

---

### 2. **SongListService** (歌单服务) ⭐⭐⭐
**高频场景**: 首页推荐、歌单列表页、歌单详情页

#### 缓存内容:
- ✅ **所有歌单列表** - `music:songlist:all` (24小时)
- ✅ **歌单详情(按ID)** - `music:songlist:{id}` (24小时)

#### 缓存策略:
```java
// 查询歌单列表
public List<SongList> allSongList() {
    Object cached = redisUtils.getCacheObject("music:songlist:all");
    if (cached != null) return (List<SongList>) cached;
    
    List<SongList> songLists = songListMapper.allSongList();
    redisUtils.setCacheObject("music:songlist:all", songLists, 24, HOURS);
    return songLists;
}

// 更新时清除缓存
public Boolean update(SongList songList) {
    Boolean result = songListMapper.update(songList) > 0;
    if (result) {
        redisUtils.deleteObject("music:songlist:all");
        redisUtils.deleteObject("music:songlist:" + songList.getId());
    }
    return result;
}
```

---

### 3. **SongService** (歌曲服务) ⭐⭐⭐⭐⭐ (最高频)
**高频场景**: 播放歌曲、歌曲列表、歌手歌曲列表

#### 缓存内容:
- ✅ **所有歌曲列表** - `music:song:all` (24小时)
- ✅ **歌曲详情(按ID)** - `music:song:{id}` (24小时)
- ✅ **歌手歌曲列表** - `music:song:singer:{singerId}` (24小时)

#### 缓存策略:
```java
// 查询歌曲详情
public Song selectByPrimaryKey(Integer id) {
    String cacheKey = "music:song:" + id;
    Object cached = redisUtils.getCacheObject(cacheKey);
    if (cached != null) return (Song) cached;
    
    Song song = songMapper.selectByPrimaryKey(id);
    redisUtils.setCacheObject(cacheKey, song, 24, HOURS);
    return song;
}

// 查询歌手的所有歌曲
public List<Song> songOfSingerId(Integer singerId) {
    String cacheKey = "music:song:singer:" + singerId;
    Object cached = redisUtils.getCacheObject(cacheKey);
    if (cached != null) return (List<Song>) cached;
    
    List<Song> songs = songMapper.songOfSingerId(singerId);
    redisUtils.setCacheObject(cacheKey, songs, 24, HOURS);
    return songs;
}

// 更新时清除多个缓存
public Boolean update(Song song) {
    Boolean result = songMapper.update(song) > 0;
    if (result) {
        redisUtils.deleteObject("music:song:all");
        redisUtils.deleteObject("music:song:" + song.getId());
        redisUtils.deleteObject("music:song:singer:" + song.getSingerId());
    }
    return result;
}
```

---

### 4. **ListSongService** (歌单内歌曲服务) ⭐⭐⭐⭐
**高频场景**: 查看歌单内所有歌曲、播放歌单

#### 缓存内容:
- ✅ **歌单内歌曲列表** - `music:list_song:{songListId}` (24小时)

#### 缓存策略:
```java
// 查询歌单内的所有歌曲
public List<ListSong> listSongOfSongListId(Integer songListId) {
    String cacheKey = "music:list_song:" + songListId;
    Object cached = redisUtils.getCacheObject(cacheKey);
    if (cached != null) return (List<ListSong>) cached;
    
    List<ListSong> listSongs = listSongMapper.listSongOfSongListId(songListId);
    redisUtils.setCacheObject(cacheKey, listSongs, 24, HOURS);
    return listSongs;
}

// 添加/删除歌曲时清除缓存
public Boolean insert(ListSong listSong) {
    Boolean result = listSongMapper.insert(listSong) > 0;
    if (result) {
        redisUtils.deleteObject("music:list_song:" + listSong.getSongListId());
    }
    return result;
}
```

---

### 5. **ConsumerService** (前台用户服务) ⭐⭐
**高频场景**: 用户登录、用户信息查询

#### 缓存内容:
- ✅ **用户详情(按ID)** - `music:consumer:{id}` (24小时)
- ✅ **用户详情(按用户名)** - `music:consumer:username:{username}` (24小时)

#### 缓存策略:
```java
// 根据ID查询用户
public Consumer selectByPrimaryKey(Integer id) {
    String cacheKey = "music:consumer:" + id;
    Object cached = redisUtils.getCacheObject(cacheKey);
    if (cached != null) return (Consumer) cached;
    
    Consumer consumer = consumerMapper.selectByPrimaryKey(id);
    redisUtils.setCacheObject(cacheKey, consumer, 24, HOURS);
    return consumer;
}

// 根据用户名查询用户(用于登录验证)
public Consumer getByUsername(String username) {
    String cacheKey = "music:consumer:username:" + username;
    Object cached = redisUtils.getCacheObject(cacheKey);
    if (cached != null) return (Consumer) cached;
    
    Consumer consumer = consumerMapper.getByUsername(username);
    redisUtils.setCacheObject(cacheKey, consumer, 24, HOURS);
    return consumer;
}

// 删除时清除两个缓存
public Boolean delete(Integer id) {
    Consumer consumer = consumerMapper.selectByPrimaryKey(id);
    Boolean result = consumerMapper.delete(id) > 0;
    if (result && consumer != null) {
        redisUtils.deleteObject("music:consumer:" + id);
        redisUtils.deleteObject("music:consumer:username:" + consumer.getUsername());
    }
    return result;
}
```

---

### 6. **CommentService** (评论服务) ⭐⭐
**高频场景**: 查看歌曲评论、查看歌单评论

#### 缓存内容:
- ✅ **歌曲评论列表** - `music:comment:song:{songId}` (2小时)
- ✅ **歌单评论列表** - `music:comment:songlist:{songListId}` (2小时)

#### 缓存策略:
```java
// 查询歌曲评论(评论变化频繁,缓存时间较短)
public List<Comment> commentOfSongId(Integer songId) {
    String cacheKey = "music:comment:song:" + songId;
    Object cached = redisUtils.getCacheObject(cacheKey);
    if (cached != null) return (List<Comment>) cached;
    
    List<Comment> comments = commentMapper.commentOfSongId(songId);
    redisUtils.setCacheObject(cacheKey, comments, 2, HOURS);
    return comments;
}

// 新增/删除评论时清除缓存
public Boolean insert(Comment comment) {
    Boolean result = commentMapper.insert(comment) > 0;
    if (result) {
        if (comment.getSongId() != null) {
            redisUtils.deleteObject("music:comment:song:" + comment.getSongId());
        }
        if (comment.getSongListId() != null) {
            redisUtils.deleteObject("music:comment:songlist:" + comment.getSongListId());
        }
    }
    return result;
}
```

---

### 7. **CollectService** (收藏服务) ⭐
**高频场景**: 查看用户收藏列表

#### 缓存内容:
- ✅ **用户收藏列表** - `music:collect:user:{userId}` (12小时)

#### 缓存策略:
```java
// 查询用户收藏列表
public List<Collect> collectOfUserId(Integer userId) {
    String cacheKey = "music:collect:user:" + userId;
    Object cached = redisUtils.getCacheObject(cacheKey);
    if (cached != null) return (List<Collect>) cached;
    
    List<Collect> collects = collectMapper.collectOfUserId(userId);
    redisUtils.setCacheObject(cacheKey, collects, 12, HOURS);
    return collects;
}

// 添加/删除收藏时清除缓存
public Boolean insert(Collect collect) {
    Boolean result = collectMapper.insert(collect) > 0;
    if (result) {
        redisUtils.deleteObject("music:collect:user:" + collect.getUserId());
    }
    return result;
}
```

---

## 📈 性能收益：无实测数据

> ⚠️ **2026-11-16 修正**：本节此前给过一张"缓存命中率预估"表（歌曲详情 95%+、
> 歌手列表 90%+、响应时间下降 80-90% 等）与"数据库压力减少 80%+"的断言。
> **这些数字没有任何实测或压测出处**，是按访问频率主观填的，已全部删除。
> 命中率取决于真实流量分布，不实测就无法知道；把它们写成表格容易被后人当基线引用。

要拿到真实数字，需要自行埋点或压测：

- **命中率**：`RedisUtils` 目前没有命中/未命中计数，需先在读取分支加计数（或接 Micrometer），
  再按 key 前缀统计。`/actuator/metrics` 需有效 JWT，prod 档默认不对匿名开放。
- **响应时间**：`RequestResponseLoggingInterceptor` 已记录每请求耗时（dev 档 INFO 落盘），
  可对同一接口在缓存冷/热两种状态下各跑一轮对比。
- **数据库压力**：对比开启缓存前后的 MySQL `Questions`/`Com_select` 增量。

### 已实现的一致性机制（这些有代码出处）

| 机制 | 实现位置 | 作用 |
|------|---------|------|
| 延时双删 | `RedisUtils.deleteWithDelay`，7 个 ServiceImpl 写路径 | 删除后延迟再删一次，覆盖并发读回填旧值 |
| TTL 随机抖动 | `RecommendationCache`：360 + 随机 0~30 分钟 | 避免同批用户缓存同时失效造成请求洪峰 |
| 空结果短 TTL 占位 | `RecommendationCache`：空结果 5 分钟 | 挡住反复空跑全链路，又不让新行为长期不可见 |
| 键前缀单一出处 | `common/CacheKeys` | 读写双方共用常量，改键不会静默错键 |
| 口令校验绕开缓存 | 登录路径始终问数据库 | 避免陈旧哈希导致改密后旧口令仍可登录 |

> 因此"缓存雪崩预防——设置随机过期时间"**不再是待办项**，推荐缓存已实现；
> 其余业务键仍是固定 TTL，若要扩展抖动需逐个 ServiceImpl 改。

---

## 🔧 技术实现细节

### 1. Redis 配置
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      timeout: 3000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
```

### 2. 序列化方式
- **Key**: StringRedisSerializer (字符串)
- **Value**: Jackson2JsonRedisSerializer (JSON格式)
- **优点**: 可读性好,便于调试和管理

### 3. 缓存过期策略
- **基础数据**(歌手/歌单/歌曲): 24小时
- **用户数据**(用户信息): 24小时
- **收藏数据**: 12小时
- **评论数据**: 2小时(变化频繁)

### 4. 缓存一致性保证
- **写操作时清除相关缓存**
- **下次读取时自动重建缓存**
- **采用 Cache-Aside Pattern (旁路缓存模式)**

---

## 📝 使用建议

### 监控指标:
1. **Redis 内存使用情况** - 定期检查内存占用
2. **缓存命中率** - 通过日志统计缓存命中次数
3. **响应时间** - 对比缓存前后的接口响应时间

### 优化方向:
1. **热点数据预热** - 应用启动时预加载热门数据
2. **缓存分层** - 本地缓存(Caffeine) + Redis 二级缓存
3. **缓存穿透保护** - 使用布隆过滤器防止查询不存在的数据
4. **缓存雪崩预防** - 设置随机过期时间避免集中失效

---

## 🎯 总结

### 已完成的优化:
✅ **7 个 ServiceImpl** 全部接缓存（另加 `RecommendationCache` 一个写入方）  
✅ **15 个业务缓存键形态** + `jwt:blacklist:{token}`，覆盖高频查询  
✅ **延时双删**确保数据一致性  
✅ **分层 TTL**：常规 24 小时、收藏 12 小时、评论 2 小时、推荐 6 小时带抖动  

> 键与 TTL 的逐项清单见 [API 开发文档](../API%20开发文档.md) 的"Redis 缓存"节，
> 本文不再重复维护，避免两处数字漂移。

### 收益
🚀 重复查询命中缓存后不再落库，机制成立  
⚠️ **具体倍数与百分比无实测数据，本文不给数字**（原"提升 5-10 倍 / 压力降低 80%+ /
并发提升 3-5 倍"已删除，理由见上文"性能收益：无实测数据"）

---

## 📌 注意事项

1. **首次启动**: 确保 Redis 服务已启动
2. **缓存预热**: 可在应用启动时预加载热点数据
3. **监控告警**: 建议添加 Redis 连接失败告警
4. **定期清理**: 监控 Redis 内存,必要时调整过期时间
