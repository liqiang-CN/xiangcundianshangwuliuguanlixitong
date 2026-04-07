package com.rural.ecommerce.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rural.ecommerce.common.Result;
import com.rural.ecommerce.entity.Banner;
import com.rural.ecommerce.entity.Category;
import com.rural.ecommerce.entity.Product;
import com.rural.ecommerce.entity.ProductIntroduction;
import com.rural.ecommerce.mapper.BannerMapper;
import com.rural.ecommerce.service.ProductIntroductionService;
import com.rural.ecommerce.service.ProductService;
import com.rural.ecommerce.vo.ProductVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductIntroductionService productIntroductionService;

    @Autowired
    private BannerMapper bannerMapper;
    
    // 获取商品列表
    @GetMapping("/list")
    public Result<List<ProductVO>> getProductList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String origin,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder) {
        List<ProductVO> list = productService.getProductVOList(page, size, categoryId, keyword, status,
                minPrice, maxPrice, origin, sortBy, sortOrder);
        return Result.success(list);
    }
    
    // 获取商品详情
    @GetMapping("/{id}")
    public Result<ProductVO> getProductDetail(@PathVariable Long id) {
        ProductVO productVO = productService.getProductVOById(id);
        if (productVO == null) {
            return Result.error("商品不存在");
        }
        return Result.success(productVO);
    }
    
    // 获取分类列表
    @GetMapping("/category")
    public Result<List<Category>> getCategories() {
        List<Category> list = productService.getCategories();
        return Result.success(list);
    }
    
    // 获取推荐商品
    @GetMapping("/recommend")
    public Result<List<Product>> getRecommendProducts() {
        List<Product> list = productService.getRecommendProducts();
        return Result.success(list);
    }
    
    // 获取热销商品
    @GetMapping("/hot")
    public Result<List<Product>> getHotProducts() {
        List<Product> list = productService.getHotProducts();
        return Result.success(list);
    }
    
    // 搜索商品
    @GetMapping("/search")
    public Result<List<Product>> searchProducts(@RequestParam String keyword) {
        List<Product> list = productService.searchProducts(keyword);
        return Result.success(list);
    }
    
    // 创建商品（管理员）
    @PostMapping
    public Result<Product> createProduct(@RequestBody Product product) {
        boolean success = productService.save(product);
        if (success) {
            // 保存商品介绍
            List<ProductIntroduction> introductionList = product.getIntroductionList();
            if (introductionList != null && !introductionList.isEmpty()) {
                productIntroductionService.batchSave(product.getId(), introductionList);
            }
            return Result.success(product);
        }
        return Result.error("创建商品失败");
    }
    
    // 更新商品（管理员）
    @PutMapping("/{id}")
    public Result<Product> updateProduct(@PathVariable Long id, @RequestBody Product product) {
        product.setId(id);
        boolean success = productService.updateById(product);
        if (success) {
            // 更新商品介绍
            List<ProductIntroduction> introductionList = product.getIntroductionList();
            if (introductionList != null) {
                productIntroductionService.batchSave(id, introductionList);
            }
            return Result.success(product);
        }
        return Result.error("更新商品失败");
    }
    
    // 删除商品（管理员）
    @DeleteMapping("/{id}")
    public Result<Void> deleteProduct(@PathVariable Long id) {
        boolean success = productService.removeById(id);
        if (success) {
            return Result.success(null);
        }
        return Result.error("删除商品失败");
    }
    
    // 修改商品状态（上架/下架）
    @PutMapping("/{id}/status")
    public Result<Void> toggleStatus(@PathVariable Long id, @RequestBody Map<String, Integer> params) {
        Integer status = params.get("status");
        Product product = new Product();
        product.setId(id);
        product.setStatus(status);
        boolean success = productService.updateById(product);
        if (success) {
            return Result.success(null);
        }
        return Result.error("修改状态失败");
    }

    // ==================== 轮播图接口 ====================

    // 获取轮播图列表（公开接口，无需登录）
    @GetMapping("/banners")
    public Result<List<Banner>> getBanners() {
        LambdaQueryWrapper<Banner> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Banner::getStatus, 1)
               .orderByAsc(Banner::getSort);
        List<Banner> list = bannerMapper.selectList(wrapper);
        return Result.success(list);
    }

    // ==================== 商品审核接口 ====================

    // 获取待审核商品列表
    @GetMapping("/review/pending")
    public Result<Map<String, Object>> getPendingReviewProducts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Map<String, Object> result = productService.getPendingReviewProducts(page, size);
        return Result.success(result);
    }

    // 获取已审核商品列表
    @GetMapping("/review/reviewed")
    public Result<Map<String, Object>> getReviewedProducts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Map<String, Object> result = productService.getReviewedProducts(page, size);
        return Result.success(result);
    }

    // 审核商品
    @PutMapping("/{id}/review")
    public Result<String> reviewProduct(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        Integer status = Integer.valueOf(params.get("status").toString());
        String remark = (String) params.get("remark");
        boolean success = productService.reviewProduct(id, status, remark);
        if (success) {
            return Result.success("审核完成");
        }
        return Result.error("审核失败");
    }
}
