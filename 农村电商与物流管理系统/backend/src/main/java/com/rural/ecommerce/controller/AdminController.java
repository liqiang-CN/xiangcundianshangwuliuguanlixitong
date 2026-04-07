package com.rural.ecommerce.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rural.ecommerce.common.Result;
import com.rural.ecommerce.entity.User;
import com.rural.ecommerce.entity.OrderInfo;
import com.rural.ecommerce.entity.OrderItem;
import com.rural.ecommerce.entity.SysConfig;
import com.rural.ecommerce.entity.Logistics;
import com.rural.ecommerce.entity.LogisticsTrack;
import com.rural.ecommerce.entity.Banner;
import com.rural.ecommerce.service.UserService;
import com.rural.ecommerce.service.OrderService;
import com.rural.ecommerce.service.LogisticsService;
import com.rural.ecommerce.mapper.OrderInfoMapper;
import com.rural.ecommerce.mapper.OrderItemMapper;
import com.rural.ecommerce.mapper.SysConfigMapper;
import com.rural.ecommerce.mapper.LogisticsMapper;
import com.rural.ecommerce.mapper.LogisticsTrackMapper;
import com.rural.ecommerce.mapper.BannerMapper;
import com.rural.ecommerce.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/admin")
public class AdminController {
    
    @Autowired
    private UserService userService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private SysConfigMapper sysConfigMapper;

    @Autowired
    private LogisticsMapper logisticsMapper;

    @Autowired
    private LogisticsService logisticsService;

    @Autowired
    private LogisticsTrackMapper logisticsTrackMapper;

    @Autowired
    private BannerMapper bannerMapper;

    @Autowired
    private JwtUtil jwtUtil;
    
    // 管理员登录
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");
        
