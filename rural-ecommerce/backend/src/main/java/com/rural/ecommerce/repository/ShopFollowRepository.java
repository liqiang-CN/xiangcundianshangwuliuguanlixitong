package com.rural.ecommerce.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rural.ecommerce.entity.ShopFollow;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ShopFollowRepository extends BaseMapper<ShopFollow> {

    @Select("SELECT * FROM shop_follow WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<ShopFollow> findByUserId(Long userId);

    @Select("SELECT * FROM shop_follow WHERE shop_id = #{shopId} ORDER BY create_time DESC")
    List<ShopFollow> findByShopId(Long shopId);

    @Select("SELECT COUNT(*) FROM shop_follow WHERE shop_id = #{shopId}")
    int countByShopId(Long shopId);

    @Select("SELECT * FROM shop_follow WHERE user_id = #{userId} AND shop_id = #{shopId} LIMIT 1")
    ShopFollow findByUserIdAndShopId(Long userId, Long shopId);

    @Delete("DELETE FROM shop_follow WHERE user_id = #{userId} AND shop_id = #{shopId}")
    int deleteByUserIdAndShopId(Long userId, Long shopId);
}
