package com.shanzhu.music.mapper;

import com.shanzhu.music.entity.po.Admin;
import org.apache.ibatis.annotations.Mapper;

/**
 * 管理员 持久层
 */
@Mapper
public interface AdminMapper {

    /**
     * 根据用户名查询管理员
     *
     * @param name 用户名
     * @return 管理员信息
     */
    Admin getByName(String name);

}
