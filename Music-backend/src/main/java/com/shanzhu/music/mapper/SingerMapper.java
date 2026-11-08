package com.shanzhu.music.mapper;

import com.shanzhu.music.entity.po.Singer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 歌手 持久层
 */
@Mapper
public interface SingerMapper {

    /**
     * 添加歌手
     *
     * @param singer 歌手信息
     * @return 结果
     */
    Integer insert(Singer singer);

    /**
     * 添加歌手
     *
     * @param singer 歌手信息
     * @return 结果
     */
    Integer update(Singer singer);

    /**
     * 删除歌手
     *
     * @param id 歌手id
     * @return 删除结果
     */
    Integer delete(Integer id);

    /**
     * 查询歌手
     *
     * @param id 歌手id
     * @return 歌手
     */
    Singer selectByPrimaryKey(Integer id);

    /**
     * 查询所有歌手
     *
     * @return 歌手列表
     */
    List<Singer> allSinger();

    /**
     * 按关键词模糊查询歌手，供管理端筛选与分页共用。
     * keyword 为空时不加过滤条件，与 allSinger 等价。
     *
     * @param keyword 姓名、地区或简介的模糊词
     * @return 匹配的歌手列表
     */
    List<Singer> selectByKeyword(@Param("keyword") String keyword);

    /**
     * 根据歌手名字模糊查询列表
     *
     * @param name 歌手名称
     * @return 歌手列表
     */
    List<Singer> singerOfName(String name);

    /**
     * 根据性别查询
     *
     * @param sex 性别
     * @return 歌手列表
     */
    List<Singer> singerOfSex(Integer sex);
}
