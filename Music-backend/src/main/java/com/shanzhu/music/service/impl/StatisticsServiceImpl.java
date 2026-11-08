package com.shanzhu.music.service.impl;

import com.shanzhu.music.entity.vo.OverviewVo;
import com.shanzhu.music.mapper.StatisticsMapper;
import com.shanzhu.music.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 统计服务实现
 */
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final StatisticsMapper statisticsMapper;

    @Override
    public OverviewVo overview() {
        OverviewVo overview = new OverviewVo();
        overview.setConsumerTotal(statisticsMapper.countConsumer());
        overview.setSongTotal(statisticsMapper.countSong());
        overview.setSingerTotal(statisticsMapper.countSinger());
        overview.setSongListTotal(statisticsMapper.countSongList());
        overview.setConsumerSex(statisticsMapper.groupConsumerSex());
        overview.setSingerSex(statisticsMapper.groupSingerSex());
        overview.setSingerLocation(statisticsMapper.groupSingerLocation());
        overview.setSongListStyle(statisticsMapper.groupSongListStyle());
        return overview;
    }
}
