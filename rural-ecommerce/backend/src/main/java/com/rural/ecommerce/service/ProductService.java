package com.rural.ecommerce.service;

import com.rural.ecommerce.entity.Category;
import com.rural.ecommerce.entity.Product;
import com.rural.ecommerce.vo.ProductVO;

import java.util.List;
import java.util.Map;

public interface ProductService {

    List<Product> getProductList(Integer page, Integer size, Integer categoryId, String keyword, Integer status,
                                  Double minPrice, Double maxPrice, String origin, String sortBy, String sortOrder);

    List<ProductVO> getProductVOList(Integer page, Integer size, Integer categoryId, String keyword, Integer status,
                                     Double minPrice, Double maxPrice, String origin, String sortBy, String sortOrder);

    Product getById(Long id);

    ProductVO getProductVOById(Long id);

    List<Category> getCategories();

    List<Product> getRecommendProducts();

    List<Product> getHotProducts();

    List<Product> searchProducts(String keyword);

    boolean save(Product product);

    boolean updateById(Product product);

    boolean removeById(Long id);

    // 农户相关方法
    int getOnSaleCount(Long farmerId);

    List<Product> getFarmerProducts(Long farmerId, Integer page, Integer size, String keyword, Integer status);
    
    // 店铺相关方法
    Map<String, Object> getShopStats(Long farmerId);

    List<Product> getShopProducts(Long farmerId, Integer page, Integer size, String sortBy);

    int getShopProductCount(Long farmerId);

    // 商品审核相关方法
    Map<String, Object> getPendingReviewProducts(Integer page, Integer size);

    Map<String, Object> getReviewedProducts(Integer page, Integer size);

    boolean reviewProduct(Long productId, Integer status, String remark);
}
