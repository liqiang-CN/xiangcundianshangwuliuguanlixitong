package com.rural.ecommerce.service;

import com.rural.ecommerce.entity.ProductIntroduction;

import java.util.List;

/**
 * 商品介绍服务接口
 */
public interface ProductIntroductionService {

    /**
     * 根据商品ID查询商品介绍列表
     * @param productId 商品ID
     * @return 商品介绍列表
     */
    List<ProductIntroduction> findByProductId(Long productId);

    /**
     * 批量保存商品介绍
     * @param productId 商品ID
     * @param introductions 商品介绍列表
     */
    void batchSave(Long productId, List<ProductIntroduction> introductions);

    /**
     * 根据商品ID删除所有介绍
     * @param productId 商品ID
     */
    void deleteByProductId(Long productId);
}
