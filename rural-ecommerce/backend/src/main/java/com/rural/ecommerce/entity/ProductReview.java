package com.rural.ecommerce.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("product_review")
public class ProductReview {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long productId;
    
    private Long userId;
    
    private Long orderId;
    
    private Integer rating;
    
    private String content;
    
    private String images;
    
    private String video;
    
    private Integer status;
    
    private Integer isAnonymous;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
