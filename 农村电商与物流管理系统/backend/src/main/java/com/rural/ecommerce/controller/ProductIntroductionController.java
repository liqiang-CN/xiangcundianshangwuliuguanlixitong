package com.rural.ecommerce.controller;

import com.rural.ecommerce.common.Result;
import com.rural.ecommerce.entity.ProductIntroduction;
import com.rural.ecommerce.service.ProductIntroductionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品介绍控制器
 */
@RestController
@RequestMapping("/api/product-introduction")
@RequiredArgsConstructor
public class ProductIntroductionController {

    private final ProductIntroductionService productIntroductionService;

    /**
     * 根据商品ID查询商品介绍列表
     * @param productId 商品ID
     * @return 商品介绍列表
     */
    @GetMapping("/product/{productId}")
    public Result<List<ProductIntroduction>> findByProductId(@PathVariable Long productId) {
        List<ProductIntroduction> list = productIntroductionService.findByProductId(productId);
        return Result.success(list);
    }

    /**
     * 批量保存商品介绍
     * @param productId 商品ID
     * @param introductions 商品介绍列表
     * @return 结果
     */
    @PostMapping("/product/{productId}")
    public Result<Void> batchSave(@PathVariable Long productId, @RequestBody List<ProductIntroduction> introductions) {
        productIntroductionService.batchSave(productId, introductions);
        return Result.success();
    }

    /**
     * 根据商品ID删除所有介绍
     * @param productId 商品ID
     * @return 结果
     */
    @DeleteMapping("/product/{productId}")
    public Result<Void> deleteByProductId(@PathVariable Long productId) {
        productIntroductionService.deleteByProductId(productId);
        return Result.success();
    }
}
