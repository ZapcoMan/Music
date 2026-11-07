package com.shanzhu.music.mapper;

import com.shanzhu.music.entity.po.Song;
import com.shanzhu.music.entity.vo.TaggedSongVo;
import com.shanzhu.music.entity.po.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 标签 Mapper
 */
@Mapper
public interface TagMapper {

    /**
     * 根据标签名称查询标签
     *
     * @param tagName 标签名称
     * @return 标签
     */
    Tag selectByName(@Param("tagName") String tagName);

    /**
     * 批量插入标签（忽略重复）
     *
     * @param tags 标签列表
     * @return 插入数量
     */
    Integer batchInsertIgnore(@Param("tags") List<Tag> tags);

    /**
     * 为歌曲批量关联标签
     *
     * @param songId 歌曲ID
     * @param tagIds 标签ID列表
     * @return 插入数量
     */
    Integer batchInsertSongTags(@Param("songId") Integer songId, @Param("tagIds") List<Integer> tagIds);

    /**
     * 查询歌曲的标签列表
     *
     * @param songId 歌曲ID
     * @return 标签列表
     */
    List<Tag> selectTagsBySongId(@Param("songId") Integer songId);

    /**
     * 根据标签ID查询相关歌曲（精确匹配，高性能）
     *
     * @param tagId 标签ID
     * @param limit 限制数量
     * @return 歌曲列表
     */
    List<Song> selectSongsByTagId(@Param("tagId") Integer tagId, @Param("limit") Integer limit);

    /**
     * 根据多个标签ID查询相关歌曲（用于多标签召回）
     *
     * @param tagIds 标签ID列表
     * @param limit 限制数量
     * @return 歌曲列表
     */
    List<TaggedSongVo> selectSongsByTagIds(@Param("tagIds") List<Integer> tagIds, @Param("limit") Integer limit);

    /**
     * 统计标签下的歌曲数量
     *
     * @param tagId 标签ID
     * @return 歌曲数量
     */
    Integer countSongsByTagId(@Param("tagId") Integer tagId);

    /**
     * 查询热门标签（按歌曲数量排序）
     *
     * @param limit 限制数量
     * @return 标签列表
     */
    List<Tag> selectHotTags(@Param("limit") Integer limit);

    /**
     * 删除歌曲的所有标签关联
     *
     * @param songId 歌曲ID
     * @return 删除数量
     */
    Integer deleteSongTags(@Param("songId") Integer songId);
}
