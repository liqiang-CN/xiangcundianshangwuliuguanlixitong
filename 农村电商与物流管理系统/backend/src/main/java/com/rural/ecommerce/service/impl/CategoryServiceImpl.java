package com.rural.ecommerce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rural.ecommerce.entity.Category;
import com.rural.ecommerce.mapper.CategoryMapper;
import com.rural.ecommerce.service.CategoryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {
    
    @Override
    public List<Category> getActiveCategories() {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getStatus, 1)
               .eq(Category::getDeleted, 0)
               .orderByAsc(Category::getSort);
        return list(wrapper);
    }
    
    @Override
    public List<Category> getCategoriesWithProductCount() {
        // 获取所有未删除的分类
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getDeleted, 0)
               .orderByAsc(Category::getSort);
        return list(wrapper);
    }
}
