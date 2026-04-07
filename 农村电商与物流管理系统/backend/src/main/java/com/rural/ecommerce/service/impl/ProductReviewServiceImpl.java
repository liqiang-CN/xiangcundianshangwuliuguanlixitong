package com.rural.ecommerce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rural.ecommerce.entity.ProductReview;
import com.rural.ecommerce.entity.ReviewReport;
import com.rural.ecommerce.entity.User;
import com.rural.ecommerce.mapper.ProductReviewMapper;
import com.rural.ecommerce.mapper.ReviewReportMapper;
import com.rural.ecommerce.service.ProductReviewService;
import com.rural.ecommerce.service.UserService;
import com.rural.ecommerce.vo.ReviewReportVO;
import com.rural.ecommerce.vo.ReviewVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ProductReviewServiceImpl extends ServiceImpl<ProductReviewMapper, ProductReview> implements ProductReviewService {

    @Autowired
    private ProductReviewMapper productReviewMapper;

    @Autowired
    private ReviewReportMapper reviewReportMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public List<ReviewVO> getProductReviews(Long productId, Integer page, Integer size, String filter) {
        LambdaQueryWrapper<ProductReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductReview::getProductId, productId)
                .eq(ProductReview::getStatus, 1)
                .eq(ProductReview::getDeleted, 0)
                .orderByDesc(ProductReview::getCreateTime);

        // 根据筛选条件过滤
        if (filter != null && !filter.isEmpty()) {
            switch (filter) {
                case "good":
                    wrapper.ge(ProductReview::getRating, 4);
                    break;
                case "medium":
                    wrapper.eq(ProductReview::getRating, 3);
                    break;
                case "bad":
                    wrapper.le(ProductReview::getRating, 2);
                    break;
                case "media":
                    // 有图片或视频的评价
                    wrapper.and(w -> w.isNotNull(ProductReview::getImages)
                            .or().isNotNull(ProductReview::getVideo));
                    break;
            }
        }

        Page<ProductReview> pageParam = new Page<>(page, size);
        Page<ProductReview> reviewPage = productReviewMapper.selectPage(pageParam, wrapper);

        List<ReviewVO> reviewVOList = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (ProductReview review : reviewPage.getRecords()) {
            ReviewVO vo = new ReviewVO();
            vo.setId(review.getId());
            vo.setProductId(review.getProductId());
            vo.setUserId(review.getUserId());
            vo.setRating(review.getRating());
            vo.setContent(review.getContent());
            vo.setVideo(review.getVideo());
            vo.setCreateTime(review.getCreateTime());
            vo.setTime(review.getCreateTime().format(formatter));

            // 解析图片 - 逗号分隔的字符串转换为列表
            if (review.getImages() != null && !review.getImages().isEmpty()) {
                try {
                    // 先尝试按JSON解析（兼容旧数据）
                    if (review.getImages().startsWith("[")) {
                        List<String> images = objectMapper.readValue(review.getImages(), new TypeReference<List<String>>() {});
                        vo.setImages(images);
                    } else {
                        // 按逗号分隔解析
                        List<String> images = Arrays.asList(review.getImages().split(","));
                        vo.setImages(images);
                    }
                } catch (Exception e) {
                    // 解析失败时，把整个字符串作为单个图片
                    vo.setImages(Arrays.asList(review.getImages()));
                }
            } else {
                vo.setImages(new ArrayList<>());
            }

            // 获取用户信息
            User user = userService.getById(review.getUserId());
            if (user != null) {
                vo.setUserName(review.getIsAnonymous() != null && review.getIsAnonymous() == 1
                        ? "匿名用户" : user.getNickname() != null ? user.getNickname() : user.getUsername());
                vo.setUserAvatar(user.getAvatar());
            } else {
                vo.setUserName("匿名用户");
                vo.setUserAvatar("/images/default-avatar.jpg");
            }

            reviewVOList.add(vo);
        }

        return reviewVOList;
    }

    @Override
    public Double getAverageRating(Long productId) {
        return productReviewMapper.selectAverageRating(productId);
    }

    @Override
    public Integer getReviewCount(Long productId) {
        return productReviewMapper.selectReviewCount(productId);
    }

    @Override
    public Map<String, Object> getRatingStats(Long productId) {
        Map<String, Object> stats = new HashMap<>();

        Double avgRating = getAverageRating(productId);
        Integer totalCount = getReviewCount(productId);

        stats.put("average", avgRating != null ? String.format("%.1f", avgRating) : "0.0");
        stats.put("total", totalCount);

        // 获取各星级分布
        List<Map<String, Object>> distribution = productReviewMapper.selectRatingDistribution(productId);
        Map<Integer, Integer> ratingMap = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            ratingMap.put(i, 0);
        }

        for (Map<String, Object> item : distribution) {
            Integer rating = (Integer) item.get("rating");
            Long count = (Long) item.get("count");
            ratingMap.put(rating, count.intValue());
        }

        stats.put("distribution", ratingMap);

        return stats;
    }

    @Override
    public boolean addReview(ProductReview review) {
        return save(review);
    }

    @Override
    public boolean reportReview(Long reviewId, Long reporterId, Integer reportType, String reportContent) {
        ReviewReport report = new ReviewReport();
        report.setReviewId(reviewId);
        report.setReporterId(reporterId);
        report.setReportType(reportType);
        report.setReportContent(reportContent);
        report.setStatus(0);

        return reviewReportMapper.insert(report) > 0;
    }

    @Override
    public Page<ReviewVO> getAdminReviewList(Integer page, Integer size, Long productId, Long userId, Integer status, String keyword) {
        // 使用自定义SQL查询所有评价（包含已删除的）
        StringBuilder sql = new StringBuilder("SELECT * FROM product_review WHERE 1=1");
        
        // 条件筛选
        if (productId != null) {
            sql.append(" AND product_id = ").append(productId);
        }
        if (userId != null) {
            sql.append(" AND user_id = ").append(userId);
        }
        if (status != null) {
            sql.append(" AND status = ").append(status);
        }
        if (keyword != null && !keyword.isEmpty()) {
            sql.append(" AND content LIKE '%").append(keyword.replace("'", "''")).append("%'");
        }
        
        sql.append(" ORDER BY create_time DESC");
        
        // 分页
        int offset = (page - 1) * size;
        String countSql = sql.toString().replace("SELECT *", "SELECT COUNT(*)");
        sql.append(" LIMIT ").append(offset).append(", ").append(size);
        
        // 执行查询
        List<ProductReview> records = productReviewMapper.selectListBySql(sql.toString());
        Long total = productReviewMapper.selectCountBySql(countSql);

        List<ReviewVO> reviewVOList = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (ProductReview review : records) {
            ReviewVO vo = convertToReviewVO(review, formatter);
            reviewVOList.add(vo);
        }

        Page<ReviewVO> resultPage = new Page<>();
        resultPage.setCurrent(page);
        resultPage.setSize(size);
        resultPage.setTotal(total);
        resultPage.setRecords(reviewVOList);

        return resultPage;
    }

    @Override
    public ReviewVO getReviewDetail(Long id) {
        // 查询评价（包含已删除的）
        ProductReview review = productReviewMapper.selectByIdWithDeleted(id);
        if (review == null) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return convertToReviewVO(review, formatter);
    }

    @Override
    public boolean deleteReview(Long id) {
        ProductReview review = getById(id);
        if (review == null) {
            return false;
        }
        // 使用 update 方法并设置 deleted = 1，同时更新 status = 2（已删除）
        LambdaUpdateWrapper<ProductReview> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ProductReview::getId, id)
               .set(ProductReview::getDeleted, 1)
               .set(ProductReview::getStatus, 2);
        return update(wrapper);
    }

    @Override
    public boolean batchDeleteReviews(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        for (Long id : ids) {
            deleteReview(id);
        }
        return true;
    }

    @Override
    public Map<String, Object> getReviewStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 总评价数
        LambdaQueryWrapper<ProductReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductReview::getDeleted, 0);
        long totalCount = count(wrapper);
        stats.put("total", totalCount);
        
        // 待审核评价数
        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductReview::getDeleted, 0).eq(ProductReview::getStatus, 0);
        long pendingCount = count(wrapper);
        stats.put("pending", pendingCount);
        
        // 已发布评价数
        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductReview::getDeleted, 0).eq(ProductReview::getStatus, 1);
        long publishedCount = count(wrapper);
        stats.put("published", publishedCount);
        
        // 今日新增
        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductReview::getDeleted, 0)
                .ge(ProductReview::getCreateTime, java.time.LocalDateTime.now().withHour(0).withMinute(0).withSecond(0));
        long todayCount = count(wrapper);
        stats.put("today", todayCount);
        
        return stats;
    }

    @Override
    public Page<ReviewReportVO> getReportList(Integer page, Integer size, Integer status) {
        LambdaQueryWrapper<ReviewReport> wrapper = new LambdaQueryWrapper<>();
        
        // 只查询未删除的举报
        wrapper.eq(ReviewReport::getDeleted, 0);
        
        if (status != null) {
            wrapper.eq(ReviewReport::getStatus, status);
        }
        
        wrapper.orderByDesc(ReviewReport::getCreateTime);

        Page<ReviewReport> pageParam = new Page<>(page, size);
        Page<ReviewReport> reportPage = reviewReportMapper.selectPage(pageParam, wrapper);
        
        // 转换为VO并填充关联信息
        List<ReviewReportVO> voList = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        for (ReviewReport report : reportPage.getRecords()) {
            ReviewReportVO vo = new ReviewReportVO();
            vo.setId(report.getId());
            vo.setReviewId(report.getReviewId());
            vo.setReporterId(report.getReporterId());
            vo.setReportType(report.getReportType());
            vo.setReportContent(report.getReportContent());
            vo.setStatus(report.getStatus());
            vo.setAdminRemark(report.getAdminRemark());
            vo.setHandleTime(report.getHandleTime());
            vo.setCreateTime(report.getCreateTime());
            vo.setCreateTimeStr(report.getCreateTime().format(formatter));
            
            // 获取举报人信息
            User reporter = userService.getById(report.getReporterId());
            if (reporter != null) {
                vo.setReporterName(reporter.getNickname() != null ? reporter.getNickname() : reporter.getUsername());
                vo.setReporterAvatar(reporter.getAvatar());
            } else {
                vo.setReporterName("未知用户");
                vo.setReporterAvatar("/images/default-avatar.jpg");
            }
            
            // 获取被举报评价信息（包含已删除的）
            ProductReview review = productReviewMapper.selectByIdWithDeleted(report.getReviewId());
            if (review != null) {
                vo.setReviewUserId(review.getUserId());
                vo.setReviewRating(review.getRating());
                vo.setReviewContent(review.getContent());
                vo.setReviewVideo(review.getVideo());
                vo.setReviewCreateTime(review.getCreateTime());
                
                // 解析评价图片
                if (review.getImages() != null && !review.getImages().isEmpty()) {
                    try {
                        if (review.getImages().startsWith("[")) {
                            List<String> images = objectMapper.readValue(review.getImages(), new TypeReference<List<String>>() {});
                            vo.setReviewImages(images);
                        } else {
                            List<String> images = Arrays.asList(review.getImages().split(","));
                            vo.setReviewImages(images);
                        }
                    } catch (Exception e) {
                        vo.setReviewImages(Arrays.asList(review.getImages()));
                    }
                } else {
                    vo.setReviewImages(new ArrayList<>());
                }
                
                // 获取被举报评价的用户信息
                User reviewUser = userService.getById(review.getUserId());
                if (reviewUser != null) {
                    vo.setReviewUserName(review.getIsAnonymous() != null && review.getIsAnonymous() == 1
                            ? "匿名用户" : reviewUser.getNickname() != null ? reviewUser.getNickname() : reviewUser.getUsername());
                    vo.setReviewUserAvatar(reviewUser.getAvatar());
                } else {
                    vo.setReviewUserName("匿名用户");
                    vo.setReviewUserAvatar("/images/default-avatar.jpg");
                }
            }
            
            voList.add(vo);
        }
        
        Page<ReviewReportVO> resultPage = new Page<>();
        resultPage.setCurrent(reportPage.getCurrent());
        resultPage.setSize(reportPage.getSize());
        resultPage.setTotal(reportPage.getTotal());
        resultPage.setRecords(voList);
        
        return resultPage;
    }

    @Override
    public boolean handleReport(Long reportId, Integer status, String adminRemark) {
        ReviewReport report = reviewReportMapper.selectById(reportId);
        if (report == null) {
            return false;
        }
        
        report.setStatus(status);
        report.setAdminRemark(adminRemark);
        report.setHandleTime(java.time.LocalDateTime.now());
        
        // 如果举报通过，删除对应评价
        if (status == 1) {
            deleteReview(report.getReviewId());
        }
        
        return reviewReportMapper.updateById(report) > 0;
    }

    private ReviewVO convertToReviewVO(ProductReview review, DateTimeFormatter formatter) {
        ReviewVO vo = new ReviewVO();
        vo.setId(review.getId());
        vo.setProductId(review.getProductId());
        vo.setUserId(review.getUserId());
        vo.setRating(review.getRating());
        vo.setContent(review.getContent());
        vo.setVideo(review.getVideo());
        vo.setCreateTime(review.getCreateTime());
        vo.setTime(review.getCreateTime().format(formatter));
        vo.setStatus(review.getStatus());

        // 解析图片
        if (review.getImages() != null && !review.getImages().isEmpty()) {
            try {
                if (review.getImages().startsWith("[")) {
                    List<String> images = objectMapper.readValue(review.getImages(), new TypeReference<List<String>>() {});
                    vo.setImages(images);
                } else {
                    List<String> images = Arrays.asList(review.getImages().split(","));
                    vo.setImages(images);
                }
            } catch (Exception e) {
                vo.setImages(Arrays.asList(review.getImages()));
            }
        } else {
            vo.setImages(new ArrayList<>());
        }

        // 获取用户信息
        User user = userService.getById(review.getUserId());
        if (user != null) {
            vo.setUserName(review.getIsAnonymous() != null && review.getIsAnonymous() == 1
                    ? "匿名用户" : user.getNickname() != null ? user.getNickname() : user.getUsername());
            vo.setUserAvatar(user.getAvatar());
        } else {
            vo.setUserName("匿名用户");
            vo.setUserAvatar("/images/default-avatar.jpg");
        }

        return vo;
    }
}
