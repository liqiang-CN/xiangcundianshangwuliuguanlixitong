package com.rural.ecommerce.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rural.ecommerce.entity.ProductReview;
import com.rural.ecommerce.entity.ReviewReport;
import com.rural.ecommerce.vo.ReviewReportVO;
import com.rural.ecommerce.vo.ReviewVO;

import java.util.List;
import java.util.Map;

public interface ProductReviewService {
    
    List<ReviewVO> getProductReviews(Long productId, Integer page, Integer size, String filter);
    
    Double getAverageRating(Long productId);
    
    Integer getReviewCount(Long productId);
    
    Map<String, Object> getRatingStats(Long productId);
    
    boolean addReview(ProductReview review);
    
    boolean reportReview(Long reviewId, Long reporterId, Integer reportType, String reportContent);
    
    // 管理员接口
    Page<ReviewVO> getAdminReviewList(Integer page, Integer size, Long productId, Long userId, Integer status, String keyword);
    
    ReviewVO getReviewDetail(Long id);
    
    boolean deleteReview(Long id);
    
    boolean batchDeleteReviews(List<Long> ids);
    
    Map<String, Object> getReviewStats();
    
    Page<ReviewReportVO> getReportList(Integer page, Integer size, Integer status);
    
    boolean handleReport(Long reportId, Integer status, String adminRemark);
}
