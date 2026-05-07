package com.shanzhu.music.service.impl;

import com.hankcs.hanlp.HanLP;
import com.shanzhu.music.entity.po.Comment;
import com.shanzhu.music.mapper.CommentMapper;
import com.shanzhu.music.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.*;
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

    /**
     * 添加评论
     *
     * @param comment 评论内容
     * @return 添加结果
     */
    @Override
    public Boolean insert(Comment comment) {
        return commentMapper.insert(comment) > 0;
    }

    /**
     * 修改评论
     *
     * @param comment 评论内容
     * @return 修改结果
     */
    @Override
    public Boolean update(Comment comment) {
        return commentMapper.update(comment) > 0;
    }

    /**
     * 删除评论
     *
     * @param id 评论id
     * @return 删除结果
     */
    @Override
    public Boolean delete(Integer id) {
        return commentMapper.delete(id) > 0;
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
     * 查询某个歌曲下的所有评论
     *
     * @param songId 歌曲id
     * @return 评论列表
     */
    @Override
    public List<Comment> commentOfSongId(Integer songId) {
        return commentMapper.commentOfSongId(songId);
    }

    /**
     * 查询某个歌单下的所有评论
     *
     * @param songListId 歌单id
     * @return 评论列表
     */
    @Override
    public List<Comment> commentOfSongListId(Integer songListId) {
        return commentMapper.commentOfSongListId(songListId);
    }


    /**
     * 获取所有精选评论内容
     */
    public List<String> getAllComments() {
        return commentMapper.selectAllComments();
    }

    @Override
    public List<String> getFrequentWords(String id) {
        return Collections.emptyList();
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

    // 打印每个词及其对应的词频次数
    wordCounts.forEach((word, count) -> log.info(word + ": " + count));

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
