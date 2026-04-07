package com.rural.ecommerce.controller;

import com.rural.ecommerce.common.Result;
import com.rural.ecommerce.entity.Logistics;
import com.rural.ecommerce.entity.LogisticsTrack;
import com.rural.ecommerce.entity.OrderInfo;
import com.rural.ecommerce.service.LogisticsService;
import com.rural.ecommerce.mapper.LogisticsMapper;
import com.rural.ecommerce.mapper.OrderInfoMapper;
import com.rural.ecommerce.service.OrderService;
import com.rural.ecommerce.util.JwtUtil;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/logistics")
public class LogisticsController {

    @Autowired
    private LogisticsService logisticsService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private LogisticsMapper logisticsMapper;

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

    /**
     * 获取工作台统计数据
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats(HttpServletRequest request) {
        Long deliveryId = getCurrentUserId(request);
        if (deliveryId == null) {
            return Result.error("请先登录");
        }
        Map<String, Object> stats = logisticsService.getStats(deliveryId);
        return Result.success(stats);
    }

    /**
     * 获取待揽件列表
     */
    @GetMapping("/pending")
    public Result<List<Logistics>> getPendingList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest request) {
        Long deliveryId = getCurrentUserId(request);
        if (deliveryId == null) {
            return Result.error("请先登录");
        }
        List<Logistics> list = logisticsService.getPendingList(deliveryId, page, size);
        return Result.success(list);
    }

    /**
     * 获取配送中列表
     */
    @GetMapping("/delivering")
    public Result<List<Logistics>> getDeliveringList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest request) {
        Long deliveryId = getCurrentUserId(request);
        if (deliveryId == null) {
            return Result.error("请先登录");
        }
        List<Logistics> list = logisticsService.getDeliveringList(deliveryId, page, size);
        return Result.success(list);
    }

    /**
     * 获取今日配送任务
     */
    @GetMapping("/today-tasks")
    public Result<List<Map<String, Object>>> getTodayTasks(HttpServletRequest request) {
        Long deliveryId = getCurrentUserId(request);
        if (deliveryId == null) {
            return Result.error("请先登录");
        }
        List<Map<String, Object>> tasks = logisticsService.getTodayTasks(deliveryId);
        return Result.success(tasks);
    }

    /**
     * 获取配送记录
     */
    @GetMapping("/records")
    public Result<List<Logistics>> getRecords(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest request) {
        Long deliveryId = getCurrentUserId(request);
        if (deliveryId == null) {
            return Result.error("请先登录");
        }
        List<Logistics> list = logisticsService.getRecords(deliveryId, page, size);
        return Result.success(list);
    }

    /**
     * 揽件
     */
    @PostMapping("/{id}/pickup")
    public Result<Void> pickup(@PathVariable Long id, HttpServletRequest request) {
        Long deliveryId = getCurrentUserId(request);
        if (deliveryId == null) {
            return Result.error("请先登录");
        }
        boolean success = logisticsService.pickup(id, deliveryId);
        if (success) {
            return Result.success(null);
        }
        return Result.error("揽件失败");
    }

    /**
     * 开始派送
     */
    @PostMapping("/{id}/deliver")
    public Result<Void> startDeliver(@PathVariable Long id, HttpServletRequest request) {
        Long deliveryId = getCurrentUserId(request);
        if (deliveryId == null) {
            return Result.error("请先登录");
        }
        boolean success = logisticsService.startDeliver(id, deliveryId);
        if (success) {
            return Result.success(null);
        }
        return Result.error("操作失败");
    }

    /**
     * 签收
     */
    @PostMapping("/{id}/sign")
    public Result<Void> sign(@PathVariable Long id, @RequestBody Map<String, String> params, HttpServletRequest request) {
        Long deliveryId = getCurrentUserId(request);
        if (deliveryId == null) {
            return Result.error("请先登录");
        }
        String signer = params.get("signer");

        // 获取物流记录并验证
        Logistics logistics = logisticsMapper.selectById(id);
        if (logistics == null) {
            return Result.error("物流记录不存在");
        }
        if (logistics.getStatus() != 2) {
            return Result.error("订单状态不是派送中，当前状态：" + logistics.getStatus());
        }
        if (!deliveryId.equals(logistics.getDeliveryId())) {
            return Result.error("该订单未分配给您，订单配送员ID：" + logistics.getDeliveryId() + "，您的ID：" + deliveryId);
        }

        boolean success = logisticsService.sign(id, deliveryId, signer);
        if (success) {
            return Result.success(null);
        }
        return Result.error("签收失败");
    }

    /**
     * 获取物流轨迹
     */
    @GetMapping("/{id}/tracks")
    public Result<List<LogisticsTrack>> getTracks(@PathVariable Long id) {
        List<LogisticsTrack> tracks = logisticsService.getTracks(id);
        return Result.success(tracks);
    }

    /**
     * 根据订单ID获取物流信息
     */
    @GetMapping("/order/{orderId}")
    public Result<Logistics> getByOrderId(@PathVariable Long orderId) {
        Logistics logistics = logisticsService.getByOrderId(orderId);
        return Result.success(logistics);
    }

    /**
     * 获取订单详情（物流人员用）
     */
    @GetMapping("/order/{orderId}/detail")
    public Result<OrderInfo> getOrderDetail(@PathVariable Long orderId, HttpServletRequest request) {
        Long deliveryId = getCurrentUserId(request);
        if (deliveryId == null) {
            return Result.error("请先登录");
        }
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            return Result.error("订单不存在");
        }
        return Result.success(order);
    }
}
