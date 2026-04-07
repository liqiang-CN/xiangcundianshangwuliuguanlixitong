package com.rural.ecommerce.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("logistics")
public class Logistics {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long orderId;
    
    private String trackingNo;
    
    private Long deliveryId;
    
    private String deliveryName;
    
    private String deliveryPhone;
    
    private Integer status;
    
    private String currentLocation;
    
    private LocalDateTime pickupTime;
    
    private LocalDateTime deliveryTime;
    
    private LocalDateTime signTime;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
