package com.shanzhu.music.service.ranking;

import com.shanzhu.music.entity.po.Song;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * 新歌曲流量扶持服务
 * 
 * 扶持策略：
 * 1. 新歌定义：发布30天内的歌曲
 * 2. 扶持系数：根据歌曲年龄动态调整
 *    - 0-7天: 1.5倍曝光
 *    - 8-14天: 1.3倍曝光
 *    - 15-30天: 1.1倍曝光
 *    - 30天以上: 1.0倍（无扶持）
 */
@Slf4j
@Service
public class NewSongBoostService {
    
    // 新歌定义：发布天数阈值
    private static final int NEW_SONG_DAYS_THRESHOLD = 30;
    
    /**
     * 计算新歌扶持系数
     *
     * @param song 歌曲对象
     * @return 扶持系数（>= 1.0）
     */
    public double calculateBoostFactor(Song song) {
        if (song == null || song.getCreateTime() == null) {
            return 1.0;
        }
        
        // 计算歌曲发布天数
        LocalDate createDate = song.getCreateTime().toInstant()
            .atZone(ZoneId.systemDefault()).toLocalDate();
        long daysSinceRelease = ChronoUnit.DAYS.between(createDate, LocalDate.now());
        
        // 如果不是新歌，返回1.0（无扶持）
        if (daysSinceRelease > NEW_SONG_DAYS_THRESHOLD) {
            return 1.0;
        }
        
        // 根据歌曲年龄计算扶持系数
        double boostFactor;
        if (daysSinceRelease <= 7) {
            // 0-7天：强力扶持 1.5倍
            boostFactor = 1.5;
        } else if (daysSinceRelease <= 14) {
            // 8-14天：中等扶持 1.3倍
            boostFactor = 1.3;
        } else {
            // 15-30天：轻度扶持 1.1倍
            boostFactor = 1.1;
        }
        
        log.debug("歌曲 {} 发布 {} 天，扶持系数: {}", song.getId(), daysSinceRelease, boostFactor);
        return boostFactor;
    }
    
    /**
     * 应用新歌扶持到排序得分
     *
     * @param originalScore 原始得分
     * @param song          歌曲对象
     * @return 扶持后的得分
     */
    public double applyBoost(double originalScore, Song song) {
        double boostFactor = calculateBoostFactor(song);
        double boostedScore = originalScore * boostFactor;
        
        if (boostFactor > 1.0) {
            log.debug("歌曲 {} 得分提升: {} -> {} (系数: {})", 
                song.getId(), originalScore, boostedScore, boostFactor);
        }
        
        return boostedScore;
    }
    
    /**
     * 检查是否为新歌
     *
     * @param song 歌曲对象
     * @return true=新歌, false=老歌
     */
    public boolean isNewSong(Song song) {
        return calculateBoostFactor(song) > 1.0;
    }
}
