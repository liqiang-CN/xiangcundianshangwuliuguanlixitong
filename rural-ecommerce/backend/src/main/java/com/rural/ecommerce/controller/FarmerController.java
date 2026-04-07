package com.rural.ecommerce.controller;

import com.rural.ecommerce.common.Result;
import com.rural.ecommerce.entity.FarmerAddress;
import com.rural.ecommerce.entity.OrderInfo;
import com.rural.ecommerce.entity.Product;
import com.rural.ecommerce.entity.ProductIntroduction;
import com.rural.ecommerce.entity.ShopFollow;
import com.rural.ecommerce.entity.User;
import com.rural.ecommerce.entity.UserMessage;
import com.rural.ecommerce.service.OrderService;
import com.rural.ecommerce.service.ProductIntroductionService;
import com.rural.ecommerce.service.ProductService;
import com.rural.ecommerce.repository.ShopFollowRepository;
import com.rural.ecommerce.repository.UserMessageRepository;
import com.rural.ecommerce.util.JwtUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/farmer")
public class FarmerController {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductIntroductionService productIntroductionService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private com.rural.ecommerce.service.UserService userService;

    @Autowired
    private com.rural.ecommerce.mapper.FarmerAddressMapper farmerAddressMapper;

    @Autowired
    private ShopFollowRepository shopFollowRepository;