        // 管理员用户类型为 4
        User user = userService.login(username, password, 4);
        if (user == null) {
            return Result.error("用户名或密码错误");
        }
        
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getUserType());
        
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        
        // 构建用户信息
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("role", "admin");
        userInfo.put("permissions", new String[]{"*"}); // 管理员拥有所有权限
        
        result.put("userInfo", userInfo);
        
        return Result.success(result);
    }
    
    // 获取管理员信息
    @GetMapping("/info")
    public Result<Map<String, Object>> getAdminInfo(@RequestHeader("Authorization") String token) {
        String jwt = token.replace("Bearer ", "");
        Long userId = jwtUtil.getUserId(jwt);
        User user = userService.getById(userId);
        
        if (user == null || user.getUserType() != 4) {
            return Result.error("无权限访问");
        }
        
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("role", "admin");
        userInfo.put("permissions", new String[]{"*"});
        
        return Result.success(userInfo);
    }
    
    // ==================== 用户管理接口 ====================
    
    /**
     * 获取用户列表（普通用户 user_type = 1）
     */
    @GetMapping("/users")
    public Result<Map<String, Object>> getUserList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String phone) {
        
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<User> pageParam = 
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User> wrapper = 
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        
        // 只查询普通用户（user_type = 1）
        wrapper.eq(User::getUserType, 1)
               .eq(User::getDeleted, 0);
        
        if (nickname != null && !nickname.isEmpty()) {
            wrapper.like(User::getNickname, nickname);
        }
        if (phone != null && !phone.isEmpty()) {
            wrapper.like(User::getPhone, phone);
        }
        
        wrapper.orderByDesc(User::getCreateTime);
        
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<User> result = 
            userService.page(pageParam, wrapper);
        
        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getRecords());
        data.put("total", result.getTotal());
        data.put("page", result.getCurrent());
        data.put("size", result.getSize());
        
        return Result.success(data);
    }
    
    /**
     * 获取用户统计
     */
    @GetMapping("/users/stats")
    public Result<Map<String, Object>> getUserStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 总用户数（普通用户）
        long total = userService.lambdaQuery()
                .eq(User::getUserType, 1)
                .eq(User::getDeleted, 0)
                .count();
        
        // 今日新增
        java.time.LocalDateTime today = java.time.LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        long todayNew = userService.lambdaQuery()
                .eq(User::getUserType, 1)
                .eq(User::getDeleted, 0)
                .ge(User::getCreateTime, today)
                .count();
        
        // 活跃用户（状态为1）
        long active = userService.lambdaQuery()
                .eq(User::getUserType, 1)
                .eq(User::getStatus, 1)
                .eq(User::getDeleted, 0)
                .count();
        
        stats.put("total", total);
        stats.put("todayNew", todayNew);
        stats.put("active", active);
        stats.put("totalOrders", 0); // 暂时返回0，后续可以从订单表统计
        
        return Result.success(stats);
    }
    
    /**
     * 切换用户状态
     */
    @PutMapping("/users/{id}/status")
    public Result<String> toggleUserStatus(@PathVariable Long id, @RequestBody Map<String, Integer> data) {
        User user = userService.getById(id);
        if (user == null || user.getUserType() != 1) {
            return Result.error("用户不存在");
        }
        
        Integer status = data.get("status");
        if (status == null) {
            return Result.error("状态不能为空");
        }
        
        user.setStatus(status);
        boolean success = userService.updateById(user);
        
        if (success) {
            return Result.success("操作成功");
        } else {
            return Result.error("操作失败");
        }
    }
    
    /**
     * 获取用户详情
     */
    @GetMapping("/users/{id}")
    public Result<User> getUserDetail(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null || user.getUserType() != 1) {
            return Result.error("用户不存在");
        }
        return Result.success(user);
    }
    
    // ==================== 农户管理接口 ====================
    
    /**
     * 获取农户列表（农户 user_type = 2）
     */
    @GetMapping("/farmers")
    public Result<Map<String, Object>> getFarmerList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status) {
        
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<User> pageParam = 
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User> wrapper = 
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        
        // 只查询农户（user_type = 2）
        wrapper.eq(User::getUserType, 2)
               .eq(User::getDeleted, 0);
        
        if (name != null && !name.isEmpty()) {
            wrapper.and(w -> w.like(User::getNickname, name).or().like(User::getUsername, name));
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        
        wrapper.orderByDesc(User::getCreateTime);
        
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<User> result = 
            userService.page(pageParam, wrapper);
        
        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getRecords());
        data.put("total", result.getTotal());
        data.put("page", result.getCurrent());
        data.put("size", result.getSize());
        
        return Result.success(data);
    }
    
    /**
     * 获取农户统计
     */
    @GetMapping("/farmers/stats")
    public Result<Map<String, Object>> getFarmerStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 总农户数
        long total = userService.lambdaQuery()
                .eq(User::getUserType, 2)
                .eq(User::getDeleted, 0)
                .count();
        
        // 已认证（状态为1）
        long verified = userService.lambdaQuery()
                .eq(User::getUserType, 2)
                .eq(User::getStatus, 1)
                .eq(User::getDeleted, 0)
                .count();
        
        // 待审核（状态为0）
        long pending = userService.lambdaQuery()
                .eq(User::getUserType, 2)
                .eq(User::getStatus, 0)
                .eq(User::getDeleted, 0)
                .count();
        
        // 本月新增
        java.time.LocalDateTime firstDayOfMonth = java.time.LocalDateTime.now()
                .withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        long newThisMonth = userService.lambdaQuery()
                .eq(User::getUserType, 2)
                .eq(User::getDeleted, 0)
                .ge(User::getCreateTime, firstDayOfMonth)
                .count();
        
        stats.put("total", total);
        stats.put("verified", verified);
        stats.put("pending", pending);
        stats.put("newThisMonth", newThisMonth);
        
        return Result.success(stats);
    }
    
    /**
     * 切换农户状态
     */
    @PutMapping("/farmers/{id}/status")
    public Result<String> toggleFarmerStatus(@PathVariable Long id, @RequestBody Map<String, Integer> data) {
        User user = userService.getById(id);
        if (user == null || user.getUserType() != 2) {
            return Result.error("农户不存在");
        }
        
        Integer status = data.get("status");
        if (status == null) {
            return Result.error("状态不能为空");
        }
        
        user.setStatus(status);
        boolean success = userService.updateById(user);
        
        if (success) {
            return Result.success("操作成功");
        } else {
            return Result.error("操作失败");
        }
    }
    
    /**
     * 获取农户详情
     */
    @GetMapping("/farmers/{id}")
    public Result<User> getFarmerDetail(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null || user.getUserType() != 2) {
            return Result.error("农户不存在");
        }
        return Result.success(user);
    }

    // ==================== 配送员管理接口 ====================

    /**
     * 获取配送员列表（配送员 user_type = 3）
     */
    @GetMapping("/delivery-staff")
    public Result<Map<String, Object>> getDeliveryStaffList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Integer status) {

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<User> pageParam =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);

        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User> wrapper =
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();

        // 只查询配送员（user_type = 3）
        wrapper.eq(User::getUserType, 3)
               .eq(User::getDeleted, 0);

        if (nickname != null && !nickname.isEmpty()) {
            wrapper.like(User::getNickname, nickname);
        }
        if (phone != null && !phone.isEmpty()) {
            wrapper.like(User::getPhone, phone);
        }
        if (location != null && !location.isEmpty()) {
            wrapper.like(User::getLocation, location);
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }

        wrapper.orderByDesc(User::getCreateTime);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<User> result =
            userService.page(pageParam, wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getRecords());
        data.put("total", result.getTotal());
        data.put("page", result.getCurrent());
        data.put("size", result.getSize());

        return Result.success(data);
    }

    /**
     * 获取数据概览统计（Dashboard）
     */
    @GetMapping("/dashboard/stats")
    public Result<Map<String, Object>> getDashboardStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime yesterday = today.minusDays(1);

            // 1. 今日订单数
            LambdaQueryWrapper<OrderInfo> todayOrderWrapper = new LambdaQueryWrapper<>();
            todayOrderWrapper.ge(OrderInfo::getCreateTime, today);
            long todayOrders = orderInfoMapper.selectCount(todayOrderWrapper);

            // 昨日订单数（用于计算环比）
            LambdaQueryWrapper<OrderInfo> yesterdayOrderWrapper = new LambdaQueryWrapper<>();
            yesterdayOrderWrapper.ge(OrderInfo::getCreateTime, yesterday)
                    .lt(OrderInfo::getCreateTime, today);
            long yesterdayOrders = orderInfoMapper.selectCount(yesterdayOrderWrapper);
            double ordersChange = yesterdayOrders > 0 ?
                    Math.round((todayOrders - yesterdayOrders) * 100.0 / yesterdayOrders * 10) / 10.0 : 0;

            // 2. 今日交易额
            LambdaQueryWrapper<OrderInfo> todayAmountWrapper = new LambdaQueryWrapper<>();
            todayAmountWrapper.ge(OrderInfo::getCreateTime, today)
                    .eq(OrderInfo::getStatus, 3); // 已完成的订单
            BigDecimal todayAmount = orderInfoMapper.selectList(todayAmountWrapper)
                    .stream()
                    .map(o -> o.getPayAmount() != null ? o.getPayAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            LambdaQueryWrapper<OrderInfo> yesterdayAmountWrapper = new LambdaQueryWrapper<>();
            yesterdayAmountWrapper.ge(OrderInfo::getCreateTime, yesterday)
                    .lt(OrderInfo::getCreateTime, today)
                    .eq(OrderInfo::getStatus, 3);
            BigDecimal yesterdayAmount = orderInfoMapper.selectList(yesterdayAmountWrapper)
                    .stream()
                    .map(o -> o.getPayAmount() != null ? o.getPayAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            double amountChange = yesterdayAmount.compareTo(BigDecimal.ZERO) > 0 ?
                    Math.round(todayAmount.subtract(yesterdayAmount).multiply(new BigDecimal(100))
                            .divide(yesterdayAmount, 2, BigDecimal.ROUND_HALF_UP).doubleValue() * 10) / 10.0 : 0;

            // 3. 今日新增用户
            long todayNewUsers = userService.lambdaQuery()
                    .eq(User::getUserType, 1)
                    .eq(User::getDeleted, 0)
                    .ge(User::getCreateTime, today)
                    .count();
            long yesterdayNewUsers = userService.lambdaQuery()
                    .eq(User::getUserType, 1)
                    .eq(User::getDeleted, 0)
                    .ge(User::getCreateTime, yesterday)
                    .lt(User::getCreateTime, today)
                    .count();
            double usersChange = yesterdayNewUsers > 0 ?
                    Math.round((todayNewUsers - yesterdayNewUsers) * 100.0 / yesterdayNewUsers * 10) / 10.0 : 0;

            // 4. 待配送订单（待发货状态）
            LambdaQueryWrapper<OrderInfo> pendingDeliveryWrapper = new LambdaQueryWrapper<>();
            pendingDeliveryWrapper.eq(OrderInfo::getStatus, 1); // 待发货
            long pendingDelivery = orderInfoMapper.selectCount(pendingDeliveryWrapper);

            // 5. 订单状态分布
            Map<String, Long> orderStatusDist = new HashMap<>();
            LambdaQueryWrapper<OrderInfo> statusWrapper0 = new LambdaQueryWrapper<>();
            orderStatusDist.put("unpaid", orderInfoMapper.selectCount(statusWrapper0.eq(OrderInfo::getStatus, 0)));
            LambdaQueryWrapper<OrderInfo> statusWrapper1 = new LambdaQueryWrapper<>();
            orderStatusDist.put("unshipped", orderInfoMapper.selectCount(statusWrapper1.eq(OrderInfo::getStatus, 1)));
            LambdaQueryWrapper<OrderInfo> statusWrapper2 = new LambdaQueryWrapper<>();
            orderStatusDist.put("unreceived", orderInfoMapper.selectCount(statusWrapper2.eq(OrderInfo::getStatus, 2)));
            LambdaQueryWrapper<OrderInfo> statusWrapper3 = new LambdaQueryWrapper<>();
            orderStatusDist.put("completed", orderInfoMapper.selectCount(statusWrapper3.eq(OrderInfo::getStatus, 3)));
            LambdaQueryWrapper<OrderInfo> statusWrapper4 = new LambdaQueryWrapper<>();
            orderStatusDist.put("cancelled", orderInfoMapper.selectCount(statusWrapper4.eq(OrderInfo::getStatus, 4)));

            // 6. 热销商品排行（Top 6）
            List<Map<String, Object>> hotProducts = orderService.getHotProducts(6);

            // 7. 农户销售排行（Top 5）
            List<Map<String, Object>> topFarmers = orderService.getTopFarmers(5);

            // 8. 交易趋势（最近7天）
            List<Map<String, Object>> tradeTrend = orderService.getTradeTrend(7);

            // 组装结果
            Map<String, Object> todayStats = new HashMap<>();
            todayStats.put("orders", todayOrders);
            todayStats.put("ordersChange", ordersChange);
            todayStats.put("amount", todayAmount);
            todayStats.put("amountChange", amountChange);
            todayStats.put("newUsers", todayNewUsers);
            todayStats.put("usersChange", usersChange);
            todayStats.put("pendingDelivery", pendingDelivery);

            stats.put("today", todayStats);
            stats.put("orderStatus", orderStatusDist);
            stats.put("hotProducts", hotProducts);
            stats.put("topFarmers", topFarmers);
            stats.put("tradeTrend", tradeTrend);

            return Result.success(stats);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取统计数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取配送员统计
     */
    @GetMapping("/delivery-staff/stats")
    public Result<Map<String, Object>> getDeliveryStaffStats() {
        Map<String, Object> stats = new HashMap<>();

        // 总配送员数
        long total = userService.lambdaQuery()
                .eq(User::getUserType, 3)
                .eq(User::getDeleted, 0)
                .count();

        // 在职配送员（状态为1）
        long active = userService.lambdaQuery()
                .eq(User::getUserType, 3)
                .eq(User::getStatus, 1)
                .eq(User::getDeleted, 0)
                .count();

        // 今日配送订单数（从物流记录中统计今日签收的订单）
        java.time.LocalDateTime today = java.time.LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        long todayDelivery = 0; // 暂时返回0，后续可以从物流表统计

        stats.put("total", total);
        stats.put("active", active);
        stats.put("todayDelivery", todayDelivery);

        return Result.success(stats);
    }

    /**
     * 添加配送员
     */
    @PostMapping("/delivery-staff")
    public Result<String> addDeliveryStaff(@RequestBody User user) {
        // 检查手机号是否已存在
        User existUser = userService.lambdaQuery()
                .eq(User::getPhone, user.getPhone())
                .eq(User::getDeleted, 0)
                .one();
        if (existUser != null) {
            return Result.error("手机号已存在");
        }

        user.setUserType(3); // 配送员
        user.setStatus(1); // 默认在职
        user.setPassword("123456"); // 默认密码
        user.setCreateTime(java.time.LocalDateTime.now());
        user.setUpdateTime(java.time.LocalDateTime.now());

        boolean success = userService.save(user);
        if (success) {
            return Result.success("添加成功");
        } else {
            return Result.error("添加失败");
        }
    }

    /**
     * 更新配送员
     */
    @PutMapping("/delivery-staff/{id}")
    public Result<String> updateDeliveryStaff(@PathVariable Long id, @RequestBody User user) {
        User existUser = userService.getById(id);
        if (existUser == null || existUser.getUserType() != 3) {
            return Result.error("配送员不存在");
        }

        user.setId(id);
        user.setUpdateTime(java.time.LocalDateTime.now());
        // 不允许修改用户类型和敏感字段
        user.setUserType(null);
        user.setPassword(null);
        user.setCreateTime(null);

        boolean success = userService.updateById(user);
        if (success) {
            return Result.success("更新成功");
        } else {
            return Result.error("更新失败");
        }
    }

    /**
     * 切换配送员状态
     */
    @PutMapping("/delivery-staff/{id}/status")
    public Result<String> toggleDeliveryStaffStatus(@PathVariable Long id, @RequestBody Map<String, Integer> data) {
        User user = userService.getById(id);
        if (user == null || user.getUserType() != 3) {
            return Result.error("配送员不存在");
        }

        Integer status = data.get("status");
        if (status == null) {
            return Result.error("状态不能为空");
        }

        user.setStatus(status);
        boolean success = userService.updateById(user);

        if (success) {
            return Result.success("操作成功");
        } else {
            return Result.error("操作失败");
        }
    }

    /**
     * 删除配送员
     */
    @DeleteMapping("/delivery-staff/{id}")
    public Result<String> deleteDeliveryStaff(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null || user.getUserType() != 3) {
            return Result.error("配送员不存在");
        }

        user.setDeleted(1);
        user.setUpdateTime(java.time.LocalDateTime.now());
        boolean success = userService.updateById(user);

        if (success) {
            return Result.success("删除成功");
        } else {
            return Result.error("删除失败");
        }
    }

    /**
     * 获取配送员详情
     */
    @GetMapping("/delivery-staff/{id}")
    public Result<User> getDeliveryStaffDetail(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null || user.getUserType() != 3) {
            return Result.error("配送员不存在");
        }
        return Result.success(user);
    }

    // ==================== 系统配置接口 ====================

    /**
     * 获取系统配置
     */
    @GetMapping("/config")
    public Result<Map<String, String>> getConfig() {
        List<SysConfig> configList = sysConfigMapper.selectList(null);
        Map<String, String> configMap = new HashMap<>();
        for (SysConfig config : configList) {
            configMap.put(config.getConfigKey(), config.getConfigValue());
        }
        return Result.success(configMap);
    }

    /**
     * 更新系统配置
     */
    @PostMapping("/config")
    public Result<String> updateConfig(@RequestBody Map<String, String> configMap) {
        for (Map.Entry<String, String> entry : configMap.entrySet()) {
            LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysConfig::getConfigKey, entry.getKey());
            SysConfig config = sysConfigMapper.selectOne(wrapper);
            if (config != null) {
                config.setConfigValue(entry.getValue());
                sysConfigMapper.updateById(config);
            } else {
                config = new SysConfig();
                config.setConfigKey(entry.getKey());
                config.setConfigValue(entry.getValue());
                sysConfigMapper.insert(config);
            }
        }
        return Result.success("保存成功");
    }

    // ==================== 物流调度接口 ====================

    /**
     * 获取物流统计
     */
    @GetMapping("/logistics/stats")
    public Result<Map<String, Object>> getLogisticsStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 待揽件（status = 0）
        LambdaQueryWrapper<Logistics> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(Logistics::getStatus, 0);
        long pendingPickup = logisticsMapper.selectCount(pendingWrapper);
        
        // 运输中（status = 1）
        LambdaQueryWrapper<Logistics> transitWrapper = new LambdaQueryWrapper<>();
        transitWrapper.eq(Logistics::getStatus, 1);
        long inTransit = logisticsMapper.selectCount(transitWrapper);
        
        // 派送中（status = 2）
        LambdaQueryWrapper<Logistics> deliveringWrapper = new LambdaQueryWrapper<>();
        deliveringWrapper.eq(Logistics::getStatus, 2);
        long delivering = logisticsMapper.selectCount(deliveringWrapper);
        
        // 今日签收（status = 3 且 sign_time 是今天）
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LambdaQueryWrapper<Logistics> signedWrapper = new LambdaQueryWrapper<>();
        signedWrapper.eq(Logistics::getStatus, 3).ge(Logistics::getSignTime, today);
        long signed = logisticsMapper.selectCount(signedWrapper);
        
        stats.put("pendingPickup", pendingPickup);
        stats.put("inTransit", inTransit);
        stats.put("delivering", delivering);
        stats.put("signed", signed);
        
        return Result.success(stats);
    }

    /**
     * 获取运单列表
     */
    @GetMapping("/logistics/waybills")
    public Result<Map<String, Object>> getWaybillList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String waybillNo,
            @RequestParam(required = false) String receiverName,
            @RequestParam(required = false) Long deliveryId,
            @RequestParam(required = false) Integer status) {
        
        Page<Logistics> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Logistics> wrapper = new LambdaQueryWrapper<>();
        
        // 运单号筛选
        if (waybillNo != null && !waybillNo.isEmpty()) {
            wrapper.like(Logistics::getTrackingNo, waybillNo);
        }
        
        // 配送员筛选
        if (deliveryId != null) {
            wrapper.eq(Logistics::getDeliveryId, deliveryId);
        }
        
        // 状态筛选
        if (status != null) {
            wrapper.eq(Logistics::getStatus, status);
        }
        
        wrapper.orderByDesc(Logistics::getCreateTime);
        Page<Logistics> logisticsPage = logisticsMapper.selectPage(pageParam, wrapper);
        
        List<Map<String, Object>> waybillList = new ArrayList<>();
        for (Logistics logistics : logisticsPage.getRecords()) {
            Map<String, Object> waybill = new HashMap<>();
            waybill.put("id", logistics.getId());
            waybill.put("waybillNo", logistics.getTrackingNo());
            waybill.put("status", logistics.getStatus());
            
            // 获取订单信息
            OrderInfo order = orderInfoMapper.selectById(logistics.getOrderId());
            if (order != null) {
                // 收件人姓名筛选
                if (receiverName != null && !receiverName.isEmpty()) {
                    if (order.getReceiverName() == null || !order.getReceiverName().contains(receiverName)) {
                        continue;
                    }
                }
                
                waybill.put("receiverName", order.getReceiverName());
                waybill.put("receiverPhone", order.getReceiverPhone());
                waybill.put("receiverAddress", order.getReceiverAddress());
            } else if (receiverName != null && !receiverName.isEmpty()) {
                // 如果订单不存在但有收件人筛选条件，跳过
                continue;
            }
            
            // 配送员信息
            if (logistics.getDeliveryId() != null) {
                User delivery = userService.getById(logistics.getDeliveryId());
                if (delivery != null) {
                    waybill.put("deliveryStaff", delivery.getNickname());
                }
            }
            
            waybillList.add(waybill);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", waybillList);
        result.put("total", logisticsPage.getTotal());
        result.put("pages", logisticsPage.getPages());
        result.put("current", logisticsPage.getCurrent());
        
        return Result.success(result);
    }

    /**
     * 分配配送员
     */
    @PostMapping("/logistics/{id}/assign")
    public Result<String> assignDeliveryStaff(@PathVariable Long id, @RequestBody Map<String, Long> params) {
        Long staffId = params.get("staffId");
        if (staffId == null) {
            return Result.error("请选择配送员");
        }
        
        Logistics logistics = logisticsMapper.selectById(id);
        if (logistics == null) {
            return Result.error("运单不存在");
        }
        
        User staff = userService.getById(staffId);
        if (staff == null || staff.getUserType() != 3) {
            return Result.error("配送员不存在");
        }
        
        logistics.setDeliveryId(staffId);
        logistics.setDeliveryName(staff.getNickname());
        logistics.setDeliveryPhone(staff.getPhone());
        logistics.setUpdateTime(LocalDateTime.now());
        
        boolean success = logisticsMapper.updateById(logistics) > 0;
        if (success) {
            return Result.success("分配成功");
        }
        return Result.error("分配失败");
    }

    /**
     * 获取物流详情
     */
    @GetMapping("/logistics/{id}/detail")
    public Result<Map<String, Object>> getLogisticsDetail(@PathVariable Long id) {
        Logistics logistics = logisticsMapper.selectById(id);
        if (logistics == null) {
            return Result.error("运单不存在");
        }
        
        Map<String, Object> detail = new HashMap<>();
        detail.put("id", logistics.getId());
        detail.put("waybillNo", logistics.getTrackingNo());
        detail.put("status", logistics.getStatus());
        detail.put("pickupTime", logistics.getPickupTime());
        detail.put("deliveryTime", logistics.getDeliveryTime());
        detail.put("signTime", logistics.getSignTime());
        detail.put("currentLocation", logistics.getCurrentLocation());
        
        // 获取订单信息
        OrderInfo order = orderInfoMapper.selectById(logistics.getOrderId());
        if (order != null) {
            detail.put("orderId", order.getId());
            detail.put("receiverName", order.getReceiverName());
            detail.put("receiverPhone", order.getReceiverPhone());
            detail.put("receiverAddress", order.getReceiverAddress());
            detail.put("orderAmount", order.getTotalAmount());
        }
        
        // 配送员信息
        if (logistics.getDeliveryId() != null) {
            User delivery = userService.getById(logistics.getDeliveryId());
            if (delivery != null) {
                detail.put("deliveryStaff", delivery.getNickname());
                detail.put("deliveryPhone", delivery.getPhone());
            }
        }
        
        return Result.success(detail);
    }

    /**
     * 获取物流轨迹
     */
    @GetMapping("/logistics/{id}/tracks")
    public Result<List<LogisticsTrack>> getLogisticsTracks(@PathVariable Long id) {
        LambdaQueryWrapper<LogisticsTrack> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LogisticsTrack::getLogisticsId, id)
               .orderByDesc(LogisticsTrack::getCreateTime);
        List<LogisticsTrack> tracks = logisticsTrackMapper.selectList(wrapper);
        return Result.success(tracks);
    }

    // ==================== 轮播图管理接口 ====================

    /**
     * 获取轮播图列表
     */
    @GetMapping("/banners")
    public Result<List<Banner>> getBannerList() {
        LambdaQueryWrapper<Banner> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Banner::getSort);
        List<Banner> list = bannerMapper.selectList(wrapper);
        return Result.success(list);
    }

    /**
     * 添加轮播图
     */
    @PostMapping("/banners")
    public Result<String> addBanner(@RequestBody Banner banner) {
        banner.setCreateTime(LocalDateTime.now());
        banner.setUpdateTime(LocalDateTime.now());
        if (banner.getStatus() == null) {
            banner.setStatus(1);
        }
        if (banner.getSort() == null) {
            banner.setSort(0);
        }
        int result = bannerMapper.insert(banner);
        if (result > 0) {
            return Result.success("添加成功");
        }
        return Result.error("添加失败");
    }

    /**
     * 更新轮播图
     */
    @PutMapping("/banners/{id}")
    public Result<String> updateBanner(@PathVariable Long id, @RequestBody Banner banner) {
        Banner existBanner = bannerMapper.selectById(id);
        if (existBanner == null) {
            return Result.error("轮播图不存在");
        }
        banner.setId(id);
        banner.setUpdateTime(LocalDateTime.now());
        int result = bannerMapper.updateById(banner);
        if (result > 0) {
            return Result.success("更新成功");
        }
        return Result.error("更新失败");
    }

    /**
     * 删除轮播图
     */
    @DeleteMapping("/banners/{id}")
    public Result<String> deleteBanner(@PathVariable Long id) {
        Banner existBanner = bannerMapper.selectById(id);
        if (existBanner == null) {
            return Result.error("轮播图不存在");
        }
        int result = bannerMapper.deleteById(id);
        if (result > 0) {
            return Result.success("删除成功");
        }
        return Result.error("删除失败");
    }

    /**
     * 更新轮播图状态
     */
    @PutMapping("/banners/{id}/status")
    public Result<String> updateBannerStatus(@PathVariable Long id, @RequestBody Map<String, Integer> params) {
        Integer status = params.get("status");
        if (status == null) {
            return Result.error("状态不能为空");
        }
        Banner banner = bannerMapper.selectById(id);
        if (banner == null) {
            return Result.error("轮播图不存在");
        }
        banner.setStatus(status);
        banner.setUpdateTime(LocalDateTime.now());
        int result = bannerMapper.updateById(banner);
        if (result > 0) {
            return Result.success("更新成功");
        }
        return Result.error("更新失败");
    }

    // ==================== 管理员管理接口 ====================

    /**
     * 获取管理员列表
     */
    @GetMapping("/admins")
    public Result<List<User>> getAdminList() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUserType, 4)  // user_type = 4 是管理员
               .eq(User::getDeleted, 0)
               .orderByDesc(User::getCreateTime);
        List<User> list = userService.list(wrapper);
        return Result.success(list);
    }

    /**
     * 添加管理员
     */
    @PostMapping("/admins")
    public Result<String> addAdmin(@RequestBody User user) {
        // 检查用户名是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, user.getUsername())
               .eq(User::getDeleted, 0);
        User existUser = userService.getOne(wrapper);
        if (existUser != null) {
            return Result.error("用户名已存在");
        }

        user.setUserType(4); // user_type = 4 是管理员
        user.setStatus(1); // 默认启用
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            user.setPassword("123456"); // 默认密码
        }
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        boolean success = userService.save(user);
        if (success) {
            return Result.success("添加成功");
        } else {
            return Result.error("添加失败");
        }
    }

    /**
     * 更新管理员
     */
    @PutMapping("/admins/{id}")
    public Result<String> updateAdmin(@PathVariable Long id, @RequestBody User user) {
        User existUser = userService.getById(id);
        if (existUser == null || existUser.getUserType() != 4) {
            return Result.error("管理员不存在");
        }

        // 检查用户名是否被其他用户使用
        if (user.getUsername() != null && !user.getUsername().equals(existUser.getUsername())) {
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getUsername, user.getUsername())
                   .eq(User::getDeleted, 0)
                   .ne(User::getId, id);
            User otherUser = userService.getOne(wrapper);
            if (otherUser != null) {
                return Result.error("用户名已存在");
            }
        }

        user.setId(id);
        user.setUpdateTime(LocalDateTime.now());
        // 不允许修改用户类型和敏感字段
        user.setUserType(null);
        user.setPassword(null);
        user.setCreateTime(null);

        boolean success = userService.updateById(user);
        if (success) {
            return Result.success("更新成功");
        } else {
            return Result.error("更新失败");
        }
    }

    /**
     * 删除管理员
     */
    @DeleteMapping("/admins/{id}")
    public Result<String> deleteAdmin(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null || user.getUserType() != 4) {
            return Result.error("管理员不存在");
        }

        user.setDeleted(1);
        user.setUpdateTime(LocalDateTime.now());
        boolean success = userService.updateById(user);

        if (success) {
            return Result.success("删除成功");
        } else {
            return Result.error("删除失败");
        }
    }

    /**
     * 更新管理员状态
     */
    @PutMapping("/admins/{id}/status")
    public Result<String> updateAdminStatus(@PathVariable Long id, @RequestBody Map<String, Integer> params) {
        Integer status = params.get("status");
        if (status == null) {
            return Result.error("状态不能为空");
        }

        User user = userService.getById(id);
        if (user == null || user.getUserType() != 4) {
            return Result.error("管理员不存在");
        }

        user.setStatus(status);
        user.setUpdateTime(LocalDateTime.now());
        boolean success = userService.updateById(user);

        if (success) {
            return Result.success("更新成功");
        } else {
            return Result.error("更新失败");
        }
    }

    // ==================== 订单管理接口 ====================

    @Autowired
    private OrderItemMapper orderItemMapper;

    /**
     * 获取订单列表（管理员）
     */
    @GetMapping("/orders")
    public Result<Map<String, Object>> getOrderList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getDeleted, 0)
               .ne(OrderInfo::getStatus, 0); // 不显示待付款订单

        // 订单号筛选
        if (orderNo != null && !orderNo.isEmpty()) {
            wrapper.like(OrderInfo::getOrderNo, orderNo);
        }

        // 状态筛选
        if (status != null) {
            wrapper.eq(OrderInfo::getStatus, status);
        }

        // 日期筛选
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(OrderInfo::getCreateTime, startDate + " 00:00:00");
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(OrderInfo::getCreateTime, endDate + " 23:59:59");
        }

        // 按创建时间倒序
        wrapper.orderByDesc(OrderInfo::getCreateTime);

        // 分页查询
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<OrderInfo> pageParam =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<OrderInfo> resultPage =
                orderInfoMapper.selectPage(pageParam, wrapper);

        // 查询订单商品信息
        List<OrderInfo> records = resultPage.getRecords();
        for (OrderInfo order : records) {
            LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.eq(OrderItem::getOrderId, order.getId());
            List<OrderItem> items = orderItemMapper.selectList(itemWrapper);
            order.setItems(items);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", records);
        result.put("total", resultPage.getTotal());
        result.put("page", page);
        result.put("size", size);

        return Result.success(result);
    }

    /**
     * 获取订单详情（管理员）
     */
    @GetMapping("/orders/{id}")
    public Result<Map<String, Object>> getOrderDetail(@PathVariable Long id) {
        OrderInfo order = orderInfoMapper.selectById(id);
        if (order == null || order.getDeleted() == 1) {
            return Result.error("订单不存在");
        }

        // 查询订单商品
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getOrderId, id);
        List<OrderItem> items = orderItemMapper.selectList(itemWrapper);
        order.setItems(items);

        // 查询物流信息
        LambdaQueryWrapper<Logistics> logisticsWrapper = new LambdaQueryWrapper<>();
        logisticsWrapper.eq(Logistics::getOrderId, id)
                       .eq(Logistics::getDeleted, 0);
        List<Logistics> logisticsList = logisticsMapper.selectList(logisticsWrapper);

        // 查询物流轨迹
        Map<String, Object> result = new HashMap<>();
        result.put("order", order);
        if (!logisticsList.isEmpty()) {
            Logistics logistics = logisticsList.get(0);
            result.put("logistics", logistics);

            // 查询物流轨迹
            LambdaQueryWrapper<LogisticsTrack> trackWrapper = new LambdaQueryWrapper<>();
            trackWrapper.eq(LogisticsTrack::getLogisticsId, logistics.getId())
                       .orderByDesc(LogisticsTrack::getCreateTime);
            List<LogisticsTrack> tracks = logisticsTrackMapper.selectList(trackWrapper);
            result.put("logisticsTracks", tracks);
        }

        return Result.success(result);
    }

    /**
     * 获取订单统计
     */
    @GetMapping("/orders/stats")
    public Result<Map<String, Object>> getOrderStats() {
        Map<String, Object> stats = new HashMap<>();

        // 今日订单数
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LambdaQueryWrapper<OrderInfo> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.ge(OrderInfo::getCreateTime, todayStart)
                    .eq(OrderInfo::getDeleted, 0);
        long todayCount = orderInfoMapper.selectCount(todayWrapper);
        stats.put("today", todayCount);

        // 待发货
        LambdaQueryWrapper<OrderInfo> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(OrderInfo::getStatus, 1)
                      .eq(OrderInfo::getDeleted, 0);
        long pendingCount = orderInfoMapper.selectCount(pendingWrapper);
        stats.put("pending", pendingCount);

        // 待收货
        LambdaQueryWrapper<OrderInfo> shippedWrapper = new LambdaQueryWrapper<>();
        shippedWrapper.eq(OrderInfo::getStatus, 2)
                      .eq(OrderInfo::getDeleted, 0);
        long shippedCount = orderInfoMapper.selectCount(shippedWrapper);
        stats.put("shipped", shippedCount);

        // 已完成
        LambdaQueryWrapper<OrderInfo> completedWrapper = new LambdaQueryWrapper<>();
        completedWrapper.eq(OrderInfo::getStatus, 3)
                        .eq(OrderInfo::getDeleted, 0);
        long completedCount = orderInfoMapper.selectCount(completedWrapper);
        stats.put("completed", completedCount);

        return Result.success(stats);
    }

    /**
     * 订单发货
     */
    @PutMapping("/orders/{id}/ship")
    public Result<String> shipOrder(@PathVariable Long id, @RequestBody Map<String, String> params) {
        String logisticsNo = params.get("logisticsNo");
        String logisticsCompany = params.get("logisticsCompany");

        if (logisticsNo == null || logisticsNo.isEmpty()) {
            return Result.error("物流单号不能为空");
        }

        OrderInfo order = orderInfoMapper.selectById(id);
        if (order == null || order.getDeleted() == 1) {
            return Result.error("订单不存在");
        }

        if (order.getStatus() != 1) {
            return Result.error("订单状态不是待发货");
        }

        order.setStatus(2); // 待收货
        order.setLogisticsNo(logisticsNo);
        order.setLogisticsCompany(logisticsCompany);
        order.setShipTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        int result = orderInfoMapper.updateById(order);
        if (result > 0) {
            return Result.success("发货成功");
        } else {
            return Result.error("发货失败");
        }
    }

    /**
     * 取消订单（管理员）
     */
    @PutMapping("/orders/{id}/cancel")
    public Result<String> cancelOrder(@PathVariable Long id) {
        OrderInfo order = orderInfoMapper.selectById(id);
        if (order == null || order.getDeleted() == 1) {
            return Result.error("订单不存在");
        }

        if (order.getStatus() == 3) {
            return Result.error("已完成订单不能取消");
        }

        if (order.getStatus() == 4) {
            return Result.error("订单已取消");
        }

        order.setStatus(4); // 已取消
        order.setCancelType(2); // 商家取消
        order.setUpdateTime(LocalDateTime.now());

        int result = orderInfoMapper.updateById(order);
        if (result > 0) {
            return Result.success("订单已取消");
        } else {
            return Result.error("取消订单失败");
        }
    }
}
