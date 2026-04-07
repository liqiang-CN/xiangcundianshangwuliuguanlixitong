package com.rural.ecommerce.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rural.ecommerce.common.Result;
import com.rural.ecommerce.entity.Logistics;
import com.rural.ecommerce.entity.LogisticsTrack;
import com.rural.ecommerce.entity.OrderInfo;
import com.rural.ecommerce.mapper.LogisticsMapper;
import com.rural.ecommerce.mapper.LogisticsTrackMapper;
import com.rural.ecommerce.service.OrderService;
import com.rural.ecommerce.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private LogisticsMapper logisticsMapper;

    @Autowired
    private LogisticsTrackMapper logisticsTrackMapper;
    
    // 获取当前登录用户ID
    private Long getCurrentUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            return jwtUtil.getUserId(token);
        }
        return null;
    }
    
    // 获取订单列表
    @GetMapping("/list")
    public Result<List<OrderInfo>> getOrderList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status,
            HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return Result.error("请先登录");
            }
            List<OrderInfo> list = orderService.getOrderList(userId, page, size, status);
            return Result.success(list);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取订单列表失败：" + e.getMessage());
        }
    }
    
    // 获取订单详情
    @GetMapping("/{id}")
    public Result<OrderInfo> getOrderDetail(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("请先登录");
        }
        OrderInfo order = orderService.getOrderDetail(id, userId);
        if (order == null) {
            return Result.error("订单不存在");
        }
        return Result.success(order);
    }
    
    // 创建订单
    @PostMapping
    public Result<OrderInfo> createOrder(@RequestBody OrderInfo orderInfo, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("请先登录");
        }
        orderInfo.setUserId(userId);
        OrderInfo newOrder = orderService.createOrder(orderInfo);
        return Result.success(newOrder);
    }
    
    // 取消订单
    @PutMapping("/{id}/cancel")
    public Result<Void> cancelOrder(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("请先登录");
        }
        boolean success = orderService.cancelOrder(id, userId);
        if (success) {
            return Result.success(null);
        }
        return Result.error("取消订单失败");
    }
    
    // 确认收货
    @PutMapping("/{id}/confirm")
    public Result<Void> confirmReceive(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("请先登录");
        }
        boolean success = orderService.confirmReceive(id, userId);
        if (success) {
            return Result.success(null);
        }
        return Result.error("确认收货失败");
    }
    
    // 支付订单
    @PostMapping("/{id}/pay")
    public Result<Void> payOrder(@PathVariable Long id, @RequestBody Map<String, String> params, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("请先登录");
        }
        boolean success = orderService.payOrder(id, userId);
        if (success) {
            return Result.success(null);
        }
        return Result.error("支付失败");
    }

    // 获取物流信息
    @GetMapping("/{id}/logistics")
    public Result<Map<String, Object>> getLogistics(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("请先登录");
        }

        // 先检查订单是否存在且属于当前用户
        OrderInfo order = orderService.getOrderDetail(id, userId);
        if (order == null) {
            return Result.error("订单不存在");
        }

        // 查询物流信息
        LambdaQueryWrapper<Logistics> logisticsWrapper = new LambdaQueryWrapper<>();
        logisticsWrapper.eq(Logistics::getOrderId, id);
        Logistics logistics = logisticsMapper.selectOne(logisticsWrapper);

        Map<String, Object> result = new HashMap<>();

        if (logistics != null) {
            result.put("company", logistics.getDeliveryName() != null ? logistics.getDeliveryName() : "未知物流");
            result.put("trackingNo", logistics.getTrackingNo());

            // 查询物流轨迹
            LambdaQueryWrapper<LogisticsTrack> trackWrapper = new LambdaQueryWrapper<>();
            trackWrapper.eq(LogisticsTrack::getLogisticsId, logistics.getId());
            trackWrapper.orderByDesc(LogisticsTrack::getCreateTime);
            List<LogisticsTrack> tracks = logisticsTrackMapper.selectList(trackWrapper);

            List<Map<String, String>> trackList = tracks.stream().map(track -> {
                Map<String, String> map = new HashMap<>();
                map.put("time", track.getCreateTime() != null ? track.getCreateTime().toString() : "");
                map.put("content", track.getDescription() != null ? track.getDescription() : "");
                return map;
            }).collect(Collectors.toList());

            result.put("tracks", trackList);
        } else {
            // 如果没有物流记录，从订单信息中获取物流信息
            result.put("company", order.getLogisticsCompany() != null ? order.getLogisticsCompany() : "未知物流");
            result.put("trackingNo", order.getLogisticsNo() != null ? order.getLogisticsNo() : "");
            result.put("tracks", new java.util.ArrayList<>());
        }

        return Result.success(result);
    }
}
