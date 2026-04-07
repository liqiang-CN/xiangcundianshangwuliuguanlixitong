package com.rural.ecommerce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rural.ecommerce.entity.OrderStats;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 订单统计报表Mapper接口
 */
@Mapper
public interface OrderStatsMapper extends BaseMapper<OrderStats> {
    
    /**
     * 根据日期查询统计记录
     */
    @Select("SELECT * FROM order_stats WHERE stat_date = #{date} AND stat_type = #{type} LIMIT 1")
    OrderStats selectByDate(@Param("date") LocalDate date, @Param("type") Integer type);
    
    /**
     * 查询日期范围内的统计数据
     */
    @Select("SELECT * FROM order_stats WHERE stat_date BETWEEN #{startDate} AND #{endDate} AND stat_type = #{type} ORDER BY stat_date DESC")
    List<OrderStats> selectByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("type") Integer type);
    
    /**
     * 获取最近N天的统计数据
     */
    @Select("SELECT * FROM order_stats WHERE stat_type = #{type} ORDER BY stat_date DESC LIMIT #{limit}")
    List<OrderStats> selectRecentStats(@Param("limit") Integer limit, @Param("type") Integer type);
}
