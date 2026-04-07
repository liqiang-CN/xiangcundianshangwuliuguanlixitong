package com.rural.ecommerce.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("logistics_track")
public class LogisticsTrack {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long logisticsId;
    
    private Integer status;
    
    private String description;
    
    private String location;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
