package com.rural.ecommerce.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rural.ecommerce.entity.Category;

import java.util.List;

public interface CategoryService extends IService<Category> {
    
    /**
     * 获取所有启用的分类（按排序号排序）
     */
    List<Category> getActiveCategories();
    
    /**
     * 获取分类列表（包含商品数量统计）
     */
    List<Category> getCategoriesWithProductCount();
}
