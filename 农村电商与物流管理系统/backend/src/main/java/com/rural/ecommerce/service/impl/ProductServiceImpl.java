package com.rural.ecommerce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rural.ecommerce.entity.Category;
import com.rural.ecommerce.entity.Product;
import com.rural.ecommerce.entity.User;
import com.rural.ecommerce.mapper.ProductMapper;
import com.rural.ecommerce.service.ProductService;
import com.rural.ecommerce.service.UserService;
import com.rural.ecommerce.vo.ProductVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private com.rural.ecommerce.mapper.CategoryMapper categoryMapper;

    @Autowired
    private UserService userService;
    
    @Override
    public List<Product> getProductList(Integer page, Integer size, Integer categoryId, String keyword, Integer status,
                                         Double minPrice, Double maxPrice, String origin, String sortBy, String sortOrder) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();

        // 如果指定了状态，则按状态筛选；否则返回所有状态的商品
        if (status != null) {
            wrapper.eq(Product::getStatus, status);
        }

        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Product::getName, keyword);
        }

        // 价格筛选
        if (minPrice != null) {
            wrapper.ge(Product::getPrice, minPrice);
        }
        if (maxPrice != null) {
            wrapper.le(Product::getPrice, maxPrice);
        }

        // 产地筛选
        if (origin != null && !origin.isEmpty()) {
            wrapper.like(Product::getOrigin, origin);
        }

        // 排序
        if ("sales".equals(sortBy)) {
            if ("asc".equals(sortOrder)) {
                wrapper.orderByAsc(Product::getSales);
            } else {
                wrapper.orderByDesc(Product::getSales);
            }
        } else if ("price".equals(sortBy)) {
            if ("asc".equals(sortOrder)) {
                wrapper.orderByAsc(Product::getPrice);
            } else {
                wrapper.orderByDesc(Product::getPrice);
            }
        } else {
            // 默认按创建时间排序
            wrapper.orderByDesc(Product::getCreateTime);
        }

        wrapper.last("LIMIT " + (page - 1) * size + ", " + size);

        return productMapper.selectList(wrapper);
    }

    @Override
    public List<ProductVO> getProductVOList(Integer page, Integer size, Integer categoryId, String keyword, Integer status,
                                             Double minPrice, Double maxPrice, String origin, String sortBy, String sortOrder) {
        // 获取商品列表
        List<Product> products = getProductList(page, size, categoryId, keyword, status,
                minPrice, maxPrice, origin, sortBy, sortOrder);

        if (products.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取所有农户ID
        List<Long> farmerIds = products.stream()
                .map(Product::getFarmerId)
                .distinct()
                .collect(Collectors.toList());

        // 批量查询农户信息
        Map<Long, String> farmerNameMap = new HashMap<>();
        for (Long farmerId : farmerIds) {
            User farmer = userService.getById(farmerId);
            if (farmer != null) {
                farmerNameMap.put(farmerId, farmer.getNickname() != null ? farmer.getNickname() : farmer.getUsername());
            } else {
                farmerNameMap.put(farmerId, "未知农户");
            }
        }

        // 获取分类信息
        List<Category> categories = categoryMapper.selectList(null);
        Map<Long, String> categoryNameMap = categories.stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (k1, k2) -> k1));

        // 转换为VO
        List<ProductVO> voList = new ArrayList<>();
        for (Product product : products) {
            ProductVO vo = new ProductVO();
            BeanUtils.copyProperties(product, vo);
            vo.setFarmerName(farmerNameMap.getOrDefault(product.getFarmerId(), "未知农户"));
            vo.setCategoryName(categoryNameMap.getOrDefault(product.getCategoryId(), "未知分类"));
            // 获取商品介绍列表
            vo.setIntroductionList(productIntroductionRepository.findByProductId(product.getId()));
            voList.add(vo);
        }

        return voList;
    }

    @Override
    public List<Category> getCategories() {
        // 从数据库查询启用的分类
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getStatus, 1)
               .eq(Category::getDeleted, 0)
               .orderByAsc(Category::getSort);
        return categoryMapper.selectList(wrapper);
    }
    
    @Override
    public List<Product> getRecommendProducts() {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 1);
        wrapper.orderByDesc(Product::getSales);
        wrapper.last("LIMIT 8");
        return productMapper.selectList(wrapper);
    }
    
    @Override
    public List<Product> getHotProducts() {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 1);
        wrapper.orderByDesc(Product::getSales);
        wrapper.last("LIMIT 6");
        return productMapper.selectList(wrapper);
    }
    
    @Override
    public List<Product> searchProducts(String keyword) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 1);
        wrapper.like(Product::getName, keyword);
        wrapper.orderByDesc(Product::getSales);
        return productMapper.selectList(wrapper);
    }

    @Override
    public boolean removeById(Long id) {
        return super.removeById(id);
    }

    @Override
    public boolean updateById(Product product) {
        return super.updateById(product);
    }

    @Override
    public boolean save(Product product) {
        return super.save(product);
    }

    @Override
    public Product getById(Long id) {
        return super.getById(id);
    }

    @Autowired
    private com.rural.ecommerce.repository.ProductIntroductionRepository productIntroductionRepository;

    @Override
    public ProductVO getProductVOById(Long id) {
        Product product = super.getById(id);
        if (product == null) {
            return null;
        }

        ProductVO vo = new ProductVO();
        BeanUtils.copyProperties(product, vo);

        // 获取农户信息
        User farmer = userService.getById(product.getFarmerId());
        if (farmer != null) {
            vo.setFarmerName(farmer.getNickname() != null ? farmer.getNickname() : farmer.getUsername());
            vo.setFarmerAvatar(farmer.getAvatar());
            vo.setShopName(farmer.getShopName());
        } else {
            vo.setFarmerName("未知农户");
        }

        // 获取分类名称
        Category category = categoryMapper.selectById(product.getCategoryId());
        if (category != null) {
            vo.setCategoryName(category.getName());
        } else {
            vo.setCategoryName("未知分类");
        }

        // 获取商品介绍列表
        vo.setIntroductionList(productIntroductionRepository.findByProductId(id));

        return vo;
    }

    @Override
    public int getOnSaleCount(Long farmerId) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getFarmerId, farmerId);
        wrapper.eq(Product::getStatus, 1);
        return productMapper.selectCount(wrapper).intValue();
    }

    @Override
    public List<Product> getFarmerProducts(Long farmerId, Integer page, Integer size, String keyword, Integer status) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getFarmerId, farmerId);

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Product::getName, keyword);
        }

        if (status != null) {
            wrapper.eq(Product::getStatus, status);
        }

        wrapper.orderByDesc(Product::getCreateTime);
        wrapper.last("LIMIT " + (page - 1) * size + ", " + size);

        List<Product> list = productMapper.selectList(wrapper);
        
        // 为每个商品加载介绍数据
        for (Product product : list) {
            List<com.rural.ecommerce.entity.ProductIntroduction> introductions = 
                productIntroductionRepository.findByProductId(product.getId());
            product.setIntroductionList(introductions);
        }
        
        return list;
    }
    
    @Override
    public Map<String, Object> getShopStats(Long farmerId) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getFarmerId, farmerId);
        wrapper.eq(Product::getStatus, 1);
        
        List<Product> products = productMapper.selectList(wrapper);
        
        int productCount = products.size();
        int sales = products.stream().mapToInt(Product::getSales).sum();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("productCount", productCount);
        stats.put("sales", sales);
        
        return stats;
    }
    
    @Override
    public List<Product> getShopProducts(Long farmerId, Integer page, Integer size, String sortBy) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getFarmerId, farmerId);
        wrapper.eq(Product::getStatus, 1);
        
        // 根据排序方式排序
        if ("sales".equals(sortBy)) {
            wrapper.orderByDesc(Product::getSales);
        } else if ("price".equals(sortBy)) {
            wrapper.orderByAsc(Product::getPrice);
        } else if ("new".equals(sortBy)) {
            wrapper.orderByDesc(Product::getCreateTime);
        } else {
            // 默认排序
            wrapper.orderByDesc(Product::getSales);
        }
        
        wrapper.last("LIMIT " + (page - 1) * size + ", " + size);
        return productMapper.selectList(wrapper);
    }
    
    @Override
    public int getShopProductCount(Long farmerId) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getFarmerId, farmerId);
        wrapper.eq(Product::getStatus, 1);
        return productMapper.selectCount(wrapper).intValue();
    }

    @Override
    public Map<String, Object> getPendingReviewProducts(Integer page, Integer size) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        // reviewStatus = 0 表示待审核
        wrapper.eq(Product::getReviewStatus, 0);
        wrapper.eq(Product::getDeleted, 0);
        wrapper.orderByDesc(Product::getCreateTime);
        wrapper.last("LIMIT " + (page - 1) * size + ", " + size);

        List<Product> products = productMapper.selectList(wrapper);

        // 获取总数
        LambdaQueryWrapper<Product> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(Product::getReviewStatus, 0);
        countWrapper.eq(Product::getDeleted, 0);
        long total = productMapper.selectCount(countWrapper);

        // 转换为VO
        List<ProductVO> voList = convertToVOList(products);

        Map<String, Object> result = new HashMap<>();
        result.put("list", voList);
        result.put("total", total);
        return result;
    }

    @Override
    public Map<String, Object> getReviewedProducts(Integer page, Integer size) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        // reviewStatus = 1 或 2 表示已审核（通过或拒绝）
        wrapper.ne(Product::getReviewStatus, 0);
        wrapper.eq(Product::getDeleted, 0);
        wrapper.orderByDesc(Product::getUpdateTime);
        wrapper.last("LIMIT " + (page - 1) * size + ", " + size);

        List<Product> products = productMapper.selectList(wrapper);

        // 获取总数
        LambdaQueryWrapper<Product> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.ne(Product::getReviewStatus, 0);
        countWrapper.eq(Product::getDeleted, 0);
        long total = productMapper.selectCount(countWrapper);

        // 转换为VO
        List<ProductVO> voList = convertToVOList(products);

        Map<String, Object> result = new HashMap<>();
        result.put("list", voList);
        result.put("total", total);
        return result;
    }

    @Override
    public boolean reviewProduct(Long productId, Integer status, String remark) {
        Product product = new Product();
        product.setId(productId);
        product.setReviewStatus(status);
        return productMapper.updateById(product) > 0;
    }

    // 辅助方法：将Product列表转换为ProductVO列表
    private List<ProductVO> convertToVOList(List<Product> products) {
        if (products.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取所有农户ID
        List<Long> farmerIds = products.stream()
                .map(Product::getFarmerId)
                .distinct()
                .collect(Collectors.toList());

        // 批量查询农户信息
        Map<Long, String> farmerNameMap = new HashMap<>();
        for (Long farmerId : farmerIds) {
            User farmer = userService.getById(farmerId);
            if (farmer != null) {
                farmerNameMap.put(farmerId, farmer.getNickname() != null ? farmer.getNickname() : farmer.getUsername());
            } else {
                farmerNameMap.put(farmerId, "未知农户");
            }
        }

        // 获取分类信息
        List<Category> categories = categoryMapper.selectList(null);
        Map<Long, String> categoryNameMap = categories.stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (k1, k2) -> k1));

        // 转换为VO
        List<ProductVO> voList = new ArrayList<>();
        for (Product product : products) {
            ProductVO vo = new ProductVO();
            BeanUtils.copyProperties(product, vo);
            vo.setFarmerName(farmerNameMap.getOrDefault(product.getFarmerId(), "未知农户"));
            vo.setCategoryName(categoryNameMap.getOrDefault(product.getCategoryId(), "未知分类"));
            voList.add(vo);
        }

        return voList;
    }
}
