package com.rural.ecommerce.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.rural.ecommerce.common.Result;
import com.rural.ecommerce.entity.Product;
import com.rural.ecommerce.entity.ProductIntroduction;
import com.rural.ecommerce.entity.ProductReport;
import com.rural.ecommerce.entity.User;
import com.rural.ecommerce.entity.UserMessage;
import com.rural.ecommerce.mapper.ProductMapper;
import com.rural.ecommerce.mapper.ProductReportMapper;
import com.rural.ecommerce.mapper.UserMapper;
import com.rural.ecommerce.mapper.UserMessageMapper;
import com.rural.ecommerce.repository.ProductIntroductionRepository;
import com.rural.ecommerce.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/report")
public class ReportController {

    @Autowired
    private ProductReportMapper productReportMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserMessageMapper userMessageMapper;

    @Autowired
    private ProductIntroductionRepository productIntroductionRepository;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 提交商品举报
     */
    @PostMapping("/product")
    public Result<String> submitReport(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        System.out.println("收到举报请求，参数: " + params);

        // 从请求头获取token并解析用户ID
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Result.error("请先登录");
        }
        String token = authHeader.substring(7);
        Long userId;
        try {
            userId = jwtUtil.getUserId(token);
        } catch (Exception e) {
            return Result.error("登录已过期，请重新登录");
        }

        System.out.println("userId: " + userId);

        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 参数校验
        if (params.get("productId") == null) {
            return Result.error("商品ID不能为空");
        }
        if (params.get("reportType") == null) {
            return Result.error("举报类型不能为空");
        }
        if (params.get("reportContent") == null || ((String) params.get("reportContent")).trim().isEmpty()) {
            return Result.error("举报内容不能为空");
        }

        Long productId = Long.valueOf(params.get("productId").toString());
        Integer reportType = Integer.valueOf(params.get("reportType").toString());
        String reportContent = ((String) params.get("reportContent")).trim();
        String reportImages = params.get("reportImages") != null ? (String) params.get("reportImages") : "";
        
        System.out.println("接收到的reportImages: " + reportImages);

        // 获取商品信息
        Product product = productMapper.selectById(productId);
        if (product == null) {
            return Result.error("商品不存在");
        }

        // 获取农户信息
        User farmer = userMapper.selectById(product.getFarmerId());

        // 创建举报记录
        ProductReport report = new ProductReport();
        report.setProductId(productId);
        report.setProductName(product.getName());
        report.setProductImage(product.getMainImage());
        report.setProductPrice(product.getPrice());
        report.setProductUnit(product.getUnit());
        report.setFarmerId(product.getFarmerId());
        report.setFarmerName(farmer != null ? farmer.getNickname() : "");
        report.setReporterId(userId);
        report.setReporterName(user.getNickname());
        report.setReportType(reportType);
        report.setReportContent(reportContent);
        report.setReportImages(reportImages);
        report.setStatus(0); // 待处理

