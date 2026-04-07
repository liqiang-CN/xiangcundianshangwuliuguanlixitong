package com.rural.ecommerce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rural.ecommerce.entity.Logistics;
import com.rural.ecommerce.entity.LogisticsTrack;
import com.rural.ecommerce.entity.OrderInfo;
import com.rural.ecommerce.entity.OrderItem;
import com.rural.ecommerce.entity.Product;
import com.rural.ecommerce.entity.User;
import com.rural.ecommerce.mapper.LogisticsMapper;
import com.rural.ecommerce.mapper.LogisticsTrackMapper;
import com.rural.ecommerce.mapper.OrderInfoMapper;
import com.rural.ecommerce.mapper.OrderItemMapper;
import com.rural.ecommerce.mapper.ProductMapper;
import com.rural.ecommerce.mapper.UserMapper;
import com.rural.ecommerce.service.LogisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LogisticsServiceImpl implements LogisticsService {

    @Autowired
    private LogisticsMapper logisticsMapper;

    @Autowired
    private LogisticsTrackMapper logisticsTrackMapper;

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private ProductMapper productMapper;

    @Override
    @Transactional
    public void createLogistics(Long orderId, String trackingNo, String logisticsCompany) {
        // 查询订单信息
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            return;
        }

        // 创建物流记录
        Logistics logistics = new Logistics();
        logistics.setOrderId(orderId);
        logistics.setTrackingNo(trackingNo);
        logistics.setStatus(0); // 待揽件
        logistics.setCreateTime(LocalDateTime.now());
        logistics.setUpdateTime(LocalDateTime.now());

        logisticsMapper.insert(logistics);

        // 添加初始物流轨迹
        LogisticsTrack track = new LogisticsTrack();
        track.setLogisticsId(logistics.getId());
        track.setStatus(0);
        track.setDescription("商家已发货，等待配送员揽件");
        track.setLocation(order.getSenderAddress());
        track.setCreateTime(LocalDateTime.now());

        logisticsTrackMapper.insert(track);
    }

    @Override
    public Map<String, Object> getStats(Long deliveryId) {
        Map<String, Object> stats = new HashMap<>();

        // 待揽件数量（status=0，未分配或分配给当前配送员）
        LambdaQueryWrapper<Logistics> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(Logistics::getStatus, 0);
        pendingWrapper.and(wrapper -> wrapper.isNull(Logistics::getDeliveryId)
                .or().eq(Logistics::getDeliveryId, deliveryId));
        int pendingPickup = logisticsMapper.selectCount(pendingWrapper).intValue();

        // 配送中数量（status=1或2，且分配给当前配送员）
        LambdaQueryWrapper<Logistics> deliveringWrapper = new LambdaQueryWrapper<>();
        deliveringWrapper.in(Logistics::getStatus, 1, 2);
        deliveringWrapper.eq(Logistics::getDeliveryId, deliveryId);
        int delivering = logisticsMapper.selectCount(deliveringWrapper).intValue();

        // 今日完成数量（今日签收，且分配给当前配送员）
        LambdaQueryWrapper<Logistics> completedWrapper = new LambdaQueryWrapper<>();
        completedWrapper.eq(Logistics::getStatus, 3);
        completedWrapper.eq(Logistics::getDeliveryId, deliveryId);
        completedWrapper.ge(Logistics::getSignTime, LocalDate.now().atStartOfDay());
        int completed = logisticsMapper.selectCount(completedWrapper).intValue();

        // 配送准时率（简化计算，实际应该根据预计时间和实际时间计算）
        int onTimeRate = 98;

        stats.put("pendingPickup", pendingPickup);
        stats.put("delivering", delivering);
        stats.put("completed", completed);
        stats.put("onTimeRate", onTimeRate);

        return stats;
    }

    @Override
    public List<Logistics> getPendingList(Long deliveryId, Integer page, Integer size) {
        LambdaQueryWrapper<Logistics> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Logistics::getStatus, 0);
        wrapper.and(w -> w.isNull(Logistics::getDeliveryId)
                .or().eq(Logistics::getDeliveryId, deliveryId));
        wrapper.orderByDesc(Logistics::getCreateTime);
        wrapper.last("LIMIT " + (page - 1) * size + ", " + size);

        return logisticsMapper.selectList(wrapper);
    }

    @Override
    public List<Logistics> getDeliveringList(Long deliveryId, Integer page, Integer size) {
        LambdaQueryWrapper<Logistics> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Logistics::getStatus, 1, 2);
        wrapper.eq(Logistics::getDeliveryId, deliveryId);
        wrapper.orderByDesc(Logistics::getUpdateTime);
        wrapper.last("LIMIT " + (page - 1) * size + ", " + size);

        return logisticsMapper.selectList(wrapper);
    }

    @Override
    public List<Map<String, Object>> getTodayTasks(Long deliveryId) {
        List<Map<String, Object>> tasks = new ArrayList<>();

        // 获取今日待揽件和配送中的任务
        LambdaQueryWrapper<Logistics> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Logistics::getStatus, 0, 1, 2);
        wrapper.and(w -> w.isNull(Logistics::getDeliveryId)
                .or().eq(Logistics::getDeliveryId, deliveryId));
        wrapper.orderByAsc(Logistics::getCreateTime);
        wrapper.last("LIMIT 10");

        List<Logistics> list = logisticsMapper.selectList(wrapper);

        for (Logistics logistics : list) {
            Map<String, Object> task = new HashMap<>();

            // 查询订单信息
            OrderInfo order = orderInfoMapper.selectById(logistics.getOrderId());
            if (order == null) continue;

            task.put("id", logistics.getId());
            task.put("waybillNo", logistics.getTrackingNo());
            task.put("title", getTaskTitle(logistics.getStatus(), order));
            task.put("address", order.getReceiverAddress());
            task.put("status", getTaskStatus(logistics.getStatus()));
            task.put("statusText", getTaskStatusText(logistics.getStatus()));
            task.put("time", logistics.getCreateTime().format(DateTimeFormatter.ofPattern("HH:mm")));
            task.put("type", getTaskType(logistics.getStatus()));

            tasks.add(task);
        }

        return tasks;
    }

    private String getTaskTitle(int status, OrderInfo order) {
        switch (status) {
            case 0:
                return "揽件 - " + order.getSenderName();
            case 1:
                return "运输 - " + order.getReceiverName();
            case 2:
                return "派送 - " + order.getReceiverName();
            default:
                return "配送任务";
        }
    }

    private String getTaskStatus(int status) {
        switch (status) {
            case 0:
                return "pending";
            case 1:
                return "transit";
            case 2:
                return "delivering";
            default:
                return "unknown";
        }
    }

    private String getTaskStatusText(int status) {
        switch (status) {
            case 0:
                return "待揽件";
            case 1:
                return "运输中";
            case 2:
                return "派送中";
            default:
                return "未知";
        }
    }

    private String getTaskType(int status) {
        switch (status) {
            case 0:
                return "primary";
            case 1:
                return "warning";
            case 2:
                return "success";
            default:
                return "info";
        }
    }

    @Override
    public List<Logistics> getRecords(Long deliveryId, Integer page, Integer size) {
        LambdaQueryWrapper<Logistics> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Logistics::getStatus, 3); // 已签收
        wrapper.eq(Logistics::getDeliveryId, deliveryId);
        wrapper.orderByDesc(Logistics::getSignTime);
        wrapper.last("LIMIT " + (page - 1) * size + ", " + size);

        return logisticsMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public boolean pickup(Long logisticsId, Long deliveryId) {
        // 更新物流记录
        Logistics logistics = logisticsMapper.selectById(logisticsId);
        if (logistics == null || logistics.getStatus() != 0) {
            return false;
        }

        // 获取配送员信息
        User deliveryUser = userMapper.selectById(deliveryId);
        String deliveryName = deliveryUser != null ? deliveryUser.getNickname() : "";
        String deliveryPhone = deliveryUser != null ? deliveryUser.getPhone() : "";

        logistics.setStatus(1); // 运输中
        logistics.setDeliveryId(deliveryId);
        logistics.setDeliveryName(deliveryName);
        logistics.setDeliveryPhone(deliveryPhone);
        logistics.setPickupTime(LocalDateTime.now());
        logistics.setUpdateTime(LocalDateTime.now());

        logisticsMapper.updateById(logistics);

        // 添加物流轨迹
        OrderInfo order = orderInfoMapper.selectById(logistics.getOrderId());
        LogisticsTrack track = new LogisticsTrack();
        track.setLogisticsId(logisticsId);
        track.setStatus(1);
        track.setDescription("配送员已揽件，准备运输");
        track.setLocation(order != null ? order.getSenderAddress() : "");
        track.setCreateTime(LocalDateTime.now());

        logisticsTrackMapper.insert(track);

        return true;
    }

    @Override
    @Transactional
    public boolean startDeliver(Long logisticsId, Long deliveryId) {
        Logistics logistics = logisticsMapper.selectById(logisticsId);
        if (logistics == null || logistics.getStatus() != 1) {
            return false;
        }

        // 验证是否分配给当前配送员
        if (!deliveryId.equals(logistics.getDeliveryId())) {
            return false;
        }

        logistics.setStatus(2); // 派送中
        logistics.setUpdateTime(LocalDateTime.now());

        logisticsMapper.updateById(logistics);

        // 添加物流轨迹
        OrderInfo order = orderInfoMapper.selectById(logistics.getOrderId());
        LogisticsTrack track = new LogisticsTrack();
        track.setLogisticsId(logisticsId);
        track.setStatus(2);
        track.setDescription("商品正在派送中，请保持电话畅通");
        track.setLocation(order != null ? order.getReceiverAddress() : "");
        track.setCreateTime(LocalDateTime.now());

        logisticsTrackMapper.insert(track);

        return true;
    }

    @Override
    @Transactional
    public boolean sign(Long logisticsId, Long deliveryId, String signer) {
        Logistics logistics = logisticsMapper.selectById(logisticsId);
        if (logistics == null || logistics.getStatus() != 2) {
            return false;
        }

        // 验证是否分配给当前配送员
        if (!deliveryId.equals(logistics.getDeliveryId())) {
            return false;
        }

        // 先查询订单，检查销量是否已计算
        OrderInfo existingOrder = orderInfoMapper.selectById(logistics.getOrderId());
        if (existingOrder != null && existingOrder.getSalesCounted() != null && existingOrder.getSalesCounted() == 1) {
            // 销量已计算，只更新物流状态，不重复计算销量（幂等）
            logistics.setStatus(3); // 已签收
            logistics.setSignTime(LocalDateTime.now());
            logistics.setUpdateTime(LocalDateTime.now());
            logisticsMapper.updateById(logistics);
            return true;
        }

        logistics.setStatus(3); // 已签收
        logistics.setSignTime(LocalDateTime.now());
        logistics.setUpdateTime(LocalDateTime.now());

        logisticsMapper.updateById(logistics);

        // 添加物流轨迹
        OrderInfo order = orderInfoMapper.selectById(logistics.getOrderId());
        LogisticsTrack track = new LogisticsTrack();
        track.setLogisticsId(logisticsId);
        track.setStatus(3);
        track.setDescription("商品已签收，签收人：" + (signer != null ? signer : "本人"));
        track.setLocation(order != null ? order.getReceiverAddress() : "");
        track.setCreateTime(LocalDateTime.now());

        logisticsTrackMapper.insert(track);

        // 更新订单状态为已完成
        OrderInfo updateOrder = new OrderInfo();
        updateOrder.setId(logistics.getOrderId());
        updateOrder.setStatus(3); // 已完成
        updateOrder.setSalesCounted(1); // 标记销量已计算
        updateOrder.setReceiveTime(LocalDateTime.now());
        updateOrder.setUpdateTime(LocalDateTime.now());

        orderInfoMapper.updateById(updateOrder);

        // 更新商品销量
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getOrderId, logistics.getOrderId());
        List<OrderItem> items = orderItemMapper.selectList(itemWrapper);

        for (OrderItem item : items) {
            Product product = productMapper.selectById(item.getProductId());
            if (product != null) {
                product.setSales(product.getSales() + item.getQuantity());
                product.setUpdateTime(LocalDateTime.now());
                productMapper.updateById(product);
            }
        }

        return true;
    }

    @Override
    public List<LogisticsTrack> getTracks(Long logisticsId) {
        LambdaQueryWrapper<LogisticsTrack> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LogisticsTrack::getLogisticsId, logisticsId);
        wrapper.orderByDesc(LogisticsTrack::getCreateTime);

        return logisticsTrackMapper.selectList(wrapper);
    }

    @Override
    public Logistics getByOrderId(Long orderId) {
        LambdaQueryWrapper<Logistics> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Logistics::getOrderId, orderId);

        return logisticsMapper.selectOne(wrapper);
    }
}
