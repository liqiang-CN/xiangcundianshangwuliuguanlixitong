package com.rural.ecommerce.controller;

import com.rural.ecommerce.entity.ShopFollow;
import com.rural.ecommerce.entity.User;
import com.rural.ecommerce.repository.ShopFollowRepository;
import com.rural.ecommerce.service.UserService;
import com.rural.ecommerce.util.JwtUtil;
import com.rural.ecommerce.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/shop-follow")
public class ShopFollowController {

    @Autowired
    private ShopFollowRepository shopFollowRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    private Long getCurrentUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            return jwtUtil.getUserId(token);
        }
        return null;
    }

    @PostMapping("/{shopId}")
    public Result<Void> followShop(@PathVariable Long shopId, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("请先登录");
        }

        // 检查是否已关注
        ShopFollow existing = shopFollowRepository.findByUserIdAndShopId(userId, shopId);
        if (existing != null) {
            return Result.error("已关注该店铺");
        }

        // 获取店铺信息
        User shop = userService.getById(shopId);
        if (shop == null || shop.getUserType() != 2) {
            return Result.error("店铺不存在");
        }

        ShopFollow follow = new ShopFollow();
        follow.setUserId(userId);
        follow.setShopId(shopId);
        follow.setShopName(shop.getShopName() != null ? shop.getShopName() : shop.getUsername());
        follow.setCreateTime(LocalDateTime.now());
        shopFollowRepository.insert(follow);

        return Result.success();
    }

    @DeleteMapping("/{shopId}")
    public Result<Void> unfollowShop(@PathVariable Long shopId, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("请先登录");
        }

        shopFollowRepository.deleteByUserIdAndShopId(userId, shopId);
        return Result.success();
    }

    @GetMapping("/check/{shopId}")
    public Result<Boolean> checkFollow(@PathVariable Long shopId, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.success(false);
        }

        ShopFollow follow = shopFollowRepository.findByUserIdAndShopId(userId, shopId);
        return Result.success(follow != null);
    }

    @GetMapping("/my-follows")
    public Result<List<ShopFollow>> getMyFollows(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("请先登录");
        }

        List<ShopFollow> list = shopFollowRepository.findByUserId(userId);
        return Result.success(list);
    }

    @GetMapping("/shop-fans/{shopId}")
    public Result<Integer> getShopFansCount(@PathVariable Long shopId) {
        int count = shopFollowRepository.countByShopId(shopId);
        return Result.success(count);
    }

    @GetMapping("/shop-fans-list/{shopId}")
    public Result<List<ShopFollow>> getShopFans(@PathVariable Long shopId, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("请先登录");
        }

        // 验证是否是店铺主人
        User shop = userService.getById(shopId);
        if (shop == null || !shop.getId().equals(userId)) {
            return Result.error("无权查看");
        }

        List<ShopFollow> list = shopFollowRepository.findByShopId(shopId);
        return Result.success(list);
    }

    @GetMapping("/my-followed-shops")
    public Result<List<Map<String, Object>>> getMyFollowedShops(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("请先登录");
        }

        List<ShopFollow> follows = shopFollowRepository.findByUserId(userId);

        // 获取店铺详细信息
        List<Map<String, Object>> shopList = follows.stream().map(follow -> {
            Map<String, Object> map = new HashMap<>();
            User shop = userService.getById(follow.getShopId());
            if (shop != null) {
                map.put("shopId", shop.getId());
                map.put("shopName", shop.getShopName() != null ? shop.getShopName() : shop.getUsername());
                map.put("shopAvatar", shop.getAvatar());
                map.put("followTime", follow.getCreateTime());
                map.put("mainCategory", shop.getMainCategory());
            }
            return map;
        }).collect(java.util.stream.Collectors.toList());

        return Result.success(shopList);
    }
}
