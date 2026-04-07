package com.rural.ecommerce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rural.ecommerce.entity.OrderInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.Map;

@Mapper
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {

    /**
     * 查询订单统计数据
     */
    @Select("SELECT " +
            "COUNT(*) as totalOrders, " +
            "SUM(CASE WHEN status >= 1 THEN 1 ELSE 0 END) as paidOrders, " +
            "SUM(CASE WHEN status = 4 THEN 1 ELSE 0 END) as cancelledOrders, " +
            "SUM(CASE WHEN status = 3 THEN 1 ELSE 0 END) as completedOrders, " +
            "SUM(total_amount) as totalAmount, " +
            "SUM(CASE WHEN status >= 1 THEN pay_amount ELSE 0 END) as paidAmount, " +
            "SUM(CASE WHEN status = 4 THEN total_amount ELSE 0 END) as refundAmount " +
            "FROM order_info " +
            "WHERE create_time >= #{startTime} AND create_time <= #{endTime} " +
            "AND deleted = 0")
    Map<String, Object> selectOrderStats(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 查询活跃用户数（有订单的用户数）
     */
    @Select("SELECT COUNT(DISTINCT user_id) FROM order_info " +
            "WHERE create_time >= #{startTime} AND create_time <= #{endTime} " +
            "AND deleted = 0")
    Integer selectActiveUserCount(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}