        int result = productReportMapper.insert(report);
        if (result > 0) {
            return Result.success("举报提交成功，我们会尽快处理");
        } else {
            return Result.error("举报提交失败");
        }
    }

    /**
     * 获取举报列表（管理员）
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> getReportList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status) {

        LambdaQueryWrapper<ProductReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductReport::getDeleted, 0);

        if (status != null) {
            wrapper.eq(ProductReport::getStatus, status);
        }

        wrapper.orderByDesc(ProductReport::getCreateTime);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<ProductReport> pageParam =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<ProductReport> resultPage =
                productReportMapper.selectPage(pageParam, wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("list", resultPage.getRecords());
        result.put("total", resultPage.getTotal());
        result.put("page", page);
        result.put("size", size);

        return Result.success(result);
    }

    /**
     * 获取举报详情（管理员）
     */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getReportDetail(@PathVariable Long id) {
        ProductReport report = productReportMapper.selectById(id);
        if (report == null || report.getDeleted() == 1) {
            return Result.error("举报记录不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("report", report);

        // 获取商品详情
        Product product = productMapper.selectById(report.getProductId());
        if (product != null) {
            Map<String, Object> productInfo = new HashMap<>();
            productInfo.put("id", product.getId());
            productInfo.put("name", product.getName());
            productInfo.put("mainImage", product.getMainImage());
            productInfo.put("images", product.getImages());
            productInfo.put("price", product.getPrice());
            productInfo.put("unit", product.getUnit());
            productInfo.put("description", product.getDescription());
            productInfo.put("categoryId", product.getCategoryId());
            productInfo.put("stock", product.getStock());
            productInfo.put("sales", product.getSales());
            productInfo.put("status", product.getStatus());
            productInfo.put("createTime", product.getCreateTime());
            // 获取商品介绍列表
            List<ProductIntroduction> introductionList = productIntroductionRepository.findByProductId(product.getId());
            productInfo.put("introductionList", introductionList);
            result.put("product", productInfo);
        }

        // 获取店铺/农户详情
        User farmer = userMapper.selectById(report.getFarmerId());
        if (farmer != null) {
            Map<String, Object> shopInfo = new HashMap<>();
            shopInfo.put("id", farmer.getId());
            shopInfo.put("username", farmer.getUsername());
            shopInfo.put("nickname", farmer.getNickname());
            shopInfo.put("shopName", farmer.getShopName());
            shopInfo.put("name", farmer.getShopName() != null ? farmer.getShopName() : farmer.getNickname());
            shopInfo.put("avatar", farmer.getAvatar());
            shopInfo.put("location", farmer.getLocation());
            shopInfo.put("description", farmer.getDescription());
            shopInfo.put("tags", farmer.getTags());
            shopInfo.put("mainCategory", farmer.getMainCategory());
            shopInfo.put("phone", farmer.getPhone());
            shopInfo.put("email", farmer.getEmail());
            shopInfo.put("rating", farmer.getRating());
            shopInfo.put("followers", farmer.getFollowers());
            shopInfo.put("status", farmer.getStatus());
            shopInfo.put("createTime", farmer.getCreateTime());
            shopInfo.put("background", farmer.getBackground());
            result.put("shop", shopInfo);
        }

        return Result.success(result);
    }

    /**
     * 处理举报 - 通过（删除商品）
     */
    @PutMapping("/{id}/approve")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> approveReport(@PathVariable Long id, @RequestBody Map<String, String> params) {
        System.out.println("收到通过举报请求，id: " + id);
        ProductReport report = productReportMapper.selectById(id);
        if (report == null || report.getDeleted() == 1) {
            return Result.error("举报记录不存在");
        }

        if (report.getStatus() != 0) {
            return Result.error("该举报已处理");
        }

        String adminRemark = params.get("adminRemark");
        System.out.println("处理举报，商品ID: " + report.getProductId() + ", 农户ID: " + report.getFarmerId());

        // 更新举报状态
        report.setStatus(1); // 已通过
        report.setAdminRemark(adminRemark);
        report.setHandleTime(LocalDateTime.now());
        productReportMapper.updateById(report);
        System.out.println("举报状态已更新为已通过");

        // 删除商品（逻辑删除）- 使用updateWrapper强制更新deleted字段
        Product product = productMapper.selectById(report.getProductId());
        if (product != null) {
            // 使用updateWrapper强制更新，绕过@TableLogic的逻辑
            LambdaUpdateWrapper<Product> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Product::getId, report.getProductId())
                        .set(Product::getDeleted, 1)
                        .set(Product::getStatus, 0); // 同时设置商品状态为下架
            productMapper.update(null, updateWrapper);
            System.out.println("商品已逻辑删除，商品ID: " + report.getProductId());
        } else {
            System.out.println("商品不存在，商品ID: " + report.getProductId());
        }

        // 发送消息通知给农户
        try {
            String reportTypeText = getReportTypeText(report.getReportType());
            UserMessage message = new UserMessage();
            message.setUserId(report.getFarmerId());
            message.setTitle("商品违规下架通知");
            message.setContent("您的商品【" + report.getProductName() + "】因" + reportTypeText +
                    "被用户举报，经平台核实已被下架。原因：" + report.getReportContent() +
                    (adminRemark != null && !adminRemark.isEmpty() ? "；管理员备注：" + adminRemark : "") +
                    "。如有疑问请联系客服。");
            message.setType(2); // 商品通知
            message.setIsRead(0);
            userMessageMapper.insert(message);
            System.out.println("消息通知已发送给农户，农户ID: " + report.getFarmerId());
        } catch (Exception e) {
            System.err.println("发送消息通知失败: " + e.getMessage());
            // 消息发送失败不影响主流程，继续返回成功
        }

        return Result.success("已处理，商品已下架");
    }

    /**
     * 处理举报 - 拒绝
     */
    @PutMapping("/{id}/reject")
    public Result<String> rejectReport(@PathVariable Long id, @RequestBody Map<String, String> params) {
        ProductReport report = productReportMapper.selectById(id);
        if (report == null || report.getDeleted() == 1) {
            return Result.error("举报记录不存在");
        }

        if (report.getStatus() != 0) {
            return Result.error("该举报已处理");
        }

        String adminRemark = params.get("adminRemark");

        // 更新举报状态
        report.setStatus(2); // 已拒绝
        report.setAdminRemark(adminRemark);
        report.setHandleTime(LocalDateTime.now());
        productReportMapper.updateById(report);

        return Result.success("已拒绝该举报");
    }

    /**
     * 获取举报统计（管理员）
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getReportStats() {
        Map<String, Object> stats = new HashMap<>();

        // 待处理数量
        LambdaQueryWrapper<ProductReport> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(ProductReport::getStatus, 0).eq(ProductReport::getDeleted, 0);
        long pendingCount = productReportMapper.selectCount(pendingWrapper);

        // 今日举报数
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LambdaQueryWrapper<ProductReport> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.ge(ProductReport::getCreateTime, todayStart)
                   .eq(ProductReport::getDeleted, 0);
        long todayCount = productReportMapper.selectCount(todayWrapper);

        // 已通过数
        LambdaQueryWrapper<ProductReport> approvedWrapper = new LambdaQueryWrapper<>();
        approvedWrapper.eq(ProductReport::getStatus, 1).eq(ProductReport::getDeleted, 0);
        long approvedCount = productReportMapper.selectCount(approvedWrapper);

        // 已拒绝数
        LambdaQueryWrapper<ProductReport> rejectedWrapper = new LambdaQueryWrapper<>();
        rejectedWrapper.eq(ProductReport::getStatus, 2).eq(ProductReport::getDeleted, 0);
        long rejectedCount = productReportMapper.selectCount(rejectedWrapper);

        stats.put("pending", pendingCount);
        stats.put("today", todayCount);
        stats.put("approved", approvedCount);
        stats.put("rejected", rejectedCount);

        return Result.success(stats);
    }

    private String getReportTypeText(Integer reportType) {
        switch (reportType) {
            case 1: return "虚假信息";
            case 2: return "侵权";
            case 3: return "违禁品";
            case 4: return "其他";
            default: return "违规";
        }
    }
}
