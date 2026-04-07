package com.rural.ecommerce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rural.ecommerce.entity.ProductReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProductReviewMapper extends BaseMapper<ProductReview> {

    @Select("SELECT AVG(rating) FROM product_review WHERE product_id = #{productId} AND status = 1 AND deleted = 0")
    Double selectAverageRating(@Param("productId") Long productId);

    @Select("SELECT COUNT(*) FROM product_review WHERE product_id = #{productId} AND status = 1 AND deleted = 0")
    Integer selectReviewCount(@Param("productId") Long productId);

    // 查询评价（包含已删除的）
    @Select("SELECT * FROM product_review WHERE id = #{id}")
    ProductReview selectByIdWithDeleted(@Param("id") Long id);

    // 使用原生SQL查询评价列表（包含已删除的）
    @Select("${sql}")
    List<ProductReview> selectListBySql(@Param("sql") String sql);

    // 使用原生SQL查询评价数量（包含已删除的）
    @Select("${sql}")
    Long selectCountBySql(@Param("sql") String sql);

    @Select("SELECT rating, COUNT(*) as count FROM product_review WHERE product_id = #{productId} AND status = 1 AND deleted = 0 GROUP BY rating")
    List<Map<String, Object>> selectRatingDistribution(@Param("productId") Long productId);
}