    @Autowired
    private UserMessageRepository userMessageRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private Long getCurrentUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            return jwtUtil.getUserId(token);
        }
        return null;
    }

    // 获取农户统计数据
    @GetMapping("/stats")
    public Result<Map<String, Object>> getFarmerStats(HttpServletRequest request) {
        try {
            Long farmerId = getCurrentUserId(request);
            if (farmerId == null) {
                return Result.error("请先登录");
            }

            Map<String, Object> stats = new HashMap<>();

            // 今日收入
            BigDecimal todayIncome = orderService.getTodayIncome(farmerId);
            stats.put("todayIncome", todayIncome);

            // 今日订单数
            int todayOrders = orderService.getTodayOrderCount(farmerId);
            stats.put("todayOrders", todayOrders);

            // 在售商品数
            int onSaleProducts = productService.getOnSaleCount(farmerId);
            stats.put("onSaleProducts", onSaleProducts);

            // 商品浏览量（模拟数据）
            stats.put("productViews", 1280);

            // 总收入
            BigDecimal totalIncome = orderService.getTotalIncome(farmerId);
            stats.put("totalIncome", totalIncome);

            // 总订单数
            int totalOrders = orderService.getTotalOrderCount(farmerId);
            stats.put("totalOrders", totalOrders);

            // 待处理订单（待发货）
            int pendingOrders = orderService.getPendingOrderCount(farmerId);
            stats.put("pendingOrders", pendingOrders);

            // 待收货订单（已发货，等待用户确认收货）
            int shippedOrders = orderService.getShippedOrderCount(farmerId);
            stats.put("shippedOrders", shippedOrders);

            // 已完成订单（用户确认收货）
            int completedOrders = orderService.getCompletedOrderCount(farmerId);
            stats.put("completedOrders", completedOrders);

            // 本月收入
            BigDecimal monthIncome = orderService.getMonthIncome(farmerId);
            stats.put("monthIncome", monthIncome);

            return Result.success(stats);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取统计数据失败：" + e.getMessage());
        }
    }

    // 获取农户商品列表
    @GetMapping("/products")
    public Result<List<Product>> getFarmerProducts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            HttpServletRequest request) {
        Long farmerId = getCurrentUserId(request);
        if (farmerId == null) {
            return Result.error("请先登录");
        }

        List<Product> list = productService.getFarmerProducts(farmerId, page, size, keyword, status);
        return Result.success(list);
    }

    // 创建商品
    @PostMapping("/product")
    public Result<Product> createProduct(@RequestBody Product product, HttpServletRequest request) {
        Long farmerId = getCurrentUserId(request);
        if (farmerId == null) {
            return Result.error("请先登录");
        }

        product.setFarmerId(farmerId);
        product.setCreateTime(LocalDateTime.now());
        product.setUpdateTime(LocalDateTime.now());

        boolean success = productService.save(product);
        if (success) {
            // 保存商品介绍
            List<ProductIntroduction> introductionList = product.getIntroductionList();
            if (introductionList != null && !introductionList.isEmpty()) {
                productIntroductionService.batchSave(product.getId(), introductionList);
            }

            // 给关注该店铺的用户发送上新消息
            sendNewProductNotification(farmerId, product);

            return Result.success(product);
        }
        return Result.error("创建商品失败");
    }

    // 发送店铺上新通知
    private void sendNewProductNotification(Long farmerId, Product product) {
        try {
            // 获取店铺信息
            User farmer = userService.getById(farmerId);
            String shopName = farmer != null && farmer.getShopName() != null
                    ? farmer.getShopName()
                    : (farmer != null ? farmer.getUsername() : "店铺");

            // 获取关注该店铺的所有用户
            List<ShopFollow> followers = shopFollowRepository.findByShopId(farmerId);

            // 给每个关注者发送消息
            for (ShopFollow follow : followers) {
                UserMessage message = new UserMessage();
                message.setUserId(follow.getUserId());
                message.setTitle("您关注的店铺上新啦！");
                message.setContent("「" + shopName + "」刚刚上架了新商品「" + product.getName() + "」，快来看看吧！");
                message.setType(2); // 商品通知
                message.setIsRead(0);
                message.setCreateTime(LocalDateTime.now());
                userMessageRepository.insert(message);
            }
        } catch (Exception e) {
            // 发送通知失败不影响商品创建
            System.err.println("发送上新通知失败: " + e.getMessage());
        }
    }

    // 更新商品
    @PutMapping("/product/{id}")
    public Result<Product> updateProduct(@PathVariable Long id, @RequestBody Product product, HttpServletRequest request) {
        Long farmerId = getCurrentUserId(request);
        if (farmerId == null) {
            return Result.error("请先登录");
        }

        // 验证商品是否属于当前农户
        Product existing = productService.getById(id);
        if (existing == null || !existing.getFarmerId().equals(farmerId)) {
            return Result.error("商品不存在或无权限");
        }

        // 只更新非 null 的字段，避免覆盖原有数据
        Product updateProduct = new Product();
        updateProduct.setId(id);
        updateProduct.setUpdateTime(LocalDateTime.now());
        
        if (product.getName() != null) {
            updateProduct.setName(product.getName());
        }
        if (product.getDescription() != null) {
            updateProduct.setDescription(product.getDescription());
        }
        if (product.getPrice() != null) {
            updateProduct.setPrice(product.getPrice());
        }
        if (product.getUnit() != null) {
            updateProduct.setUnit(product.getUnit());
        }
        if (product.getStock() != null) {
            updateProduct.setStock(product.getStock());
        }
        if (product.getMainImage() != null) {
            updateProduct.setMainImage(product.getMainImage());
        }
        if (product.getImages() != null) {
            updateProduct.setImages(product.getImages());
        }
        if (product.getCategoryId() != null) {
            updateProduct.setCategoryId(product.getCategoryId());
        }
        if (product.getOrigin() != null) {
            updateProduct.setOrigin(product.getOrigin());
        }
        if (product.getStatus() != null) {
            updateProduct.setStatus(product.getStatus());
        }

        boolean success = productService.updateById(updateProduct);
        if (success) {
            // 更新商品介绍
            List<ProductIntroduction> introductionList = product.getIntroductionList();
            if (introductionList != null) {
                productIntroductionService.batchSave(id, introductionList);
            }
            return Result.success(updateProduct);
        }
        return Result.error("更新商品失败");
    }

    // 删除商品
    @DeleteMapping("/product/{id}")
    public Result<Void> deleteProduct(@PathVariable Long id, HttpServletRequest request) {
        Long farmerId = getCurrentUserId(request);
        if (farmerId == null) {
            return Result.error("请先登录");
        }

        // 验证商品是否属于当前农户
        Product existing = productService.getById(id);
        if (existing == null || !existing.getFarmerId().equals(farmerId)) {
            return Result.error("商品不存在或无权限");
        }

        boolean success = productService.removeById(id);
        if (success) {
            return Result.success(null);
        }
        return Result.error("删除商品失败");
    }

    // 修改商品状态
    @PutMapping("/product/{id}/status")
    public Result<Void> toggleProductStatus(@PathVariable Long id, @RequestBody Map<String, Integer> params, HttpServletRequest request) {
        Long farmerId = getCurrentUserId(request);
        if (farmerId == null) {
            return Result.error("请先登录");
        }

        // 验证商品是否属于当前农户
        Product existing = productService.getById(id);
        if (existing == null || !existing.getFarmerId().equals(farmerId)) {
            return Result.error("商品不存在或无权限");
        }

        Integer status = params.get("status");
        Product product = new Product();
        product.setId(id);
        product.setStatus(status);

        boolean success = productService.updateById(product);
        if (success) {
            return Result.success(null);
        }
        return Result.error("修改状态失败");
    }

    // 获取农户订单列表
    @GetMapping("/orders")
    public Result<List<OrderInfo>> getFarmerOrders(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletRequest request) {
        try {
            Long farmerId = getCurrentUserId(request);
            if (farmerId == null) {
                return Result.error("请先登录");
            }

            List<OrderInfo> list = orderService.getFarmerOrders(farmerId, page, size, status, keyword, startDate, endDate);
            return Result.success(list);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取订单列表失败：" + e.getMessage());
        }
    }

    // 获取订单详情
    @GetMapping("/order/{id}")
    public Result<OrderInfo> getFarmerOrderDetail(@PathVariable Long id, HttpServletRequest request) {
        Long farmerId = getCurrentUserId(request);
        if (farmerId == null) {
            return Result.error("请先登录");
        }

        OrderInfo order = orderService.getFarmerOrderDetail(id, farmerId);
        if (order == null) {
            return Result.error("订单不存在");
        }
        return Result.success(order);
    }

    // 发货
    @PostMapping("/order/{id}/ship")
    public Result<Void> shipOrder(@PathVariable Long id, @RequestBody Map<String, String> params, HttpServletRequest request) {
        Long farmerId = getCurrentUserId(request);
        if (farmerId == null) {
            return Result.error("请先登录");
        }

        String logisticsNo = params.get("logisticsNo");
        String logisticsCompany = params.get("logisticsCompany");
        String senderName = params.get("senderName");
        String senderPhone = params.get("senderPhone");
        String senderAddress = params.get("senderAddress");

        boolean success = orderService.shipOrder(id, farmerId, logisticsNo, logisticsCompany,
                                                  senderName, senderPhone, senderAddress);
        if (success) {
            return Result.success(null);
        }
        return Result.error("发货失败");
    }

    // 生成物流单号
    @GetMapping("/tracking-no")
    public Result<String> generateTrackingNo(@RequestParam String company, HttpServletRequest request) {
        Long farmerId = getCurrentUserId(request);
        if (farmerId == null) {
            return Result.error("请先登录");
        }

        String trackingNo = orderService.generateTrackingNo(company);
        return Result.success(trackingNo);
    }

    // 农户取消订单（待发货状态）
    @PutMapping("/order/{id}/cancel")
    public Result<Void> cancelOrder(@PathVariable Long id, HttpServletRequest request) {
        Long farmerId = getCurrentUserId(request);
        if (farmerId == null) {
            return Result.error("请先登录");
        }

        boolean success = orderService.cancelOrderByFarmer(id, farmerId);
        if (success) {
            return Result.success(null);
        }
        return Result.error("取消订单失败，订单不存在或状态不允许取消");
    }

    // 获取收入统计
    @GetMapping("/income")
    public Result<Map<String, Object>> getFarmerIncome(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletRequest request) {
        Long farmerId = getCurrentUserId(request);
        if (farmerId == null) {
            return Result.error("请先登录");
        }

        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1).toString();
        }
        if (endDate == null) {
            endDate = LocalDate.now().toString();
        }

        Map<String, Object> income = orderService.getFarmerIncomeStats(farmerId, startDate, endDate);
        return Result.success(income);
    }

    // 切换店铺状态
    @PutMapping("/shop/status")
    public Result<String> toggleShopStatus(@RequestBody Map<String, Integer> data, HttpServletRequest request) {
        try {
            Long farmerId = getCurrentUserId(request);
            if (farmerId == null) {
                return Result.error("请先登录");
            }

            Integer status = data.get("status");
            if (status == null) {
                return Result.error("状态不能为空");
            }

            // 更新用户状态
            com.rural.ecommerce.entity.User user = new com.rural.ecommerce.entity.User();
            user.setId(farmerId);
            user.setStatus(status);
            // 使用 userService 更新状态
            boolean result = userService.updateById(user);

            if (result) {
                return Result.success("操作成功");
            } else {
                return Result.error("操作失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("操作失败：" + e.getMessage());
        }
    }

    // ==================== 发货地址管理 ====================

    // 获取发货地址列表
    @GetMapping("/address")
    public Result<List<FarmerAddress>> getAddressList(HttpServletRequest request) {
        Long farmerId = getCurrentUserId(request);
        if (farmerId == null) {
            return Result.error("请先登录");
        }

        LambdaQueryWrapper<FarmerAddress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FarmerAddress::getFarmerId, farmerId);
        wrapper.orderByDesc(FarmerAddress::getIsDefault);
        wrapper.orderByDesc(FarmerAddress::getCreateTime);

        List<FarmerAddress> list = farmerAddressMapper.selectList(wrapper);
        return Result.success(list);
    }

    // 获取默认发货地址
    @GetMapping("/address/default")
    public Result<FarmerAddress> getDefaultAddress(HttpServletRequest request) {
        Long farmerId = getCurrentUserId(request);
        if (farmerId == null) {
            return Result.error("请先登录");
        }

        LambdaQueryWrapper<FarmerAddress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FarmerAddress::getFarmerId, farmerId);
        wrapper.eq(FarmerAddress::getIsDefault, 1);

        FarmerAddress address = farmerAddressMapper.selectOne(wrapper);
        return Result.success(address);
    }

    // 添加发货地址
    @PostMapping("/address")
    public Result<FarmerAddress> addAddress(@RequestBody FarmerAddress address, HttpServletRequest request) {
        Long farmerId = getCurrentUserId(request);
        if (farmerId == null) {
            return Result.error("请先登录");
        }

        address.setFarmerId(farmerId);
        address.setCreateTime(LocalDateTime.now());
        address.setUpdateTime(LocalDateTime.now());

        // 如果设置为默认地址，先将其他地址设为非默认
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            clearDefaultAddress(farmerId);
        }

        int rows = farmerAddressMapper.insert(address);
        if (rows > 0) {
            return Result.success(address);
        }
        return Result.error("添加地址失败");
    }

    // 修改发货地址
    @PutMapping("/address/{id}")
    public Result<FarmerAddress> updateAddress(@PathVariable Long id, @RequestBody FarmerAddress address, HttpServletRequest request) {
        Long farmerId = getCurrentUserId(request);
        if (farmerId == null) {
            return Result.error("请先登录");
        }

        // 验证地址是否属于当前农户
        FarmerAddress existing = farmerAddressMapper.selectById(id);
        if (existing == null || !existing.getFarmerId().equals(farmerId)) {
            return Result.error("地址不存在或无权限");
        }

        address.setId(id);
        address.setUpdateTime(LocalDateTime.now());

        // 如果设置为默认地址，先将其他地址设为非默认
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            clearDefaultAddress(farmerId);
        }

        int rows = farmerAddressMapper.updateById(address);
        if (rows > 0) {
            return Result.success(address);
        }
        return Result.error("修改地址失败");
    }

    // 删除发货地址
    @DeleteMapping("/address/{id}")
    public Result<Void> deleteAddress(@PathVariable Long id, HttpServletRequest request) {
        Long farmerId = getCurrentUserId(request);
        if (farmerId == null) {
            return Result.error("请先登录");
        }

        // 验证地址是否属于当前农户
        FarmerAddress existing = farmerAddressMapper.selectById(id);
        if (existing == null || !existing.getFarmerId().equals(farmerId)) {
            return Result.error("地址不存在或无权限");
        }

        int rows = farmerAddressMapper.deleteById(id);
        if (rows > 0) {
            return Result.success(null);
        }
        return Result.error("删除地址失败");
    }

    // 设置默认发货地址
    @PutMapping("/address/{id}/default")
    public Result<Void> setDefaultAddress(@PathVariable Long id, HttpServletRequest request) {
        Long farmerId = getCurrentUserId(request);
        if (farmerId == null) {
            return Result.error("请先登录");
        }

        // 验证地址是否属于当前农户
        FarmerAddress existing = farmerAddressMapper.selectById(id);
        if (existing == null || !existing.getFarmerId().equals(farmerId)) {
            return Result.error("地址不存在或无权限");
        }

        // 先将其他地址设为非默认
        clearDefaultAddress(farmerId);

        // 设置当前地址为默认
        FarmerAddress update = new FarmerAddress();
        update.setId(id);
        update.setIsDefault(1);
        update.setUpdateTime(LocalDateTime.now());

        int rows = farmerAddressMapper.updateById(update);
        if (rows > 0) {
            return Result.success(null);
        }
        return Result.error("设置默认地址失败");
    }

    // 辅助方法：清除默认地址
    private void clearDefaultAddress(Long farmerId) {
        FarmerAddress update = new FarmerAddress();
        update.setIsDefault(0);

        LambdaQueryWrapper<FarmerAddress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FarmerAddress::getFarmerId, farmerId);
        wrapper.eq(FarmerAddress::getIsDefault, 1);

        farmerAddressMapper.update(update, wrapper);
    }
}
