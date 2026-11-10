# Redis 缓存使用指南

> ⚠️ **口径声明**：本文为早期快照，仅缓存思路（Cache-Aside + `music:` 前缀）仍成立。现状差异：
> 缓存已扩展到歌曲/推荐/榜单/搜索等多个域，键前缀全部收敛到 `CacheKeys` 常量类
> （各 Service 内不再散落 `private static final String`）；写操作改用**延时双删**
> （共享调度池 + afterCommit，见 DOUBLE_DELETE_STRATEGY.md）；Redis 另有两类非缓存
> 用途：限流计数 `rate:*` 与令牌黑名单 `jwt:blacklist:`（测试隔离时前者可清、后者绝不可清）；
> prod 的 `REDIS_PASSWORD` 为必填环境变量，compose 端口不再暴露宿主机。详见
> [REDIS_CACHE_OPTIMIZATION.md](REDIS_CACHE_OPTIMIZATION.md)、[FIX-LOG-2026-11.md](FIX-LOG-2026-11.md)。

## 概述

本项目已集成 Redis 缓存,用于优化热点数据的访问性能。目前以下数据已启用缓存:

- ✅ 歌单列表 (allSongList)
- ✅ 歌单详情 (selectByPrimaryKey)
- ✅ 歌手列表 (allSinger)
- ✅ 歌手详情 (selectByPrimaryKey)

## 配置说明

### 1. Redis 连接配置

在 `application.yml` 中配置 Redis 连接信息:

```yaml
spring:
  data:
    redis:
      host: localhost        # Redis服务器地址
      port: 6379            # Redis端口
      password:             # Redis密码(如果有)
      database: 0           # 使用的数据库索引
      timeout: 3000ms       # 连接超时时间
      lettuce:
        pool:
          max-active: 8     # 连接池最大连接数
          max-wait: -1ms    # 连接池最大阻塞等待时间
          max-idle: 8       # 连接池最大空闲连接数
          min-idle: 0       # 连接池最小空闲连接数
```

### 2. 启动 Redis 服务

确保本地已安装并启动 Redis 服务:

**Windows:**
```bash
redis-server.exe
```

**Linux/Mac:**
```bash
redis-server
```

## 缓存策略

### 缓存键命名规范

采用统一的命名前缀,便于管理和清理:

- 歌单相关: `music:songlist:*`
  - 所有歌单: `music:songlist:all`
  - 单个歌单: `music:songlist:{id}`
  
- 歌手相关: `music:singer:*`
  - 所有歌手: `music:singer:all`
  - 单个歌手: `music:singer:{id}`

### 缓存过期时间

- 默认缓存时间: **24小时**
- 可根据业务需求调整 `CACHE_EXPIRE_HOURS` 常量

### 缓存更新策略

采用 **Cache-Aside Pattern** (旁路缓存模式):

1. **读操作**: 
   - 先查缓存
   - 缓存命中则直接返回
   - 缓存未命中则查数据库,并写入缓存

2. **写操作** (增删改):
   - 先更新数据库
   - 成功后删除相关缓存
   - 下次读取时重新加载到缓存

## 如何为其他 Service 添加缓存

以 `SongService` 为例:

### 1. 注入 RedisUtils

```java
@Service
@RequiredArgsConstructor
public class SongServiceImpl implements SongService {
    
    private final SongMapper songMapper;
    private final RedisUtils redisUtils;  // 注入Redis工具类
    
    private static final String SONG_CACHE_KEY = "music:song:";
    private static final String ALL_SONG_KEY = SONG_CACHE_KEY + "all";
    private static final long CACHE_EXPIRE_HOURS = 24;
}
```

### 2. 查询方法添加缓存

```java
@Override
public Song selectByPrimaryKey(Integer id) {
    String cacheKey = SONG_CACHE_KEY + id;
    
    // 先从缓存获取
    Object cachedData = redisUtils.getCacheObject(cacheKey);
    if (cachedData != null) {
        log.info("从Redis缓存获取歌曲数据，id={}", id);
        return (Song) cachedData;
    }
    
    // 缓存未命中,查数据库
    log.info("从数据库查询歌曲数据，id={}", id);
    Song song = songMapper.selectByPrimaryKey(id);
    
    // 写入缓存
    if (song != null) {
        redisUtils.setCacheObject(cacheKey, song, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
    }
    
    return song;
}
```

### 3. 写操作清除缓存

```java
@Override
public Boolean insert(Song song) {
    Boolean result = songMapper.insert(song) > 0;
    if (result) {
        redisUtils.deleteObject(ALL_SONG_KEY);  // 清除列表缓存
        log.info("新增歌曲，清除缓存");
    }
    return result;
}

@Override
public Boolean update(Song song) {
    Boolean result = songMapper.update(song) > 0;
    if (result) {
        redisUtils.deleteObject(ALL_SONG_KEY);
        redisUtils.deleteObject(SONG_CACHE_KEY + song.getId());
        log.info("更新歌曲，清除缓存");
    }
    return result;
}
```

## RedisUtils 常用方法

```java
// 设置缓存(永久)
redisUtils.setCacheObject(key, value);

// 设置缓存(带过期时间)
redisUtils.setCacheObject(key, value, 24, TimeUnit.HOURS);

// 获取缓存
Object value = redisUtils.getCacheObject(key);

// 删除缓存
redisUtils.deleteObject(key);

// 判断key是否存在
Boolean exists = redisUtils.hasKey(key);

// 设置过期时间
redisUtils.expire(key, 24, TimeUnit.HOURS);

// 获取剩余生存时间(秒)
Long ttl = redisUtils.getExpire(key);
```

## 监控和管理

### 查看缓存数据

使用 Redis CLI:

```bash
# 连接到Redis
redis-cli

# 查看所有key
KEYS music:*

# 查看某个key的值
GET music:songlist:all

# 查看key的剩余生存时间
TTL music:songlist:all

# 删除指定key
DEL music:songlist:all

# 清空当前数据库
FLUSHDB
```

### 缓存命中率监控

通过日志可以观察缓存命中情况:

```
INFO  - 从Redis缓存获取所有歌单数据         # 缓存命中
INFO  - 从数据库查询所有歌单数据             # 缓存未命中
INFO  - 新增歌单，清除缓存                   # 缓存清除
```

## 注意事项

1. **实体类必须实现 Serializable 接口**
   - 已检查: `Song`, `Singer`, `SongList` 等实体类都已实现

2. **缓存穿透问题**
   - 对于不存在的数据,可以考虑缓存空值或采用布隆过滤器

3. **缓存雪崩问题**
   - 可以为不同key设置不同的过期时间,避免集中失效

4. **数据一致性**
   - 写操作后及时清除缓存,保证数据最终一致性

5. **生产环境建议**
   - 使用 Redis 集群提高可用性
   - 配置 Redis 持久化(RDB/AOF)
   - 设置合理的内存淘汰策略
   - 监控 Redis 性能和内存使用情况

## 性能提升预期

根据典型场景估算:

- **歌单列表查询**: 从 ~50ms 降低到 ~5ms (提升 10倍)
- **歌手列表查询**: 从 ~30ms 降低到 ~3ms (提升 10倍)
- **详情页查询**: 从 ~20ms 降低到 ~2ms (提升 10倍)

*实际效果取决于数据量、网络状况和 Redis 配置*
