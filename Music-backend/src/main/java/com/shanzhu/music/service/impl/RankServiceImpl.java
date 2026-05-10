package com.shanzhu.music.service.impl;

import com.shanzhu.music.entity.po.Rank;
import com.shanzhu.music.mapper.RankMapper;
import com.shanzhu.music.service.RankService;
import com.shanzhu.music.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 评价 服务层实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankServiceImpl implements RankService {

    private final RankMapper rankMapper;
    private final RedisUtils redisUtils;
    
    // 推荐缓存键前缀
    private static final String RECOMMENDATION_CACHE_KEY = "music:recommendation:user:";

    /**
     * 新增评价
     *
     * @param rank 评价信息
     * @return 结果
     */
    @Override
    public Boolean insert(Rank rank) {
        Boolean result = rankMapper.insert(rank) > 0;
        if (result && rank.getConsumerId() != null) {
            // 清除该用户的推荐缓存，因为评分会影响相似度计算
            redisUtils.deleteWithDelay(RECOMMENDATION_CACHE_KEY + rank.getConsumerId());
            log.info("新增评价，清除推荐缓存，userId={}", rank.getConsumerId());
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
    public Integer rankOfSongListId(Integer songListId) {
        int rankNum = rankMapper.selectRankNum(songListId);
        if (rankNum == 0) {
            return 5;
        }
        return rankMapper.selectScoreSum(songListId) / rankNum;
    }

}
