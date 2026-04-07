package com.rural.ecommerce.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("product")
public class Product {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    private BigDecimal price;

    private String unit;

    private Integer stock;

    private String mainImage;

    private String images;

    private Long categoryId;

    private Long farmerId;

    private String origin;

    private Integer sales;

    private Integer status;

    private Integer reviewStatus;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;

    /**
     * 商品介绍列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<ProductIntroduction> introductionList;
}
