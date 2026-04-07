package com.rural.ecommerce.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品介绍实体类
 */
@Data
@TableName("product_introduction")
public class ProductIntroduction {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 类型：text_image(文本+图片), image(仅图片)
     */
    private String type;

    /**
     * 标题（text_image类型时可选）
     */
    private String title;

    /**
     * 文本内容（text_image类型时必填）
     */
    private String content;

    /**
     * 图片URL
     */
    private String imageUrl;

    /**
     * 排序顺序
     */
    private Integer sortOrder;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
