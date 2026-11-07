package com.shanzhu.music.service.impl;
import com.shanzhu.music.common.CacheKeys;

import com.hankcs.hanlp.HanLP;
import com.shanzhu.music.entity.po.Comment;
import com.shanzhu.music.exception.BusinessException;
import com.shanzhu.music.mapper.CommentMapper;
import com.shanzhu.music.service.CommentService;
import com.shanzhu.music.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 评论 服务层实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {
    @Resource
    private final CommentMapper commentMapper;
    private final RedisUtils redisUtils;
    
    // Redis缓存键前缀
    private static final String COMMENT_CACHE_KEY = CacheKeys.COMMENT;
    private static final long CACHE_EXPIRE_HOURS = 2; // 评论缓存2小时（评论变化较频繁）

    /**
     * 添加评论（延时双删缓存）
     *
     * @param comment 评论内容
     * @return 添加结果
     */
    @Override
    public Boolean insert(Comment comment) {
        Boolean result = commentMapper.insert(comment) > 0;
        if (result) {
            // 使用延时双删策略清除相关缓存
            evictListCache(comment);
            log.info("新增评论，延时双删缓存");
        }
        return result;
    }

    /**
     * 给评论点赞（原子自增 + 延时双删缓存）
     *
     * @param id 评论id
     * @return 自增结果
     */
    @Override
    public Boolean like(Integer id) {
        Comment origin = commentMapper.selectByPrimaryKey(id);
        if (origin == null) {
            throw new BusinessException("评论不存在");
        }
        // 自增交给数据库：此前在 Controller 里"先读出 up 再 +1 回写"，
        // 两个并发请求会读到同一个值并互相覆盖，点赞数被吞掉。
        // 同时 up 只能由服务端算，客户端传多少一概不采纳。
        Boolean result = commentMapper.incrementUp(id) > 0;
        if (result) {
            // 列表缓存里存着整条 Comment（含 up），不清掉的话点赞要等 2 小时才可见；
            // 此前点赞完全没碰缓存
            evictListCache(origin);
            log.info("评论点赞，延时双删缓存");
        }
        return result;
    }

    /**
     * 删除评论（校验归属）
     *
     * @param id           评论id
     * @param operatorId   令牌中的用户id
     * @param operatorType 令牌中的角色
     * @return 删除结果
     */
    @Override
    public Boolean deleteBy(Integer id, Integer operatorId, String operatorType) {
        Comment origin = commentMapper.selectByPrimaryKey(id);
        if (origin == null) {
            throw new BusinessException("评论不存在");
        }
        // 归属校验：非本人且非管理员不得删除。
        // 原先把 userId 当成普通参数，提交别人的 id 就能把评论"挪到"别人名下，
        // 这类判断也不该留在 Controller——漏写一次就是又一条越权通道。
        if (!operatorId.equals(origin.getUserId()) && !"admin".equals(operatorType)) {
            log.warn("评论删除被拒 - 令牌用户={}, 评论作者={}, commentId={}",
                    operatorId, origin.getUserId(), id);
            throw new BusinessException("无权删除他人评论");
        }
        return deleteAndEvict(id, origin);
    }

    /**
     * 清除评论所属歌单/歌曲的列表缓存
     */
    private void evictListCache(Comment comment) {
        if (comment.getSongId() != null) {
            redisUtils.deleteWithDelay(COMMENT_CACHE_KEY + "song:" + comment.getSongId());
        }
        if (comment.getSongListId() != null) {
            redisUtils.deleteWithDelay(COMMENT_CACHE_KEY + "songlist:" + comment.getSongListId());
        }
    }

    /**
     * 删除评论（延时双删缓存）
     *
     * @param id 评论id
     * @return 删除结果
     */
    @Override
    public Boolean delete(Integer id) {
        return deleteAndEvict(id, commentMapper.selectByPrimaryKey(id));
    }

    /**
     * 删除已查出的评论并清理其列表缓存。
     * 单独拆出来是为了让 deleteBy 复用归属校验时那次查询，不必再读一次库。
     */
    private Boolean deleteAndEvict(Integer id, Comment comment) {
        Boolean result = commentMapper.delete(id) > 0;
        if (result && comment != null) {
            // 使用延时双删策略清除相关缓存
            evictListCache(comment);
            log.info("删除评论，延时双删缓存");
        }
        return result;
    }

    /**
     * 通过主键查询评论
     *
     * @param id 评论主键
     * @return 评论
     */
    @Override
    public Comment selectByPrimaryKey(Integer id) {
        return commentMapper.selectByPrimaryKey(id);
    }

    /**
     * 查询所有评论
     *
     * @return 所有评论
     */
    @Override
    public List<Comment> allComment() {
        return commentMapper.allComment();
    }

    /**
     * 查询某个歌曲下的所有评论（带缓存）
     *
     * @param songId 歌曲id
     * @return 评论列表
     */
    @Override
    public List<Comment> commentOfSongId(Integer songId) {
        String cacheKey = COMMENT_CACHE_KEY + "song:" + songId;
        
        Object cachedData = redisUtils.getCacheObject(cacheKey);
        if (cachedData != null) {
            log.info("从Redis缓存获取歌曲评论，songId={}", songId);
            return (List<Comment>) cachedData;
        }
        
        log.info("从数据库查询歌曲评论，songId={}", songId);
        List<Comment> comments = commentMapper.commentOfSongId(songId);
        
        redisUtils.setCacheObject(cacheKey, comments, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        
        return comments;
    }

    /**
     * 查询某个歌单下的所有评论（带缓存）
     *
     * @param songListId 歌单id
     * @return 评论列表
     */
    @Override
    public List<Comment> commentOfSongListId(Integer songListId) {
        String cacheKey = COMMENT_CACHE_KEY + "songlist:" + songListId;
        
        Object cachedData = redisUtils.getCacheObject(cacheKey);
        if (cachedData != null) {
            log.info("从Redis缓存获取歌单评论，songListId={}", songListId);
            return (List<Comment>) cachedData;
        }
        
        log.info("从数据库查询歌单评论，songListId={}", songListId);
        List<Comment> comments = commentMapper.commentOfSongListId(songListId);
        
        redisUtils.setCacheObject(cacheKey, comments, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        
        return comments;
    }


    /**
     * 获取所有精选评论内容
     */
    public List<String> getAllComments() {
        return commentMapper.selectAllComments();
    }

    /**
     * 接口方法此前恒返回空列表，真正的分词统计实现（getTopFourFrequentWords）
     * 没有接入任何调用链，导致 /comment/frequent-words 永远拿不到数据。
     */
    @Override
    public List<String> getFrequentWords(String id) {
        return getTopFourFrequentWords(id);
    }

    /**
 * 获取指定歌单评论中的最高频四个词。
 * 如果最高频的词有多个，则全部展示。
 * 如果最高频的词的频率小于2，则不展示。
 * 如果不足四个，则展示所有高频词。
 *
 * @param id 歌单ID
 * @return 最高频的四个词列表
 */
public List<String> getTopFourFrequentWords(String id) {
    List<String> comments = commentMapper.selectAllCommentsBySongListId(id);
    Map<String, Integer> wordCounts = new HashMap<>();

    // 分词并统计每个词的出现次数
    for (String comment : comments) {
        List<String> tokens = HanLP.segment(comment).stream()
                .map(term -> term.word)
                .filter(word -> word.length() > 1) // 过滤掉长度小于等于1的词
                .collect(Collectors.toList());
        for (String token : tokens) {
            wordCounts.put(token, wordCounts.getOrDefault(token, 0) + 1);
        }
    }

    // 词频明细只作排查用，接口接线后按请求触发，不能以 info 级别刷屏
    wordCounts.forEach((word, count) -> log.debug(word + ": " + count));

    // 根据出现次数排序，并获取排序后的单词列表
    List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(wordCounts.entrySet());
    sortedEntries.sort(Map.Entry.<String, Integer>comparingByValue().reversed()); // 降序排序

    List<String> sortedWords = new ArrayList<>();
    int count = 0;
    int maxCount = 4;
    Integer fourthHighestFrequency = sortedEntries.size() > 3 ? sortedEntries.get(3).getValue() : null;

    // 遍历排序后的词频列表，添加最高频的四个词
    for (Map.Entry<String, Integer> entry : sortedEntries) {
        if (count >= maxCount) {
            break; // 如果已经添加了四个词，则停止遍历
        }
        // 如果最高频的词的频率小于2，则不展示
        if (entry.getValue() < 2) {
            continue;
        }
        sortedWords.add(entry.getKey());
        count++;
        // 如果当前词的频率等于第四高的频率，继续添加
        if (entry.getValue().equals(fourthHighestFrequency)) {
            maxCount = count + (int) sortedEntries.subList(3, sortedEntries.size())
                    .stream()
                    .filter(e -> e.getValue().equals(fourthHighestFrequency))
                    .count();
        }
    }

    return sortedWords;
  }
}
