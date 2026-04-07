package com.rural.ecommerce.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("farmer_address")
public class FarmerAddress {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long farmerId;
    
    private String senderName;
    
    private String senderPhone;
    
    private String province;
    
    private String city;
    
    private String district;
    
    private String detailAddress;
    
    private Integer isDefault;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
