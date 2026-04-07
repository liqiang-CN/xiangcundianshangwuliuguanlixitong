package com.rural.ecommerce.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rural.ecommerce.entity.ProductIntroduction;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 商品介绍数据访问层
 */
@Mapper
public interface ProductIntroductionRepository extends BaseMapper<ProductIntroduction> {

    /**
     * 根据商品ID查询商品介绍列表
     * @param productId 商品ID
     * @return 商品介绍列表
     */
    @Select("SELECT * FROM product_introduction WHERE product_id = #{productId} ORDER BY sort_order ASC, id ASC")
    List<ProductIntroduction> findByProductId(@Param("productId") Long productId);

    /**
     * 根据商品ID删除所有介绍
     * @param productId 商品ID
     * @return 删除数量
     */
    @Delete("DELETE FROM product_introduction WHERE product_id = #{productId}")
    int deleteByProductId(@Param("productId") Long productId);
}
