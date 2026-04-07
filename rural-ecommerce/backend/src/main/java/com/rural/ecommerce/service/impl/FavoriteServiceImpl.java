package com.rural.ecommerce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rural.ecommerce.entity.Favorite;
import com.rural.ecommerce.mapper.FavoriteMapper;
import com.rural.ecommerce.service.FavoriteService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FavoriteServiceImpl extends ServiceImpl<FavoriteMapper, Favorite> implements FavoriteService {

    @Override
    public boolean addFavorite(Long userId, Long productId) {
        // 检查是否已收藏
        if (isFavorite(userId, productId)) {
            return true;
        }
        
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setProductId(productId);
        favorite.setCreateTime(LocalDateTime.now());
        
        return save(favorite);
    }

    @Override
    public boolean removeFavorite(Long userId, Long productId) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId)
               .eq(Favorite::getProductId, productId);
        return remove(wrapper);
    }

    @Override
    public List<Favorite> getUserFavorites(Long userId) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId)
               .orderByDesc(Favorite::getCreateTime);
        return list(wrapper);
    }

    @Override
    public boolean isFavorite(Long userId, Long productId) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId)
               .eq(Favorite::getProductId, productId);
        return count(wrapper) > 0;
    }
}
