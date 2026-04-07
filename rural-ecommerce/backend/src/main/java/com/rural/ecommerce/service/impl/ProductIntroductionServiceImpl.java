package com.rural.ecommerce.service.impl;

import com.rural.ecommerce.entity.ProductIntroduction;
import com.rural.ecommerce.repository.ProductIntroductionRepository;
import com.rural.ecommerce.service.ProductIntroductionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 商品介绍服务实现类
 */
@Service
@RequiredArgsConstructor
public class ProductIntroductionServiceImpl implements ProductIntroductionService {

    private final ProductIntroductionRepository productIntroductionRepository;

    @Override
    public List<ProductIntroduction> findByProductId(Long productId) {
        return productIntroductionRepository.findByProductId(productId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchSave(Long productId, List<ProductIntroduction> introductions) {
        // 先删除旧的介绍
        productIntroductionRepository.deleteByProductId(productId);

        // 保存新的介绍
        if (introductions != null && !introductions.isEmpty()) {
            for (int i = 0; i < introductions.size(); i++) {
                ProductIntroduction intro = introductions.get(i);
                intro.setId(null); // 确保是新增
                intro.setProductId(productId);
                intro.setSortOrder(i);
                productIntroductionRepository.insert(intro);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByProductId(Long productId) {
        productIntroductionRepository.deleteByProductId(productId);
    }
}
