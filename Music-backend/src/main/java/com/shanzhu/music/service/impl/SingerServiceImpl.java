package com.shanzhu.music.service.impl;
import com.shanzhu.music.common.CacheKeys;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.shanzhu.music.entity.dto.SingerRequest;
import com.shanzhu.music.entity.po.Singer;
import com.shanzhu.music.mapper.SingerMapper;
import com.shanzhu.music.entity.po.Song;
import com.shanzhu.music.service.SingerService;
import com.shanzhu.music.service.SongService;
import com.shanzhu.music.service.TagService;
import com.shanzhu.music.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 歌手 服务层实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SingerServiceImpl implements SingerService {

    private final SingerMapper singerMapper;
    private final SongService songService;
    private final TagService tagService;
    private final RedisUtils redisUtils;
    
    // Redis缓存键前缀
    private static final String SINGER_CACHE_KEY = CacheKeys.SINGER;
    private static final String ALL_SINGER_KEY = SINGER_CACHE_KEY + "all";
    private static final long CACHE_EXPIRE_HOURS = 24; // 缓存过期时间24小时

    /**
     * 添加歌手（延时双删缓存）
     * @param singer 歌手信息
     * @return 结果
     */
    @Override
    public Boolean insert(Singer singer) {
        // 与歌曲侧同一套约定：tags 为 null 表示不改动，规范化后再落库，
        // 中间表由 tagService 重建，两侧不会各写各的
        if (singer.getTags() != null) {
            singer.setTags(tagService.normalizeTags(singer.getTags()));
        }
        Boolean result = singerMapper.insert(singer) > 0;
        if (result && singer.getTags() != null) {
            tagService.replaceSingerTags(singer.getId(), singer.getTags());
        }
        if (result) {
            // 使用延时双删策略
            redisUtils.deleteWithDelay(ALL_SINGER_KEY);
            log.info("新增歌手，延时双删缓存");
        }
        return result;
    }

    /**
     * 修改歌手（延时双删缓存）
     * @param singer 歌手信息
     * @return 结果
     */
    @Override
    public Boolean update(Singer singer) {
        if (singer.getTags() != null) {
            singer.setTags(tagService.normalizeTags(singer.getTags()));
        }
        Boolean result = singerMapper.update(singer) > 0;
        if (result && singer.getTags() != null) {
            tagService.replaceSingerTags(singer.getId(), singer.getTags());
        }
        if (result) {
            // 使用延时双删策略
            redisUtils.deleteWithDelay(ALL_SINGER_KEY);
            redisUtils.deleteWithDelay(SINGER_CACHE_KEY + singer.getId());
            log.info("更新歌手，延时双删缓存");
        }
        return result;
    }

    /**
     * 添加歌手（请求入口）
     *
     * @param request 歌手信息
     * @return 结果
     */
    @Override
    public Boolean insert(SingerRequest request) {
        Singer singer = toSinger(request);
        // 头像由上传接口单独维护，新增时占空串，与原 Controller 行为一致
        singer.setPic("");
        return insert(singer);
    }

    /**
     * 修改歌手（请求入口）
     *
     * @param id      歌手id，以 URL 指向的资源为准
     * @param request 歌手信息
     * @return 结果
     */
    @Override
    public Boolean update(Integer id, SingerRequest request) {
        Singer singer = toSinger(request);
        singer.setId(id);
        return update(singer);
    }

    /**
     * 请求 DTO 转持久层对象。
     *
     * 库里 birth 是 date，前端传 yyyy-MM-dd 字符串。这段转换原先在 SingerController 的
     * add 与 update 里各写一遍（ConsumerController 还有同款两份），漏改一处就会出现
     * "新增存得下生日、修改把生日丢掉"这种半边生效的差异；标签不在请求里，
     * 因此 tags 保持 null，交给 PO 方法按"不改动"处理。
     */
    private Singer toSinger(SingerRequest request) {
        Singer singer = new Singer();
        singer.setName(request.getName());
        singer.setSex(request.getSex());
        singer.setBirth(parseBirth(request.getBirth()));
        singer.setLocation(request.getLocation());
        singer.setIntroduction(request.getIntroduction());
        return singer;
    }

    /**
     * 前端生日字符串转 Date；空串与 null 一律按"未填写"处理成 null。
     */
    private static Date parseBirth(String birth) {
        return StrUtil.isBlank(birth) ? null : DateUtil.parse(birth, DatePattern.NORM_DATE_PATTERN);
    }

    /**
     * 删除歌手（延时双删缓存）
     *
     * @param id 歌手id
     * @return 删除结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean delete(Integer id) {
        // song.singer_id 上没有外键，此前删歌手只删 singer 一行，
        // 结果是留下一批指向不存在歌手的孤儿歌曲：列表页显示空白歌手名，
        // 召回与统计也继续把它们算在内。
        // 级联清理改为一次批量调用（原来是 for 循环逐首 songService.delete，
        // N 首歌产生 5N 条 SQL 且全在同一事务内）。song_tag 与 singer_tag 各有
        // ON DELETE CASCADE，标签中间表由数据库负责，应用层不重复处理。
        int removedSongs = songService.deleteBySingerId(id);

        Boolean result = singerMapper.delete(id) > 0;
        if (result) {
            // 使用延时双删策略
            redisUtils.deleteWithDelay(ALL_SINGER_KEY);
            redisUtils.deleteWithDelay(SINGER_CACHE_KEY + id);
            log.info("删除歌手 id={}, 连带删除 {} 首歌曲及其歌单关联、收藏与评论，延时双删缓存",
                    id, removedSongs);
        }
        return result;
    }

    /**
     * 查询歌手（带缓存）
     *
     * @param id 歌手id
     * @return 歌手
     */
    @Override
    public Singer selectByPrimaryKey(Integer id) {
        String cacheKey = SINGER_CACHE_KEY + id;
        
        Object cachedData = redisUtils.getCacheObject(cacheKey);
        if (cachedData != null) {
            log.info("从Redis缓存获取歌手数据，id={}", id);
            return (Singer) cachedData;
        }
        
        log.info("从数据库查询歌手数据，id={}", id);
        Singer singer = singerMapper.selectByPrimaryKey(id);
        
        if (singer != null) {
            redisUtils.setCacheObject(cacheKey, singer, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        }
        
        return singer;
    }

    /**
     * 查询所有歌手（带缓存）
     *
     * @return 歌手列表
     */
    @Override
    public List<Singer> allSinger() {
        Object cachedData = redisUtils.getCacheObject(ALL_SINGER_KEY);
        if (cachedData != null) {
            log.info("从Redis缓存获取所有歌手数据");
            return (List<Singer>) cachedData;
        }
        
        log.info("从数据库查询所有歌手数据");
        List<Singer> singers = singerMapper.allSinger();
        
        redisUtils.setCacheObject(ALL_SINGER_KEY, singers, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        
        return singers;
    }

    /**
     * 根据歌手名字模糊查询列表
     *
     * @param name 歌手名称
     * @return 歌手列表
     */
    @Override
    public List<Singer> singerOfName(String name) {
        // 模糊匹配模式属于查询实现细节，原先在 Controller 里拼 "%" + name + "%"；
        // 放到这里之后，调用方传的就是用户输入本身
        return singerMapper.singerOfName("%" + name + "%");
    }

    /**
     * 根据性别查询
     *
     * @param sex 性别
     * @return 歌手列表
     */
    @Override
    public List<Singer> singerOfSex(Integer sex) {
        return singerMapper.singerOfSex(sex);
    }
    
    /**
     * 分页查询所有歌手
     *
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    @Override
    public PageInfo<Singer> allSingerByPage(int pageNum, int pageSize) {
        // 开启分页
        PageHelper.startPage(pageNum, pageSize);
        // 执行查询
        List<Singer> list = singerMapper.allSinger();
        // 封装分页结果
        return new PageInfo<>(list);
    }

}
