package com.shanzhu.music.service.ranking;

import com.shanzhu.music.config.RecommendationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 新歌曲流量扶持服务
 *
 * 扶持以"加性增益"形式在 sigmoid 之前生效，使最终得分仍落在 (0, 1) 区间内。
 * 原先的做法是在 sigmoid 之后乘以 1.1~1.5：那会把概率推成大于 1 的无意义数值，
 * 并与线性特征 songAgeDays 的负权重构成重复计分。
 *
 * 分档阈值与增益值均来自 RecommendationProperties，默认：
 *   0-7 天 +0.15、8-14 天 +0.10、15-30 天 +0.05、超期或年龄未知 不扶持
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewSongBoostService {

    private final RecommendationProperties properties;

    /**
     * 计算新歌扶持增益
     *
     * @param songAgeDays 歌曲发布天数，null 或负值表示年龄未知
     * @return 叠加到排序得分上的增益，恒 >= 0
     */
    public double calculateBonus(Integer songAgeDays) {
        RecommendationProperties.NewSong config = properties.getNewSong();
        if (songAgeDays == null || songAgeDays < 0 || songAgeDays > config.getThresholdDays()) {
            return 0.0;
        }

        double bonus;
        if (songAgeDays <= config.getStrongDays()) {
            bonus = config.getStrongBonus();
        } else if (songAgeDays <= config.getMediumDays()) {
            bonus = config.getMediumBonus();
        } else {
            bonus = config.getLightBonus();
        }

        log.debug("歌曲发布 {} 天，新歌扶持增益: {}", songAgeDays, bonus);
        return bonus;
    }

    /**
     * 是否处于扶持期内
     */
    public boolean isBoosted(Integer songAgeDays) {
        return calculateBonus(songAgeDays) > 0.0;
    }
}
