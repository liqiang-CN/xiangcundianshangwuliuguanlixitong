package com.rural.ecommerce.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rural.ecommerce.common.Result;
import com.rural.ecommerce.entity.Product;
import com.rural.ecommerce.entity.User;
import com.rural.ecommerce.service.ProductService;
import com.rural.ecommerce.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/shop")
public class ShopController {

    @Autowired
    private UserService userService;

    @Autowired
    private ProductService productService;

    /**
     * 获取特色店铺列表
     */
    @GetMapping("/featured")
    public Result<List<Map<String, Object>>> getFeaturedShops(
            @RequestParam(defaultValue = "4") Integer limit) {
        try {
            // 查询所有农户用户（userType = 2）
            List<User> farmers = userService.getFarmers();
            
            if (farmers.isEmpty()) {
                return Result.success(new ArrayList<>());
            }
            
            // 转换为店铺信息
            List<Map<String, Object>> shopList = farmers.stream()
                .limit(limit)
                .map(farmer -> {
                    Map<String, Object> shop = new HashMap<>();
                    shop.put("id", farmer.getId());
                    shop.put("name", farmer.getShopName() != null ? farmer.getShopName() : farmer.getNickname());
                    shop.put("avatar", farmer.getAvatar());
                    shop.put("location", farmer.getLocation());
                    shop.put("description", farmer.getDescription());
                    shop.put("rating", farmer.getRating());
                    shop.put("followers", farmer.getFollowers());
                    shop.put("mainCategory", farmer.getMainCategory());
                    
                    // 获取店铺商品数和销量
                    Map<String, Object> stats = productService.getShopStats(farmer.getId());
                    shop.put("productCount", stats.get("productCount"));
                    shop.put("sales", stats.get("sales"));
                    
                    return shop;
                })
                .collect(Collectors.toList());
            
            return Result.success(shopList);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取特色店铺失败：" + e.getMessage());
        }
    }

    /**
     * 获取店铺详情
     */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getShopDetail(@PathVariable Long id) {
        User farmer = userService.getById(id);
        if (farmer == null || farmer.getUserType() != 2) {
            return Result.error("店铺不存在");
        }
        
        Map<String, Object> shop = new HashMap<>();
        shop.put("id", farmer.getId());
        shop.put("farmerId", farmer.getId());
        shop.put("username", farmer.getUsername());
        shop.put("shopName", farmer.getShopName());
        shop.put("nickname", farmer.getNickname());
        shop.put("name", farmer.getShopName() != null ? farmer.getShopName() : farmer.getNickname());
        shop.put("avatar", farmer.getAvatar());
        shop.put("location", farmer.getLocation());
        shop.put("description", farmer.getDescription());
        shop.put("tags", parseTags(farmer.getTags()));
        shop.put("mainCategory", farmer.getMainCategory());
        shop.put("ownerName", farmer.getNickname());
        shop.put("phone", farmer.getPhone());
        shop.put("email", farmer.getEmail());
        shop.put("rating", farmer.getRating());
        shop.put("followers", farmer.getFollowers());
        shop.put("status", farmer.getStatus());
        shop.put("createTime", farmer.getCreateTime() != null ?
            farmer.getCreateTime().toLocalDate().toString() : "");
        shop.put("background", farmer.getBackground());
        
        // 获取店铺商品数和销量
        Map<String, Object> stats = productService.getShopStats(farmer.getId());
        shop.put("productCount", stats.get("productCount"));
        shop.put("sales", stats.get("sales"));
        
        return Result.success(shop);
    }

