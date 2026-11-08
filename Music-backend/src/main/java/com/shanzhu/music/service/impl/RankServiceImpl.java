package com.shanzhu.music.service.impl;

import com.shanzhu.music.config.RecommendationProperties;
import com.shanzhu.music.entity.po.Rank;
import com.shanzhu.music.mapper.RankMapper;
import com.shanzhu.music.service.RankService;
import com.shanzhu.music.service.ranking.RecommendationCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 评价 服务层实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankServiceImpl implements RankService {

    private final RankMapper rankMapper;
    private final RecommendationCache recommendationCache;
    private final RecommendationProperties recommendationProperties;

    /**
     * 保存评分（新增或改分）
     *
     * ranks 上有 UNIQUE(song_list_id, consumer_id)，而 RankMapper 原先只有
     * insert/delete，没有 update —— 用户改过一次分之后，第二次提交必然撞唯一键，
     * 页面看到"评价失败"，而那份旧分数一直挂在那里没变过。
     * 现在按"是否已有评分"分流：没有则插入，有则覆盖；
     * 并发下两个请求同时读到"还没有评分"时，后写的那个撞 1062，
     * 捕获后走更新，语义上正是"以这次的分数为准"，不该把数据库约束错误抛给用户。
     *
     * @param rank 评价信息
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean save(Rank rank) {
        Rank existing = rankMapper.selectByConsumerAndSongList(rank.getConsumerId(), rank.getSongListId());
        boolean result;
        if (existing == null) {
            try {
                result = rankMapper.insert(rank) > 0;
            } catch (DuplicateKeyException e) {
                log.info("并发提交首次评分，唯一键冲突后改走更新，consumerId={}, songListId={}",
                        rank.getConsumerId(), rank.getSongListId());
                result = rankMapper.updateScoreByConsumerAndSongList(rank) > 0;
            }
        } else {
            result = rankMapper.updateScoreByConsumerAndSongList(rank) > 0;
        }

        if (result && rank.getConsumerId() != null) {
            // 评分会影响相似度与统计，让该用户的推荐缓存失效
            recommendationCache.evict(rank.getConsumerId());
            log.info("保存评价，清除推荐缓存，userId={}", rank.getConsumerId());
        }
        return result;
    }

    /**
     * 计算平均分
     *
     * @param songListId 歌单id
     * @return 平均分
     */
    @Override
    public Double rankOfSongListId(Integer songListId) {
        int rankNum = rankMapper.selectRankNum(songListId);
        if (rankNum == 0) {
            // 中性兜底取自 recommendation.neutral-rating，与打分侧共用一个配置项；
            // 原先这里硬编码 5，两处各写一份，改量纲时必漏一处
            return recommendationProperties.getNeutralRating();
        }
        return (double) rankMapper.selectScoreSum(songListId) / rankNum;
    }

}
