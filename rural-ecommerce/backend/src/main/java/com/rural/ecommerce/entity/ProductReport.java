package com.rural.ecommerce.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("product_report")
public class ProductReport {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long productId;
    
    private String productName;
    
    private String productImage;
    
    private java.math.BigDecimal productPrice;

    private String productUnit;

    private Long farmerId;
    
    private String farmerName;
    
    private Long reporterId;
    
    private String reporterName;
    
    private Integer reportType;
    
    private String reportContent;
    
    private String reportImages;
    
    private Integer status;
    
    private String adminRemark;
    
    private LocalDateTime handleTime;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