    /**
     * 获取店铺商品列表
     */
    @GetMapping("/{id}/products")
    public Result<Map<String, Object>> getShopProducts(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "12") Integer size,
            @RequestParam(required = false) String sortBy) {
        
        User farmer = userService.getById(id);
        if (farmer == null || farmer.getUserType() != 2) {
            return Result.error("店铺不存在");
        }
        
        // 查询店铺商品
        List<Product> products = productService.getShopProducts(id, page, size, sortBy);
        int total = productService.getShopProductCount(id);
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", products);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        
        return Result.success(result);
    }

    /**
     * 解析标签字符串为列表
     */
    private List<String> parseTags(String tags) {
        if (tags == null || tags.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(tags.split(","));
    }

    /**
     * 管理员：获取店铺列表
     */
    @GetMapping("/admin/shop/list")
    public Result<Map<String, Object>> getAdminShopList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status) {
        try {
            // 查询所有农户用户（userType = 2），包括未开通店铺的
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getUserType, 2)
                   .eq(User::getDeleted, 0);
            
            // 按状态筛选
            if (status != null) {
                wrapper.eq(User::getStatus, status);
            }
            
            List<User> farmers = userService.list(wrapper);
            
            // 过滤和搜索 - 只显示已开通店铺的农户（main_category不为空）
            List<User> filteredList = farmers.stream()
                .filter(farmer -> {
                    // 只显示已开通店铺的农户（main_category不为空）
                    if (farmer.getMainCategory() == null || farmer.getMainCategory().isEmpty()) {
                        return false;
                    }
                    // 按名称搜索（店铺名称或店主名称）
                    if (name != null && !name.isEmpty()) {
                        String shopName = farmer.getShopName();
                        String nickname = farmer.getNickname();
                        boolean matchShopName = shopName != null && shopName.toLowerCase().contains(name.toLowerCase());
                        boolean matchNickname = nickname != null && nickname.toLowerCase().contains(name.toLowerCase());
                        if (!matchShopName && !matchNickname) {
                            return false;
                        }
                    }
                    // 按品类筛选
                    if (category != null && !category.isEmpty()) {
                        String mainCategory = farmer.getMainCategory();
                        if (mainCategory == null || !mainCategory.equals(category)) {
                            return false;
                        }
                    }
                    // 按状态筛选
                    if (status != null) {
                        if (farmer.getStatus() != status) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());
            
            // 分页
            int total = filteredList.size();
            int fromIndex = (page - 1) * size;
            int toIndex = Math.min(fromIndex + size, total);
            
            List<User> pageList = fromIndex < total 
                ? filteredList.subList(fromIndex, toIndex) 
                : new ArrayList<>();
            
            // 转换为店铺信息
            List<Map<String, Object>> shopList = pageList.stream()
                .map(farmer -> {
                    Map<String, Object> shop = new HashMap<>();
                    shop.put("id", farmer.getId());
                    shop.put("shopName", farmer.getShopName());
                    shop.put("nickname", farmer.getNickname());
                    shop.put("avatar", farmer.getAvatar());
                    shop.put("location", farmer.getLocation());
                    shop.put("description", farmer.getDescription());
                    shop.put("tags", farmer.getTags());
                    shop.put("mainCategory", farmer.getMainCategory());
                    shop.put("background", farmer.getBackground());
                    shop.put("rating", farmer.getRating());
                    shop.put("followers", farmer.getFollowers());
                    shop.put("status", farmer.getStatus());
                    shop.put("createTime", farmer.getCreateTime() != null ?
                        farmer.getCreateTime().toLocalDate().toString() : "");
                    return shop;
                })
                .collect(Collectors.toList());
            
            // 统计 - 只统计已开通店铺的农户
            long activeCount = filteredList.stream().filter(f -> f.getStatus() == 1).count();
            long closedCount = filteredList.stream().filter(f -> f.getStatus() == 0).count();
            
            Map<String, Object> result = new HashMap<>();
            result.put("list", shopList);
            result.put("total", total);
            result.put("active", activeCount);
            result.put("closed", closedCount);
            result.put("newThisMonth", 0); // 可扩展统计本月新增
            
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取店铺列表失败：" + e.getMessage());
        }
    }

    /**
     * 管理员：获取店铺详情
     */
    @GetMapping("/admin/shop/{id}")
    public Result<Map<String, Object>> getAdminShopDetail(@PathVariable Long id) {
        return getShopDetail(id);
    }

    /**
     * 管理员：更新店铺信息
     */
    @PutMapping("/admin/shop/{id}")
    public Result<String> updateShop(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        try {
            User farmer = userService.getById(id);
            if (farmer == null || farmer.getUserType() != 2) {
                return Result.error("店铺不存在");
            }
            
            // 更新字段
            if (data.containsKey("shopName")) {
                farmer.setShopName((String) data.get("shopName"));
            }
            if (data.containsKey("nickname")) {
                farmer.setNickname((String) data.get("nickname"));
            }
            if (data.containsKey("location")) {
                farmer.setLocation((String) data.get("location"));
            }
            if (data.containsKey("mainCategory")) {
                farmer.setMainCategory((String) data.get("mainCategory"));
            }
            if (data.containsKey("tags")) {
                farmer.setTags((String) data.get("tags"));
            }
            if (data.containsKey("description")) {
                farmer.setDescription((String) data.get("description"));
            }
            if (data.containsKey("rating")) {
                farmer.setRating(Double.valueOf(data.get("rating").toString()));
            }
            if (data.containsKey("status")) {
                farmer.setStatus((Integer) data.get("status"));
            }
            
            userService.updateById(farmer);
            return Result.success("更新成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("更新失败：" + e.getMessage());
        }
    }

    /**
     * 管理员：切换店铺状态
     */
    @PutMapping("/admin/shop/{id}/status")
    public Result<String> toggleShopStatus(@PathVariable Long id, @RequestBody Map<String, Integer> data) {
        try {
            User farmer = userService.getById(id);
            if (farmer == null || farmer.getUserType() != 2) {
                return Result.error("店铺不存在");
            }
            
            Integer status = data.get("status");
            if (status == null) {
                return Result.error("状态不能为空");
            }
            
            farmer.setStatus(status);
            userService.updateById(farmer);
            return Result.success("操作成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("操作失败：" + e.getMessage());
        }
    }

    /**
     * 管理员：获取店铺统计
     */
    @GetMapping("/admin/shop/stats")
    public Result<Map<String, Object>> getShopStats() {
        try {
            List<User> farmers = userService.getFarmers();
            
            long total = farmers.size();
            long active = farmers.stream().filter(f -> f.getStatus() == 1).count();
            long closed = farmers.stream().filter(f -> f.getStatus() == 0).count();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("total", total);
            stats.put("active", active);
            stats.put("closed", closed);
            stats.put("newThisMonth", 0);
            
            return Result.success(stats);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取统计失败：" + e.getMessage());
        }
    }

    /**
     * 管理员：删除店铺
     */
    @DeleteMapping("/admin/shop/{id}")
    public Result<String> deleteShop(@PathVariable Long id) {
        try {
            User farmer = userService.getById(id);
            if (farmer == null || farmer.getUserType() != 2) {
                return Result.error("店铺不存在");
            }
            
            // 逻辑删除 - 使用 removeById 触发 MyBatis-Plus 逻辑删除
            boolean result = userService.removeById(id);
            if (result) {
                return Result.success("删除成功");
            } else {
                return Result.error("删除失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("删除失败：" + e.getMessage());
        }
    }
}
