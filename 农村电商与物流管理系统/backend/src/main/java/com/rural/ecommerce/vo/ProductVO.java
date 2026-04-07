package com.rural.ecommerce.vo;

import com.rural.ecommerce.entity.ProductIntroduction;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductVO {

    private Long id;

    private String name;

    private String description;

    private BigDecimal price;

    private String unit;

    private BigDecimal originalPrice;

    private Integer stock;

    private Integer sales;

    private String mainImage;

    private String images;

    private Long categoryId;

    private String categoryName;

    private Long farmerId;

    private String farmerName;

    private String farmerAvatar;

    private String shopName;

    private String origin;

    private Integer status;

    private Integer reviewStatus;

    private Integer isRecommend;

    private Integer isHot;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /**
     * 商品介绍列表
     */
    private List<ProductIntroduction> introductionList;
}
