package com.rural.ecommerce.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 订单统计报表实体类
 * 用于记录每日订单统计数据
 */
@Data
@TableName("order_stats")
public class OrderStats {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 统计日期
     */
    private LocalDate statDate;
    
    /**
     * 订单总数
     */
    private Integer totalOrders;
    
    /**
     * 支付订单数
     */
    private Integer paidOrders;
    
    /**
     * 取消订单数
     */
    private Integer cancelledOrders;
    
    /**
     * 完成订单数
     */
    private Integer completedOrders;
    
    /**
     * 订单总金额
     */
    private BigDecimal totalAmount;
    
    /**
     * 实际支付金额
     */
    private BigDecimal paidAmount;
    
    /**
     * 退款金额
     */
    private BigDecimal refundAmount;
    
    /**
     * 新增用户数
     */
    private Integer newUsers;
    
    /**
     * 活跃用户数
     */
    private Integer activeUsers;
    
    /**
     * 统计类型：1日统计 2周统计 3月统计
     */
    private Integer statType;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
