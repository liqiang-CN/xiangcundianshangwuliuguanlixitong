package com.rural.ecommerce.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rural.ecommerce.common.Result;
import com.rural.ecommerce.entity.ProductReview;
import com.rural.ecommerce.service.ProductReviewService;
import com.rural.ecommerce.vo.ReviewReportVO;
import com.rural.ecommerce.vo.ReviewVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/review")
public class AdminReviewController {

    @Autowired
    private ProductReviewService productReviewService;

    // 获取评价列表
    @GetMapping("/list")
    public Result<Page<ReviewVO>> getReviewList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        Page<ReviewVO> result = productReviewService.getAdminReviewList(page, size, productId, userId, status, keyword);
        return Result.success(result);
    }

    // 获取评价详情
    @GetMapping("/detail/{id}")
    public Result<ReviewVO> getReviewDetail(@PathVariable Long id) {
        ReviewVO review = productReviewService.getReviewDetail(id);
        if (review == null) {
            return Result.error("评价不存在");
        }
        return Result.success(review);
    }

    // 删除评价
    @PostMapping("/delete/{id}")
    public Result<String> deleteReview(@PathVariable Long id) {
        boolean success = productReviewService.deleteReview(id);
        if (success) {
            return Result.success("删除成功");
        } else {
            return Result.error("删除失败");
        }
    }

    // 批量删除评价
    @PostMapping("/batch-delete")
    public Result<String> batchDeleteReviews(@RequestBody Map<String, List<Long>> params) {
        List<Long> ids = params.get("ids");
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要删除的评价");
        }
        boolean success = productReviewService.batchDeleteReviews(ids);
        if (success) {
            return Result.success("批量删除成功");
        } else {
            return Result.error("批量删除失败");
        }
    }

    // 获取评价统计
    @GetMapping("/stats")
    public Result<Map<String, Object>> getReviewStats() {
        Map<String, Object> stats = productReviewService.getReviewStats();
        return Result.success(stats);
    }

    // 获取举报列表
    @GetMapping("/report/list")
    public Result<Page<ReviewReportVO>> getReportList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status) {
        Page<ReviewReportVO> result = productReviewService.getReportList(page, size, status);
        return Result.success(result);
    }

    // 处理举报
    @PostMapping("/report/handle")
    public Result<String> handleReport(@RequestBody Map<String, Object> params) {
        Long reportId = Long.valueOf(params.get("reportId").toString());
        Integer status = Integer.valueOf(params.get("status").toString());
        String adminRemark = (String) params.get("adminRemark");

        boolean success = productReviewService.handleReport(reportId, status, adminRemark);
        if (success) {
            return Result.success("处理成功");
        } else {
            return Result.error("处理失败");
        }
    }
}
