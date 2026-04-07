package com.rural.ecommerce.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rural.ecommerce.entity.Favorite;

import java.util.List;

public interface FavoriteService extends IService<Favorite> {
    
    /**
     * 添加收藏
     */
    boolean addFavorite(Long userId, Long productId);
    
    /**
     * 取消收藏
     */
    boolean removeFavorite(Long userId, Long productId);
    
    /**
     * 获取用户收藏列表
     */
    List<Favorite> getUserFavorites(Long userId);
    
    /**
     * 检查是否已收藏
     */
    boolean isFavorite(Long userId, Long productId);
}
