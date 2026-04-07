package com.rural.ecommerce.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("review_report")
public class ReviewReport {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long reviewId;
    
    private Long reporterId;
    
    private Integer reportType;
    
    private String reportContent;
    
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
