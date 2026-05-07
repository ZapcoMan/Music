package com.shanzhu.music.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.shanzhu.music.entity.po.Singer;
import com.shanzhu.music.mapper.SingerMapper;
import com.shanzhu.music.service.SingerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 歌手 服务层实现类
 */
@Service
@RequiredArgsConstructor
public class SingerServiceImpl implements SingerService {

    private final SingerMapper singerMapper;

    /**
     * 添加歌手
     * @param singer 歌手信息
     * @return 结果
     */
    @Override
    public Boolean insert(Singer singer) {
        return singerMapper.insert(singer) > 0;
    }

    /**
     * 修改歌手
     * @param singer 歌手信息
     * @return 结果
     */
    @Override
    public Boolean update(Singer singer) {
        return singerMapper.update(singer) > 0;
    }

    /**
     * 删除歌手
     *
     * @param id 歌手id
     * @return 删除结果
     */
    @Override
    public Boolean delete(Integer id) {
        return singerMapper.delete(id) > 0;
    }

    /**
     * 查询歌手
     *
     * @param id 歌手id
     * @return 歌手
     */
    @Override
    public Singer selectByPrimaryKey(Integer id) {
        return singerMapper.selectByPrimaryKey(id);
    }

    /**
     * 查询所有歌手
     *
     * @return 歌手列表
     */
    @Override
    public List<Singer> allSinger() {
        return singerMapper.allSinger();
    }

    /**
     * 根据歌手名字模糊查询列表
     *
     * @param name 歌手名称
     * @return 歌手列表
     */
    @Override
    public List<Singer> singerOfName(String name) {
        return singerMapper.singerOfName(name);
    }

    /**
     * 根据性别查询
     *
     * @param sex 性别
     * @return 歌手列表
     */
    @Override
    public List<Singer> singerOfSex(Integer sex) {
        return singerMapper.singerOfSex(sex);
    }
    
    /**
     * 分页查询所有歌手
     *
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    @Override
    public PageInfo<Singer> allSingerByPage(int pageNum, int pageSize) {
        // 开启分页
        PageHelper.startPage(pageNum, pageSize);
        // 执行查询
        List<Singer> list = singerMapper.allSinger();
        // 封装分页结果
        return new PageInfo<>(list);
    }

}
