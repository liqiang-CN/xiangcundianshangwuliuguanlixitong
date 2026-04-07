package com.rural.ecommerce.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("order_info")
public class OrderInfo {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String orderNo;
    
    private Long userId;
    
    private Long farmerId;
    
    private Long shopId;
    
    private String shopName;
    
    private BigDecimal totalAmount;
    
    private BigDecimal freightAmount;
    
    private BigDecimal discountAmount;
    
    private BigDecimal payAmount;
    
    private Integer payType;
    
    private Integer status;

    private Integer cancelType;

    private String receiverName;
    
    private String receiverPhone;
    
    private String receiverAddress;
    
    private String remark;
    
    private LocalDateTime payTime;
    
    private LocalDateTime shipTime;
    
    private String logisticsNo;
    
    private String logisticsCompany;
    
    private String senderName;
    
    private String senderPhone;
    
    private String senderAddress;
    
    private LocalDateTime receiveTime;
    
    private Integer salesCounted;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
    
    private Integer deleted;
    
    @TableField(exist = false)
    private List<OrderItem> items;
}
