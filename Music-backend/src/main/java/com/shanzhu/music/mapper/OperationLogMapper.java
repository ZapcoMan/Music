package com.shanzhu.music.mapper;

import com.shanzhu.music.entity.po.OperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 操作审计日志Mapper
 */
@Mapper
public interface OperationLogMapper {

    /**
     * 插入操作日志
     */
    int insert(OperationLog log);

    /**
     * 根据ID查询
     */
    OperationLog selectByPrimaryKey(@Param("id") Integer id);

    /**
     * 查询操作日志列表
     */
    List<OperationLog> selectAll();

    /**
     * 根据用户ID查询
     */
    List<OperationLog> selectByUserId(@Param("userId") Integer userId);

    /**
     * 根据操作类型查询
     */
    List<OperationLog> selectByOperationType(@Param("operationType") String operationType);

    /**
     * 删除日志
     */
    int deleteByPrimaryKey(@Param("id") Integer id);

    /**
     * 清理过期日志
     */
    int deleteExpiredLogs(@Param("days") Integer days);
}
