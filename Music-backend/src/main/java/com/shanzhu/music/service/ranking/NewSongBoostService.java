package com.shanzhu.music.service.ranking;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 新歌曲流量扶持服务
 *
 * 扶持以"加性增益"形式在 sigmoid 之前生效，使最终得分仍落在 (0,1) 区间内。
 * 原先的做法是在 sigmoid 之后乘以 1.1~1.5：那会把概率推成大于 1 的无意义数值，
 * 并且与线性特征 songAgeDays 的负权重构成重复计分。
 *
 * 扶持分档（与发布天数 songAgeDays 同源，不再重复读 createTime）：
 *   0-7 天    +0.15
 *   8-14 天   +0.10
 *   15-30 天  +0.05
 *   30 天以上 或 年龄未知  不扶持
 */
@Slf4j
@Service
public class NewSongBoostService {

    /** 新歌定义：发布天数阈值 */
    private static final int NEW_SONG_DAYS_THRESHOLD = 30;

    /**
     * 计算新歌扶持增益
     *
     * @param songAgeDays 歌曲发布天数，null 或负值表示年龄未知
     * @return 叠加到排序得分上的增益，恒 >= 0
     */
    public double calculateBonus(Integer songAgeDays) {
        if (songAgeDays == null || songAgeDays < 0 || songAgeDays > NEW_SONG_DAYS_THRESHOLD) {
            return 0.0;
        }

        double bonus;
        if (songAgeDays <= 7) {
            bonus = 0.15;
        } else if (songAgeDays <= 14) {
            bonus = 0.10;
        } else {
            bonus = 0.05;
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
