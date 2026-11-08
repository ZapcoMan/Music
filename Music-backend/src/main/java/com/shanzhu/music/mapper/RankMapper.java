package com.shanzhu.music.mapper;

import com.shanzhu.music.entity.po.Rank;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 评价 持久层
 */
@Mapper
public interface RankMapper {

    /**
     * 新增评价
     *
     * @param rank 评价信息
     * @return 结果
     */
    Integer insert(Rank rank);

    /**
     * 查某用户对某歌单的既有评分
     * ranks 有 UNIQUE(song_list_id, consumer_id)，所以这里最多一行
     *
     * @param consumerId 用户id
     * @param songListId 歌单id
     * @return 既有评分，没有时为 null
     */
    Rank selectByConsumerAndSongList(@Param("consumerId") Integer consumerId,
                                    @Param("songListId") Integer songListId);

    /**
     * 覆盖某用户对某歌单的评分
     * 改分走的是一条 UPDATE：原先 Mapper 没有这个方法，第二次提交只能撞唯一键，
     * 用户以为"评价失败"，实际是分数从来没被改过
     *
     * @param rank 评价信息（consumerId + songListId 定位，score 为新值）
     * @return 影响行数
     */
    Integer updateScoreByConsumerAndSongList(Rank rank);

    /**
     * 查总分
     *
     * @param songListId 歌单
     * @return 总分
     */
    Integer selectScoreSum(Integer songListId);

    /**
     * 查总评分人数
     *
     * @param songListId 歌单
     * @return 查总评分人数
     */
    Integer selectRankNum(Integer songListId);

    /**
     * 查询用户的评分统计
     *
     * @param userId 用户id
     * @return avgRating 平均评分（1-10 分制，无评分时为 5.0）、ratingCount 评分次数
     */
    Map<String, Object> selectUserRatingStats(@Param("userId") Integer userId);

    /**
     * 查询歌曲的评分统计（通过歌单关联）
     *
     * @param songId 歌曲id
     * @return avgRating 平均评分（1-10 分制，无评分时为 5.0）、ratingCount 评分次数
     */
    Map<String, Object> selectSongRatingStats(@Param("songId") Integer songId);

    Integer deleteBySongListId(@Param("songListId") Integer songListId);

    /**
     * 删除某用户的全部评分（删除用户时级联清理，避免相似度计算把已注销用户算进去）
     *
     * @param consumerId 用户id
     * @return 影响行数
     */
    Integer deleteByConsumerId(@Param("consumerId") Integer consumerId);

    /**
     * 批量查询多首歌曲的评分统计，用于替代逐首查询
     *
     * @param songIds 歌曲id列表，调用方需保证非空
     * @return 每项含 songId / avgRating / ratingCount；无评分记录的歌曲不会出现在结果中
     */
    List<Map<String, Object>> selectRatingStatsBySongIds(@Param("songIds") List<Integer> songIds);
}
