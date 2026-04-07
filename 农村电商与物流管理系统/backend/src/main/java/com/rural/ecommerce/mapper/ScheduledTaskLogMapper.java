package com.rural.ecommerce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rural.ecommerce.entity.ScheduledTaskLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务执行日志Mapper接口
 */
@Mapper
public interface ScheduledTaskLogMapper extends BaseMapper<ScheduledTaskLog> {
    
    /**
     * 查询指定任务的最近执行记录
     */
    @Select("SELECT * FROM scheduled_task_log WHERE task_name = #{taskName} ORDER BY create_time DESC LIMIT #{limit}")
    List<ScheduledTaskLog> selectRecentLogs(@Param("taskName") String taskName, @Param("limit") Integer limit);
    
    /**
     * 查询时间范围内的执行记录
     */
    @Select("SELECT * FROM scheduled_task_log WHERE create_time BETWEEN #{startTime} AND #{endTime} ORDER BY create_time DESC")
    List<ScheduledTaskLog> selectByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查询失败的任务记录
     */
    @Select("SELECT * FROM scheduled_task_log WHERE status = 0 AND create_time >= #{startTime} ORDER BY create_time DESC")
    List<ScheduledTaskLog> selectFailedLogs(@Param("startTime") LocalDateTime startTime);
    
    /**
     * 统计任务执行次数
     */
    @Select("SELECT COUNT(*) FROM scheduled_task_log WHERE task_name = #{taskName} AND status = #{status}")
    Long countByStatus(@Param("taskName") String taskName, @Param("status") Integer status);
}
