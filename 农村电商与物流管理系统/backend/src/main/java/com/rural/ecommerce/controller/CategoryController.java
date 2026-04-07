package com.rural.ecommerce.controller;

import com.rural.ecommerce.common.Result;
import com.rural.ecommerce.entity.Category;
import com.rural.ecommerce.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/category")
public class CategoryController {
    
    @Autowired
    private CategoryService categoryService;
    
    /**
     * 获取分类列表
     */
    @GetMapping("/list")
    public Result<List<Category>> getCategoryList() {
        List<Category> list = categoryService.getCategoriesWithProductCount();
        return Result.success(list);
    }
    
    /**
     * 获取分类详情
     */
    @GetMapping("/{id}")
    public Result<Category> getCategoryDetail(@PathVariable Long id) {
        Category category = categoryService.getById(id);
        if (category == null) {
            return Result.error("分类不存在");
        }
        return Result.success(category);
    }
    
    /**
     * 添加分类
     */
    @PostMapping
    public Result<Category> addCategory(@RequestBody Category category) {
        // 设置默认值
        if (category.getStatus() == null) {
            category.setStatus(1);
        }
        if (category.getSort() == null) {
            category.setSort(0);
        }
        if (category.getLevel() == null) {
            category.setLevel(1);
        }
        
        boolean success = categoryService.save(category);
        if (success) {
            return Result.success(category);
        } else {
            return Result.error("添加失败");
        }
    }
    
    /**
     * 更新分类
     */
    @PutMapping("/{id}")
    public Result<Category> updateCategory(@PathVariable Long id, @RequestBody Category category) {
        Category existing = categoryService.getById(id);
        if (existing == null) {
            return Result.error("分类不存在");
        }
        
        category.setId(id);
        boolean success = categoryService.updateById(category);
        if (success) {
            return Result.success(categoryService.getById(id));
        } else {
            return Result.error("更新失败");
        }
    }
    
    /**
     * 删除分类
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteCategory(@PathVariable Long id) {
        Category category = categoryService.getById(id);
        if (category == null) {
            return Result.error("分类不存在");
        }
        
        boolean success = categoryService.removeById(id);
        if (success) {
            return Result.success("删除成功");
        } else {
            return Result.error("删除失败");
        }
    }
    
    /**
     * 切换分类状态
     */
    @PutMapping("/{id}/status")
    public Result<String> toggleStatus(@PathVariable Long id, @RequestBody Map<String, Integer> data) {
        Category category = categoryService.getById(id);
        if (category == null) {
            return Result.error("分类不存在");
        }
        
        Integer status = data.get("status");
        if (status == null) {
            return Result.error("状态不能为空");
        }
        
        category.setStatus(status);
        boolean success = categoryService.updateById(category);
        if (success) {
            return Result.success("操作成功");
        } else {
            return Result.error("操作失败");
        }
    }
    
    /**
     * 获取分类统计
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getCategoryStats() {
        Map<String, Object> stats = new HashMap<>();
        
        long total = categoryService.count();
        long active = categoryService.lambdaQuery()
                .eq(Category::getStatus, 1)
                .eq(Category::getDeleted, 0)
                .count();
        
        stats.put("total", total);
        stats.put("active", active);
        
        return Result.success(stats);
    }
}
