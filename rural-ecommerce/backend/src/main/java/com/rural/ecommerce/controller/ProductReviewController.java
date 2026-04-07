package com.rural.ecommerce.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rural.ecommerce.common.Result;
import com.rural.ecommerce.entity.ProductReview;
import com.rural.ecommerce.entity.User;
import com.rural.ecommerce.service.ProductReviewService;
import com.rural.ecommerce.service.UserService;
import com.rural.ecommerce.util.JwtUtil;
import com.rural.ecommerce.vo.ReviewVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/review")
public class ProductReviewController {

    @Autowired
    private ProductReviewService productReviewService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private Long getCurrentUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            return null;
        }
        String jwt = token.replace("Bearer ", "");
        return jwtUtil.getUserId(jwt);
    }

    // 获取商品评价列表
    @GetMapping("/product/{productId}")
    public Result<List<ReviewVO>> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String filter) {
        List<ReviewVO> list = productReviewService.getProductReviews(productId, page, size, filter);
        return Result.success(list);
    }

    // 获取商品评价统计
    @GetMapping("/stats/{productId}")
    public Result<Map<String, Object>> getReviewStats(@PathVariable Long productId) {
        Map<String, Object> stats = productReviewService.getRatingStats(productId);
        return Result.success(stats);
    }

    // 发布评价
    @PostMapping("/add")
    public Result<String> addReview(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("请先登录");
        }

        try {
            ProductReview review = new ProductReview();
            review.setProductId(Long.valueOf(params.get("productId").toString()));
            review.setUserId(userId);
            if (params.get("orderId") != null) {
                review.setOrderId(Long.valueOf(params.get("orderId").toString()));
            }
            review.setRating(Integer.valueOf(params.get("rating").toString()));
            review.setContent((String) params.get("content"));

            // 处理图片 - 将数组转换为逗号分隔的字符串
            if (params.get("images") != null) {
                Object images = params.get("images");
                if (images instanceof java.util.List && !((java.util.List<?>) images).isEmpty()) {
                    java.util.List<?> imageList = (java.util.List<?>) images;
                    String imagesStr = String.join(",", imageList.stream()
                            .map(Object::toString)
                            .collect(java.util.stream.Collectors.toList()));
                    review.setImages(imagesStr);
                }
            }

            // 处理视频
            if (params.get("video") != null) {
                review.setVideo((String) params.get("video"));
            }

            review.setStatus(1); // 直接发布
            review.setIsAnonymous(0);

            boolean success = productReviewService.addReview(review);
            if (success) {
                return Result.success("评价发布成功");
            } else {
                return Result.error("评价发布失败");
            }
        } catch (Exception e) {
            return Result.error("评价发布失败：" + e.getMessage());
        }
    }

    // 举报评价
    @PostMapping("/report")
    public Result<String> reportReview(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("请先登录");
        }

        try {
            Long reviewId = Long.valueOf(params.get("reviewId").toString());
            Integer reportType = Integer.valueOf(params.get("reportType").toString());
            String reportContent = (String) params.get("reportContent");

            boolean success = productReviewService.reportReview(reviewId, userId, reportType, reportContent);
            if (success) {
                return Result.success("举报成功，我们会尽快处理");
            } else {
                return Result.error("举报失败");
            }
        } catch (Exception e) {
            return Result.error("举报失败：" + e.getMessage());
        }
    }
}
