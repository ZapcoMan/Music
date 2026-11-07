package com.shanzhu.music.common;

/**
 * Redis 键前缀的唯一出处。
 *
 * 收敛前 8 个 music: 前缀分散在 7 个 ServiceImpl 与 RecommendationCache 各自的私有常量里，
 * jwt:blacklist: 更同时硬编码在 JwtAuthenticationInterceptor 与 TokenBlacklistService 两处。
 * 键由读写双方各自拼接时，任何一侧改名都不会报错，只会静默退化为
 * "缓存永远命中不了"或"登出后令牌依然可用"，因此集中到这一处。
 *
 * 前缀取值保持与收敛前逐字节一致：改动取值会让 Redis 中的现有缓存整体失效，
 * 也会让写入方与尚未升级的读取方错键。
 * songlist 与 list_song 两种拼法是历史上就并存的不同域，此处如实保留。
 */
public final class CacheKeys {

    /**
     * 歌曲详情
     */
    public static final String SONG = "music:song:";

    /**
     * 歌手详情
     */
    public static final String SINGER = "music:singer:";

    /**
     * 歌单详情
     */
    public static final String SONG_LIST = "music:songlist:";

    /**
     * 歌单-歌曲关联
     */
    public static final String LIST_SONG = "music:list_song:";

    /**
     * 收藏
     */
    public static final String COLLECT = "music:collect:";

    /**
     * 评论
     */
    public static final String COMMENT = "music:comment:";

    /**
     * 前台用户
     */
    public static final String CONSUMER = "music:consumer:";

    /**
     * 用户推荐结果
     */
    public static final String RECOMMENDATION = "music:recommendation:user:";

    /**
     * 已吊销令牌黑名单
     */
    public static final String TOKEN_BLACKLIST = "jwt:blacklist:";

    private CacheKeys() {
    }
}
