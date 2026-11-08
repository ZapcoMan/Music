package com.shanzhu.music.service;

import com.shanzhu.music.entity.po.Rank;

/**
 * 评价 服务层
 */
public interface RankService {

    /**
     * 保存评分：同一用户对同一歌单只应有一条记录，
     * 已存在时改为覆盖分数，所以方法名是 save 而不是 insert。
     *
     * @param rank 评价信息
     * @return 结果
     */
    Boolean save(Rank rank);

    /**
     * 计算平均分。
     * 返回 Double：整数除法会把 4.6 打成 4，而这个值既用于星级展示，
     * 也是打分侧的先验，截断误差会一路带进排序。
     *
     * @param songListId 歌单id
     * @return 平均分（1-10 分制）
     */
    Double rankOfSongListId(Integer songListId);
}
