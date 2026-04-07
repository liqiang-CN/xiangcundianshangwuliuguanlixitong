package com.rural.ecommerce.controller;

import com.rural.ecommerce.common.Result;
import com.rural.ecommerce.entity.Favorite;
import com.rural.ecommerce.entity.Product;
import com.rural.ecommerce.service.FavoriteService;
import com.rural.ecommerce.service.ProductService;
import com.rural.ecommerce.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/favorite")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    @Autowired
    private ProductService productService;

    @Autowired
    private JwtUtil jwtUtil;

    private Long getCurrentUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            return null;
        }
        String jwt = token.replace("Bearer ", "");
        return jwtUtil.getUserId(jwt);
    }

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> getFavorites(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("请先登录");
        }

        List<Favorite> favorites = favoriteService.getUserFavorites(userId);
        
        // 获取商品详情
        List<Map<String, Object>> result = favorites.stream().map(favorite -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", favorite.getId());
            item.put("productId", favorite.getProductId());
            item.put("createTime", favorite.getCreateTime());
            
            // 获取商品信息
            Product product = productService.getById(favorite.getProductId());
            if (product != null) {
                item.put("name", product.getName());
                item.put("mainImage", product.getMainImage());
                item.put("price", product.getPrice());
                item.put("origin", product.getOrigin());
            }
            
            return item;
        }).collect(Collectors.toList());

        return Result.success(result);
    }

    @PostMapping("/add")
    public Result<Boolean> addFavorite(@RequestBody Map<String, Long> params, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("请先登录");
        }

        Long productId = params.get("productId");
        if (productId == null) {
            return Result.error("商品ID不能为空");
        }

        boolean success = favoriteService.addFavorite(userId, productId);
        if (success) {
            return Result.success(true);
        }
        return Result.error("添加收藏失败");
    }

    @DeleteMapping("/remove/{productId}")
    public Result<Boolean> removeFavorite(@PathVariable Long productId, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("请先登录");
        }

        boolean success = favoriteService.removeFavorite(userId, productId);
        if (success) {
            return Result.success(true);
        }
        return Result.error("取消收藏失败");
    }

    @GetMapping("/check/{productId}")
    public Result<Map<String, Object>> checkFavorite(@PathVariable Long productId, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("isFavorite", false);
            return Result.success(result);
        }

        boolean isFavorite = favoriteService.isFavorite(userId, productId);
        Map<String, Object> result = new HashMap<>();
        result.put("isFavorite", isFavorite);
        return Result.success(result);
    }
}
