package com.rural.ecommerce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rural.ecommerce.entity.Logistics;
import com.rural.ecommerce.entity.LogisticsTrack;
import com.rural.ecommerce.entity.OrderInfo;
import com.rural.ecommerce.entity.OrderItem;
import com.rural.ecommerce.entity.Product;
import com.rural.ecommerce.entity.User;
import com.rural.ecommerce.entity.UserMessage;
import com.rural.ecommerce.mapper.ProductMapper;
import com.rural.ecommerce.mapper.LogisticsMapper;
import com.rural.ecommerce.mapper.LogisticsTrackMapper;
import com.rural.ecommerce.mapper.OrderInfoMapper;
import com.rural.ecommerce.mapper.OrderItemMapper;
import com.rural.ecommerce.mapper.UserMapper;
import com.rural.ecommerce.repository.UserMessageRepository;
import com.rural.ecommerce.service.OrderService;
import com.rural.ecommerce.websocket.OrderWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private LogisticsMapper logisticsMapper;

    @Autowired
    private LogisticsTrackMapper logisticsTrackMapper;

    @Autowired
    private OrderWebSocketHandler orderWebSocketHandler;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserMessageRepository userMessageRepository;
    
    @Override
    public List<OrderInfo> getOrderList(Long userId, Integer page, Integer size, Integer status) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getUserId, userId);
        
        // 按状态筛选
        if (status != null) {
            wrapper.eq(OrderInfo::getStatus, status);
        }
        
        // 按创建时间倒序
        wrapper.orderByDesc(OrderInfo::getCreateTime);
        
        // 分页
        wrapper.last("LIMIT " + (page - 1) * size + ", " + size);
        
        List<OrderInfo> orders = orderInfoMapper.selectList(wrapper);
        
        // 查询每个订单的商品项
        for (OrderInfo order : orders) {
            List<OrderItem> items = getOrderItems(order.getId());
            order.setItems(items);
        }
        
        return orders;
    }
    
    @Override
    public OrderInfo getOrderDetail(Long id, Long userId) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getId, id);
        wrapper.eq(OrderInfo::getUserId, userId);
        
        OrderInfo order = orderInfoMapper.selectOne(wrapper);
        if (order != null) {
            List<OrderItem> items = getOrderItems(order.getId());
            order.setItems(items);
        }
        return order;
    }
    
    private List<OrderItem> getOrderItems(Long orderId) {
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, orderId);
        return orderItemMapper.selectList(wrapper);
    }
    
    @Override
    @Transactional
    public OrderInfo createOrder(OrderInfo orderInfo) {
        // 生成订单号
        String orderNo = generateOrderNo();
        orderInfo.setOrderNo(orderNo);
        
        // 设置初始状态为待付款
        orderInfo.setStatus(0);
        orderInfo.setCreateTime(LocalDateTime.now());
        orderInfo.setUpdateTime(LocalDateTime.now());
        
        // 保存订单
        orderInfoMapper.insert(orderInfo);
        
        // 保存订单商品项
        if (orderInfo.getItems() != null) {
            for (OrderItem item : orderInfo.getItems()) {
                item.setOrderId(orderInfo.getId());
                
                // 从商品表获取商品信息（如果前端没有传入）
                if (item.getProductId() != null) {
                    Product product = productMapper.selectById(item.getProductId());
                    if (product != null) {
                        // 填充商品名称（如果为空）
                        if (item.getProductName() == null || item.getProductName().isEmpty()) {
                            item.setProductName(product.getName());
                        }
                        // 填充商品图片（如果为空）
                        if (item.getProductImage() == null || item.getProductImage().isEmpty()) {
                            item.setProductImage(product.getMainImage());
                        }
                        // 填充农户ID（如果为空）
                        if (item.getFarmerId() == null || item.getFarmerId() == 0) {
                            item.setFarmerId(product.getFarmerId());
                        }
                        
                        // 填充店铺信息到订单（如果订单还没有店铺信息）
                        if (orderInfo.getShopId() == null || orderInfo.getShopId() == 0) {
                            orderInfo.setShopId(product.getFarmerId());
                            // 查询店铺名称
                            User farmer = userMapper.selectById(product.getFarmerId());
                            if (farmer != null) {
                                orderInfo.setShopName(farmer.getShopName() != null ? farmer.getShopName() : farmer.getUsername());
                                orderInfo.setFarmerId(product.getFarmerId());
                                // 更新订单的店铺信息到数据库
                                OrderInfo updateOrder = new OrderInfo();
                                updateOrder.setId(orderInfo.getId());
                                updateOrder.setShopId(orderInfo.getShopId());
                                updateOrder.setShopName(orderInfo.getShopName());
                                updateOrder.setFarmerId(orderInfo.getFarmerId());
                                orderInfoMapper.updateById(updateOrder);
                            }
                        }
                        // 填充价格（如果为空）
                        if (item.getPrice() == null || item.getPrice().compareTo(BigDecimal.ZERO) == 0) {
                            item.setPrice(product.getPrice());
                        }
                        // 填充单位（如果为空）
                        if (item.getUnit() == null || item.getUnit().isEmpty()) {
                            item.setUnit(product.getUnit());
                        }
                    }
                }
                
                // 计算总金额
                if (item.getTotalAmount() == null || item.getTotalAmount().compareTo(BigDecimal.ZERO) == 0) {
                    item.setTotalAmount(item.getPrice().multiply(new BigDecimal(item.getQuantity())));
                }
                
                orderItemMapper.insert(item);
            }
        }
        
        return orderInfo;
    }
    
    private String generateOrderNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomStr = String.format("%04d", (int) (Math.random() * 10000));
        return dateStr + randomStr;
    }
    
    @Override
    public boolean cancelOrder(Long id, Long userId) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getId, id);
        wrapper.eq(OrderInfo::getUserId, userId);
        wrapper.eq(OrderInfo::getStatus, 0); // 只能取消待付款订单

        OrderInfo order = new OrderInfo();
        order.setStatus(4); // 已取消
        order.setCancelType(1); // 用户取消
        order.setUpdateTime(LocalDateTime.now());

        int rows = orderInfoMapper.update(order, wrapper);
        
        // 发送订单取消通知
        if (rows > 0) {
            OrderInfo cancelledOrder = orderInfoMapper.selectById(id);
            if (cancelledOrder != null) {
                sendOrderNotification(userId, cancelledOrder.getOrderNo(), "已取消");
            }
        }
        
        return rows > 0;
    }
    
    @Override
    @Transactional
    public boolean confirmReceive(Long id, Long userId) {
        // 先查询订单，检查销量是否已计算
        OrderInfo existingOrder = orderInfoMapper.selectById(id);
        if (existingOrder == null || existingOrder.getSalesCounted() != null && existingOrder.getSalesCounted() == 1) {
            // 订单不存在或销量已计算，直接返回成功（幂等）
            return existingOrder != null;
        }
        
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getId, id);
        wrapper.eq(OrderInfo::getUserId, userId);
        wrapper.eq(OrderInfo::getStatus, 2); // 只能确认待收货订单

        OrderInfo order = new OrderInfo();
        order.setStatus(3); // 已完成
        order.setSalesCounted(1); // 标记销量已计算
        order.setUpdateTime(LocalDateTime.now());

        int rows = orderInfoMapper.update(order, wrapper);

        // 更新商品销量
        if (rows > 0) {
            // 查询订单项
            LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.eq(OrderItem::getOrderId, id);
            List<OrderItem> items = orderItemMapper.selectList(itemWrapper);

            // 更新每个商品的销量
            for (OrderItem item : items) {
                Product product = productMapper.selectById(item.getProductId());
                if (product != null) {
                    product.setSales(product.getSales() + item.getQuantity());
                    product.setUpdateTime(LocalDateTime.now());
                    productMapper.updateById(product);
                }
            }
            
            // 发送确认收货通知
            OrderInfo completedOrder = orderInfoMapper.selectById(id);
            if (completedOrder != null) {
                sendOrderNotification(userId, completedOrder.getOrderNo(), "已完成，感谢您的购买");
            }
        }

        return rows > 0;
    }
    
    @Override
    @Transactional
    public boolean payOrder(Long id, Long userId) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getId, id);
        wrapper.eq(OrderInfo::getUserId, userId);
        wrapper.eq(OrderInfo::getStatus, 0); // 只能支付待付款订单

        OrderInfo order = new OrderInfo();
        order.setStatus(1); // 待发货
        order.setPayTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        int rows = orderInfoMapper.update(order, wrapper);

        if (rows > 0) {
            // 支付成功，查询订单详情和订单项
            OrderInfo paidOrder = orderInfoMapper.selectById(id);
            if (paidOrder != null) {
                // 查询该订单包含的所有订单项
                LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
                itemWrapper.eq(OrderItem::getOrderId, id);
                List<OrderItem> items = orderItemMapper.selectList(itemWrapper);

                System.out.println("订单支付成功，订单ID: " + id + ", 订单项数量: " + items.size());

                // 向每个相关农户发送新订单通知
                for (OrderItem item : items) {
                    Long farmerId = item.getFarmerId();
                    System.out.println("订单项: " + item.getProductName() + ", farmerId: " + farmerId);
                    if (farmerId != null) {
                        Map<String, Object> orderData = new HashMap<>();
                        orderData.put("orderId", id);
                        orderData.put("orderNo", paidOrder.getOrderNo());
                        orderData.put("productName", item.getProductName());
                        orderData.put("quantity", item.getQuantity());
                        orderData.put("totalAmount", item.getTotalAmount());
                        orderData.put("createTime", paidOrder.getCreateTime().toString());

                        orderWebSocketHandler.sendNewOrderNotification(farmerId, orderData);
                    }
                }
            }
            
            // 发送支付成功通知给用户
            sendOrderNotification(userId, paidOrder.getOrderNo(), "已支付成功，等待商家发货");
        }

        return rows > 0;
    }

    // 农户相关方法
    @Override
    public BigDecimal getTodayIncome(Long farmerId) {
        // 查询今日该农户的订单项收入
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getFarmerId, farmerId);
        List<OrderItem> items = orderItemMapper.selectList(itemWrapper);

        if (items.isEmpty()) {
            return new BigDecimal("0.00");
        }

        // 获取订单ID列表
        List<Long> orderIds = items.stream()
            .map(OrderItem::getOrderId)
            .distinct()
            .collect(Collectors.toList());

        // 查询今日已完成的订单（status=3）
        LambdaQueryWrapper<OrderInfo> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.in(OrderInfo::getId, orderIds);
        orderWrapper.ge(OrderInfo::getCreateTime, LocalDate.now().atStartOfDay());
        orderWrapper.lt(OrderInfo::getCreateTime, LocalDate.now().plusDays(1).atStartOfDay());
        orderWrapper.eq(OrderInfo::getStatus, 3); // 已完成订单

        List<OrderInfo> orders = orderInfoMapper.selectList(orderWrapper);

        // 计算收入（只计算该农户的商品）
        BigDecimal income = BigDecimal.ZERO;
        for (OrderInfo order : orders) {
            LambdaQueryWrapper<OrderItem> farmerItemWrapper = new LambdaQueryWrapper<>();
            farmerItemWrapper.eq(OrderItem::getOrderId, order.getId());
            farmerItemWrapper.eq(OrderItem::getFarmerId, farmerId);
            List<OrderItem> farmerItems = orderItemMapper.selectList(farmerItemWrapper);

            for (OrderItem item : farmerItems) {
                if (item.getTotalAmount() != null) {
                    income = income.add(item.getTotalAmount());
                }
            }
        }

        return income;
    }

    @Override
    public int getTodayOrderCount(Long farmerId) {
        // 查询今日已支付的订单数（用户确认支付后+1，商家取消后-1）
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getFarmerId, farmerId);
        List<OrderItem> items = orderItemMapper.selectList(itemWrapper);

        if (items.isEmpty()) {
            return 0;
        }

        List<Long> orderIds = items.stream()
            .map(OrderItem::getOrderId)
            .distinct()
            .collect(Collectors.toList());

        // 1. 统计今日已支付的订单数（status>=1，不管当前状态，只要支付过就算）
        LambdaQueryWrapper<OrderInfo> paidWrapper = new LambdaQueryWrapper<>();
        paidWrapper.in(OrderInfo::getId, orderIds);
        paidWrapper.ge(OrderInfo::getCreateTime, LocalDate.now().atStartOfDay());
        paidWrapper.lt(OrderInfo::getCreateTime, LocalDate.now().plusDays(1).atStartOfDay());
        paidWrapper.ge(OrderInfo::getStatus, 1); // 已支付及以上
        int paidCount = orderInfoMapper.selectCount(paidWrapper).intValue();

        // 2. 统计今日商家取消的订单数（status=4且cancelType=2）
        LambdaQueryWrapper<OrderInfo> cancelWrapper = new LambdaQueryWrapper<>();
        cancelWrapper.in(OrderInfo::getId, orderIds);
        cancelWrapper.ge(OrderInfo::getCreateTime, LocalDate.now().atStartOfDay());
        cancelWrapper.lt(OrderInfo::getCreateTime, LocalDate.now().plusDays(1).atStartOfDay());
        cancelWrapper.eq(OrderInfo::getStatus, 4); // 已取消
        cancelWrapper.eq(OrderInfo::getCancelType, 2); // 商家取消
        int cancelCount = orderInfoMapper.selectCount(cancelWrapper).intValue();

        // 今日订单 = 已支付订单数 - 商家取消订单数
        return paidCount - cancelCount;
    }

    @Override
    public BigDecimal getTotalIncome(Long farmerId) {
        // 查询该农户的所有订单项收入
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getFarmerId, farmerId);
        List<OrderItem> items = orderItemMapper.selectList(itemWrapper);
        
        if (items.isEmpty()) {
            return new BigDecimal("0.00");
        }
        
        List<Long> orderIds = items.stream()
            .map(OrderItem::getOrderId)
            .distinct()
            .collect(Collectors.toList());
        
        // 查询已支付的订单
        LambdaQueryWrapper<OrderInfo> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.in(OrderInfo::getId, orderIds);
        orderWrapper.ge(OrderInfo::getStatus, 1); // 已支付及以上
        
        List<OrderInfo> orders = orderInfoMapper.selectList(orderWrapper);
        
        BigDecimal income = BigDecimal.ZERO;
        for (OrderInfo order : orders) {
            LambdaQueryWrapper<OrderItem> farmerItemWrapper = new LambdaQueryWrapper<>();
            farmerItemWrapper.eq(OrderItem::getOrderId, order.getId());
            farmerItemWrapper.eq(OrderItem::getFarmerId, farmerId);
            List<OrderItem> farmerItems = orderItemMapper.selectList(farmerItemWrapper);
            
            for (OrderItem item : farmerItems) {
                if (item.getTotalAmount() != null) {
                    income = income.add(item.getTotalAmount());
                }
            }
        }
        
        return income;
    }

    @Override
    public int getTotalOrderCount(Long farmerId) {
        // 查询包含该农户商品的所有订单数
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getFarmerId, farmerId);
        List<OrderItem> items = orderItemMapper.selectList(itemWrapper);
        
        return (int) items.stream()
            .map(OrderItem::getOrderId)
            .distinct()
            .count();
    }

    @Override
    public int getPendingOrderCount(Long farmerId) {
        // 查询待发货的订单数（status=1）
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getFarmerId, farmerId);
        List<OrderItem> items = orderItemMapper.selectList(itemWrapper);
        
        if (items.isEmpty()) {
            return 0;
        }
        
        List<Long> orderIds = items.stream()
            .map(OrderItem::getOrderId)
            .distinct()
            .collect(Collectors.toList());
        
        LambdaQueryWrapper<OrderInfo> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.in(OrderInfo::getId, orderIds);
        orderWrapper.eq(OrderInfo::getStatus, 1); // 待发货

        return orderInfoMapper.selectCount(orderWrapper).intValue();
    }

    @Override
    public int getCompletedOrderCount(Long farmerId) {
        // 查询已完成的订单数（status=3，用户确认收货）
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getFarmerId, farmerId);
        List<OrderItem> items = orderItemMapper.selectList(itemWrapper);

        if (items.isEmpty()) {
            return 0;
        }

        List<Long> orderIds = items.stream()
            .map(OrderItem::getOrderId)
            .distinct()
            .collect(Collectors.toList());

        LambdaQueryWrapper<OrderInfo> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.in(OrderInfo::getId, orderIds);
        orderWrapper.eq(OrderInfo::getStatus, 3); // 已完成

        return orderInfoMapper.selectCount(orderWrapper).intValue();
    }

    @Override
    public int getShippedOrderCount(Long farmerId) {
        // 查询待收货的订单数（status=2，已发货待用户确认收货）
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getFarmerId, farmerId);
        List<OrderItem> items = orderItemMapper.selectList(itemWrapper);

        if (items.isEmpty()) {
            return 0;
        }

        List<Long> orderIds = items.stream()
            .map(OrderItem::getOrderId)
            .distinct()
            .collect(Collectors.toList());

        LambdaQueryWrapper<OrderInfo> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.in(OrderInfo::getId, orderIds);
        orderWrapper.eq(OrderInfo::getStatus, 2); // 待收货（已发货）

        return orderInfoMapper.selectCount(orderWrapper).intValue();
    }

    @Override
    public BigDecimal getMonthIncome(Long farmerId) {
        // 查询本月该农户的收入
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getFarmerId, farmerId);
        List<OrderItem> items = orderItemMapper.selectList(itemWrapper);
        
        if (items.isEmpty()) {
            return new BigDecimal("0.00");
        }
        
        List<Long> orderIds = items.stream()
            .map(OrderItem::getOrderId)
            .distinct()
            .collect(Collectors.toList());
        
        LambdaQueryWrapper<OrderInfo> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.in(OrderInfo::getId, orderIds);
        orderWrapper.ge(OrderInfo::getCreateTime, startOfMonth.atStartOfDay());
        orderWrapper.ge(OrderInfo::getStatus, 1); // 已支付及以上
        
        List<OrderInfo> orders = orderInfoMapper.selectList(orderWrapper);
        
        BigDecimal income = BigDecimal.ZERO;
        for (OrderInfo order : orders) {
            LambdaQueryWrapper<OrderItem> farmerItemWrapper = new LambdaQueryWrapper<>();
            farmerItemWrapper.eq(OrderItem::getOrderId, order.getId());
            farmerItemWrapper.eq(OrderItem::getFarmerId, farmerId);
            List<OrderItem> farmerItems = orderItemMapper.selectList(farmerItemWrapper);
            
            for (OrderItem item : farmerItems) {
                if (item.getTotalAmount() != null) {
                    income = income.add(item.getTotalAmount());
                }
            }
        }
        
        return income;
    }

    @Override
    public List<OrderInfo> getFarmerOrders(Long farmerId, Integer page, Integer size, Integer status, String keyword, String startDate, String endDate) {
        // 1. 先查询包含该农户商品的所有订单项（用于获取订单ID）
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getFarmerId, farmerId);

        List<OrderItem> allItems = orderItemMapper.selectList(itemWrapper);

        // 获取订单ID列表
        List<Long> allOrderIds = allItems.stream()
            .map(OrderItem::getOrderId)
            .distinct()
            .collect(Collectors.toList());

        if (allOrderIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 查询这些订单（排除待付款订单 status=0）
        LambdaQueryWrapper<OrderInfo> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.in(OrderInfo::getId, allOrderIds);
        orderWrapper.ne(OrderInfo::getStatus, 0); // 排除待付款订单

        // 按状态筛选
        if (status != null) {
            orderWrapper.eq(OrderInfo::getStatus, status);
        }

        // 按关键词筛选（订单号）
        if (keyword != null && !keyword.isEmpty()) {
            orderWrapper.like(OrderInfo::getOrderNo, keyword);
        }

        // 按日期区间筛选
        if (startDate != null && !startDate.isEmpty()) {
            orderWrapper.ge(OrderInfo::getCreateTime, startDate + " 00:00:00");
        }
        if (endDate != null && !endDate.isEmpty()) {
            orderWrapper.le(OrderInfo::getCreateTime, endDate + " 23:59:59");
        }

        // 按创建时间倒序
        orderWrapper.orderByDesc(OrderInfo::getCreateTime);

        // 分页
        orderWrapper.last("LIMIT " + (page - 1) * size + ", " + size);

        List<OrderInfo> orders = orderInfoMapper.selectList(orderWrapper);

        // 3. 如果有商品名称关键词，需要额外筛选包含该商品的订单
        if (keyword != null && !keyword.isEmpty()) {
            // 查询包含该商品名称的订单项
            LambdaQueryWrapper<OrderItem> keywordItemWrapper = new LambdaQueryWrapper<>();
            keywordItemWrapper.eq(OrderItem::getFarmerId, farmerId);
            keywordItemWrapper.like(OrderItem::getProductName, keyword);
            List<OrderItem> keywordItems = orderItemMapper.selectList(keywordItemWrapper);

            // 获取这些订单ID
            List<Long> keywordOrderIds = keywordItems.stream()
                .map(OrderItem::getOrderId)
                .distinct()
                .collect(Collectors.toList());

            // 查询这些订单（合并到之前的订单列表）
            if (!keywordOrderIds.isEmpty()) {
                LambdaQueryWrapper<OrderInfo> keywordOrderWrapper = new LambdaQueryWrapper<>();
                keywordOrderWrapper.in(OrderInfo::getId, keywordOrderIds);

                // 按状态筛选
                if (status != null) {
                    keywordOrderWrapper.eq(OrderInfo::getStatus, status);
                }

                // 按日期区间筛选
                if (startDate != null && !startDate.isEmpty()) {
                    keywordOrderWrapper.ge(OrderInfo::getCreateTime, startDate + " 00:00:00");
                }
                if (endDate != null && !endDate.isEmpty()) {
                    keywordOrderWrapper.le(OrderInfo::getCreateTime, endDate + " 23:59:59");
                }

                // 排除已经通过订单号搜索到的订单
                List<Long> existingOrderIds = orders.stream()
                    .map(OrderInfo::getId)
                    .collect(Collectors.toList());

                if (!existingOrderIds.isEmpty()) {
                    keywordOrderWrapper.notIn(OrderInfo::getId, existingOrderIds);
                }

                keywordOrderWrapper.orderByDesc(OrderInfo::getCreateTime);
                keywordOrderWrapper.last("LIMIT " + (page - 1) * size + ", " + size);

                List<OrderInfo> keywordOrders = orderInfoMapper.selectList(keywordOrderWrapper);
                orders.addAll(keywordOrders);
            }
        }

        // 4. 查询每个订单的商品项（只包含该农户的商品）
        for (OrderInfo order : orders) {
            LambdaQueryWrapper<OrderItem> orderItemWrapper = new LambdaQueryWrapper<>();
            orderItemWrapper.eq(OrderItem::getOrderId, order.getId());
            orderItemWrapper.eq(OrderItem::getFarmerId, farmerId);
            List<OrderItem> orderItems = orderItemMapper.selectList(orderItemWrapper);
            order.setItems(orderItems);
        }

        return orders;
    }

    @Override
    public OrderInfo getFarmerOrderDetail(Long id, Long farmerId) {
        // 查询订单
        OrderInfo order = orderInfoMapper.selectById(id);
        if (order == null) {
            return null;
        }
        
        // 查询该农户的商品项
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, id);
        wrapper.eq(OrderItem::getFarmerId, farmerId);
        List<OrderItem> items = orderItemMapper.selectList(wrapper);
        
        if (items.isEmpty()) {
            return null;
        }
        
        order.setItems(items);
        return order;
    }

    @Override
    @Transactional
    public boolean shipOrder(Long id, Long farmerId, String logisticsNo, String logisticsCompany,
                             String senderName, String senderPhone, String senderAddress) {
        // 验证订单包含该农户的商品
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getOrderId, id);
        itemWrapper.eq(OrderItem::getFarmerId, farmerId);
        long count = orderItemMapper.selectCount(itemWrapper);

        if (count == 0) {
            return false;
        }

        // 更新订单状态为待收货，并保存物流和发货人信息
        OrderInfo order = new OrderInfo();
        order.setId(id);
        order.setStatus(2);
        order.setShipTime(LocalDateTime.now());
        order.setLogisticsNo(logisticsNo);
        order.setLogisticsCompany(logisticsCompany);
        order.setSenderName(senderName);
        order.setSenderPhone(senderPhone);
        order.setSenderAddress(senderAddress);
        order.setUpdateTime(LocalDateTime.now());

        int rows = orderInfoMapper.updateById(order);

        if (rows > 0) {
            // 创建物流记录（状态为待揽件，等待配送员揽件）
            Logistics logistics = new Logistics();
            logistics.setOrderId(id);
            logistics.setTrackingNo(logisticsNo);
            logistics.setStatus(0); // 待揽件
            logistics.setCreateTime(LocalDateTime.now());
            logistics.setUpdateTime(LocalDateTime.now());
            logisticsMapper.insert(logistics);

            // 创建物流轨迹记录
            // 将物流公司代码转换为中文名称
            String companyName;
            switch (logisticsCompany) {
                case "sf":
                    companyName = "顺丰速运";
                    break;
                case "zt":
                    companyName = "中通快递";
                    break;
                case "yt":
                    companyName = "圆通快递";
                    break;
                case "yd":
                    companyName = "韵达快递";
                    break;
                default:
                    companyName = logisticsCompany;
            }
            LogisticsTrack track = new LogisticsTrack();
            track.setLogisticsId(logistics.getId());
            track.setStatus(0);
            track.setDescription("商家已发货，物流公司为" + companyName + "，运单号" + logisticsNo + "，等待配送员揽件");
            track.setLocation(senderAddress);
            track.setCreateTime(LocalDateTime.now());
            logisticsTrackMapper.insert(track);
            
            // 发送发货通知给用户
            OrderInfo shippedOrder = orderInfoMapper.selectById(id);
            if (shippedOrder != null) {
                sendOrderNotification(shippedOrder.getUserId(), shippedOrder.getOrderNo(), 
                    "已发货，" + companyName + "，运单号" + logisticsNo);
            }
        }

        return rows > 0;
    }

    @Override
    @Transactional
    public boolean cancelOrderByFarmer(Long id, Long farmerId) {
        // 验证订单包含该农户的商品
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getOrderId, id);
        itemWrapper.eq(OrderItem::getFarmerId, farmerId);
        long count = orderItemMapper.selectCount(itemWrapper);

        if (count == 0) {
            return false;
        }

        // 只能取消待发货的订单（status=1）
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getId, id);
        wrapper.eq(OrderInfo::getStatus, 1); // 待发货

        OrderInfo order = new OrderInfo();
        order.setStatus(4); // 已取消
        order.setCancelType(2); // 商家取消
        order.setUpdateTime(LocalDateTime.now());

        int rows = orderInfoMapper.update(order, wrapper);
        return rows > 0;
    }

    @Override
    public String generateTrackingNo(String company) {
        // 生成物流单号：公司代码 + 13位时间戳 + 3位随机数（共19位）
        long timestamp = System.currentTimeMillis();
        String timePart = String.valueOf(timestamp).substring(0, 13);
        int randomPart = (int) (Math.random() * 900) + 100; // 100-999
        return company + timePart + randomPart;
    }

    @Override
    public Map<String, Object> getFarmerIncomeStats(Long farmerId, String startDate, String endDate) {
        Map<String, Object> stats = new HashMap<>();
        
        // 解析日期
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        
        // 查询该农户在日期范围内的所有订单项
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getFarmerId, farmerId);
        List<OrderItem> items = orderItemMapper.selectList(itemWrapper);
        
        if (items.isEmpty()) {
            stats.put("totalIncome", new BigDecimal("0.00"));
            stats.put("orderCount", 0);
            stats.put("dailyStats", new ArrayList<>());
            return stats;
        }
        
        // 获取订单ID列表
        List<Long> orderIds = items.stream()
            .map(OrderItem::getOrderId)
            .distinct()
            .collect(Collectors.toList());
        
        // 查询日期范围内已支付的订单
        LambdaQueryWrapper<OrderInfo> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.in(OrderInfo::getId, orderIds);
        orderWrapper.ge(OrderInfo::getCreateTime, start.atStartOfDay());
        orderWrapper.lt(OrderInfo::getCreateTime, end.plusDays(1).atStartOfDay());
        orderWrapper.ge(OrderInfo::getStatus, 1); // 已支付及以上
        orderWrapper.orderByDesc(OrderInfo::getCreateTime);
        
        List<OrderInfo> orders = orderInfoMapper.selectList(orderWrapper);
        
        // 按日期分组统计
        Map<LocalDate, List<OrderInfo>> ordersByDate = orders.stream()
            .collect(Collectors.groupingBy(o -> o.getCreateTime().toLocalDate()));
        
        // 计算总收入和订单数
        BigDecimal totalIncome = BigDecimal.ZERO;
        int totalOrderCount = 0;
        
        // 构建每日统计
        List<Map<String, Object>> dailyStats = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        for (Map.Entry<LocalDate, List<OrderInfo>> entry : ordersByDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<OrderInfo> dayOrders = entry.getValue();
            
            // 计算该日期的订单项
            List<Long> dayOrderIds = dayOrders.stream()
                .map(OrderInfo::getId)
                .collect(Collectors.toList());
            
            List<OrderItem> dayItems = items.stream()
                .filter(item -> dayOrderIds.contains(item.getOrderId()))
                .collect(Collectors.toList());
            
            // 计算该日期的收入
            BigDecimal dayOrderAmount = dayItems.stream()
                .map(OrderItem::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // 计算运费（假设运费由平台承担，农户不承担）
            BigDecimal dayFreight = BigDecimal.ZERO;
            
            // 计算平台佣金（假设佣金比例为5%）
            BigDecimal dayCommission = dayOrderAmount.multiply(new BigDecimal("0.05"));
            
            // 实际收入 = 订单金额 - 佣金
            BigDecimal dayActualIncome = dayOrderAmount.subtract(dayCommission);
            
            // 统计订单数（去重）
            int dayOrderCount = dayOrderIds.size();
            
            Map<String, Object> dayStat = new HashMap<>();
            dayStat.put("date", date.format(formatter));
            dayStat.put("orderCount", dayOrderCount);
            dayStat.put("orderAmount", dayOrderAmount.setScale(2, BigDecimal.ROUND_HALF_UP));
            dayStat.put("freight", dayFreight.setScale(2, BigDecimal.ROUND_HALF_UP));
            dayStat.put("commission", dayCommission.setScale(2, BigDecimal.ROUND_HALF_UP));
            dayStat.put("actualIncome", dayActualIncome.setScale(2, BigDecimal.ROUND_HALF_UP));
            dayStat.put("status", "settled"); // 默认已结算
            
            dailyStats.add(dayStat);
            
            totalIncome = totalIncome.add(dayActualIncome);
            totalOrderCount += dayOrderCount;
        }
        
        // 按日期排序
        dailyStats.sort((a, b) -> ((String) b.get("date")).compareTo((String) a.get("date")));
        
        stats.put("totalIncome", totalIncome.setScale(2, BigDecimal.ROUND_HALF_UP));
        stats.put("orderCount", totalOrderCount);
        stats.put("dailyStats", dailyStats);
        
        return stats;
    }

    @Override
    public List<Map<String, Object>> getHotProducts(int limit) {
        // 查询所有在售商品，按销量排序
        LambdaQueryWrapper<Product> productWrapper = new LambdaQueryWrapper<>();
        productWrapper.eq(Product::getStatus, 1)
                      .orderByDesc(Product::getSales)
                      .last("LIMIT " + limit);
        List<Product> products = productMapper.selectList(productWrapper);
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Product product : products) {
            Map<String, Object> productStat = new HashMap<>();
            productStat.put("name", product.getName());
            productStat.put("sales", product.getSales() != null ? product.getSales() : 0);
            // 计算销售额（销量 * 单价）
            BigDecimal amount = product.getPrice()
                .multiply(new BigDecimal(product.getSales() != null ? product.getSales() : 0));
            productStat.put("amount", "¥" + amount.setScale(2, BigDecimal.ROUND_HALF_UP).toString());
            
            result.add(productStat);
        }
        
        return result;
    }

    @Override
    public List<Map<String, Object>> getTopFarmers(int limit) {
        // 查询所有订单项，按农户分组统计
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        List<OrderItem> items = orderItemMapper.selectList(wrapper);

        // 按农户ID分组统计（过滤掉farmerId为null的记录）
        Map<Long, List<OrderItem>> itemsByFarmer = items.stream()
            .filter(item -> item.getFarmerId() != null)
            .collect(Collectors.groupingBy(OrderItem::getFarmerId));
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Map.Entry<Long, List<OrderItem>> entry : itemsByFarmer.entrySet()) {
            Long farmerId = entry.getKey();
            List<OrderItem> farmerItems = entry.getValue();

            // 查询农户信息（从用户表中获取农户名称）
            User farmer = userMapper.selectById(farmerId);
            String farmerName = farmer != null ? farmer.getNickname() : "农户" + farmerId;
            
            // 获取该农户的所有订单ID
            List<Long> orderIds = farmerItems.stream()
                .map(OrderItem::getOrderId)
                .distinct()
                .collect(Collectors.toList());
            
            // 查询订单状态，只统计已支付的订单
            LambdaQueryWrapper<OrderInfo> orderWrapper = new LambdaQueryWrapper<>();
            orderWrapper.in(OrderInfo::getId, orderIds);
            orderWrapper.ge(OrderInfo::getStatus, 1); // 已支付及以上
            List<OrderInfo> orders = orderInfoMapper.selectList(orderWrapper);
            
            // 计算订单数和销售额（只统计已支付订单）
            List<Long> paidOrderIds = orders.stream()
                .map(OrderInfo::getId)
                .collect(Collectors.toList());
            
            int orderCount = paidOrderIds.size();
            BigDecimal amount = farmerItems.stream()
                .filter(item -> paidOrderIds.contains(item.getOrderId()))
                .map(item -> item.getTotalAmount() != null ? item.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            Map<String, Object> farmerStat = new HashMap<>();
            farmerStat.put("name", farmerName != null ? farmerName : "农户" + farmerId);
            farmerStat.put("orders", orderCount);
            farmerStat.put("amount", "¥" + amount.setScale(2, BigDecimal.ROUND_HALF_UP).toString());
            
            result.add(farmerStat);
        }
        
        // 按销售额排序，取前N个
        result.sort((a, b) -> {
            String amountA = ((String) a.get("amount")).replace("¥", "").replace(",", "");
            String amountB = ((String) b.get("amount")).replace("¥", "").replace(",", "");
            return new BigDecimal(amountB).compareTo(new BigDecimal(amountA));
        });
        return result.stream().limit(limit).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getTradeTrend(int days) {
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        
        for (int i = days - 1; i >= 0; i--) {
            LocalDateTime date = now.minusDays(i);
            LocalDateTime dayStart = date.withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime dayEnd = date.withHour(23).withMinute(59).withSecond(59).withNano(999999999);
            
            // 查询该日期的订单
            LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.ge(OrderInfo::getCreateTime, dayStart);
            wrapper.le(OrderInfo::getCreateTime, dayEnd);
            List<OrderInfo> orders = orderInfoMapper.selectList(wrapper);
            
            // 统计订单数和交易额
            int orderCount = orders.size();
            BigDecimal amount = orders.stream()
                .filter(o -> o.getStatus() >= 1) // 已支付及以上
                .map(o -> o.getPayAmount() != null ? o.getPayAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            Map<String, Object> dayStat = new HashMap<>();
            dayStat.put("date", dayStart.format(formatter));
            dayStat.put("orders", orderCount);
            dayStat.put("amount", amount);
            
            result.add(dayStat);
        }
        
        return result;
    }

    /**
     * 发送订单状态变更通知
     */
    private void sendOrderNotification(Long userId, String orderNo, String statusText) {
        try {
            UserMessage message = new UserMessage();
            message.setUserId(userId);
            message.setTitle("订单状态更新");
            message.setContent("您的订单 " + orderNo + " " + statusText);
            message.setType(3); // 订单通知
            message.setIsRead(0);
            message.setCreateTime(LocalDateTime.now());
            userMessageRepository.insert(message);
        } catch (Exception e) {
            System.err.println("发送订单通知失败: " + e.getMessage());
        }
    }

    // ========== 定时任务相关方法实现 ==========

    @Override
    @Transactional
    public int autoConfirmReceive() {
        // 查询发货超过15天且未确认的订单（status=2 待收货）
        LocalDateTime fifteenDaysAgo = LocalDateTime.now().minusDays(15);
        
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getStatus, 2); // 待收货状态
        wrapper.le(OrderInfo::getShipTime, fifteenDaysAgo); // 发货时间超过15天
        wrapper.isNotNull(OrderInfo::getShipTime); // 确保有发货时间
        
        List<OrderInfo> orders = orderInfoMapper.selectList(wrapper);
        int count = 0;
        
        for (OrderInfo order : orders) {
            try {
                // 更新订单状态为已完成
                order.setStatus(3); // 已完成
                order.setReceiveTime(LocalDateTime.now());
                order.setUpdateTime(LocalDateTime.now());
                orderInfoMapper.updateById(order);
                
                // 增加商品销量统计
                increaseProductSales(order.getId());
                
                // 发送自动确认收货通知
                sendOrderNotification(order.getUserId(), order.getOrderNo(), "已自动确认收货");
                
                // 添加物流轨迹记录
                addAutoConfirmLogisticsTrack(order.getId());
                
                count++;
                System.out.println("自动确认收货成功，订单号：" + order.getOrderNo());
            } catch (Exception e) {
                System.err.println("自动确认收货失败，订单ID：" + order.getId() + "，错误：" + e.getMessage());
            }
        }
        
        return count;
    }

    @Override
    @Transactional
    public int autoCancelUnpaidOrders() {
        // 查询创建超过30分钟未支付的订单（status=0 待付款）
        LocalDateTime thirtyMinutesAgo = LocalDateTime.now().minusMinutes(30);
        
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getStatus, 0); // 待付款状态
        wrapper.le(OrderInfo::getCreateTime, thirtyMinutesAgo); // 创建时间超过30分钟
        
        List<OrderInfo> orders = orderInfoMapper.selectList(wrapper);
        int count = 0;
        
        for (OrderInfo order : orders) {
            try {
                // 恢复商品库存
                restoreStock(order.getId());
                
                // 更新订单状态为已取消
                order.setStatus(4); // 已取消
                order.setCancelType(1); // 系统自动取消
                order.setUpdateTime(LocalDateTime.now());
                orderInfoMapper.updateById(order);
                
                // 发送订单取消通知
                sendOrderNotification(order.getUserId(), order.getOrderNo(), "因超时未支付已自动取消");
                
                count++;
                System.out.println("自动取消订单成功，订单号：" + order.getOrderNo());
            } catch (Exception e) {
                System.err.println("自动取消订单失败，订单ID：" + order.getId() + "，错误：" + e.getMessage());
            }
        }
        
        return count;
    }

    @Override
    @Transactional
    public void restoreStock(Long orderId) {
        // 查询订单商品项
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, orderId);
        List<OrderItem> items = orderItemMapper.selectList(wrapper);
        
        for (OrderItem item : items) {
            if (item.getProductId() != null && item.getQuantity() != null) {
                // 恢复商品库存
                Product product = productMapper.selectById(item.getProductId());
                if (product != null) {
                    product.setStock(product.getStock() + item.getQuantity());
                    productMapper.updateById(product);
                    System.out.println("恢复库存成功，商品：" + product.getName() + "，数量：" + item.getQuantity());
                }
            }
        }
    }

    @Override
    @Transactional
    public void increaseProductSales(Long orderId) {
        // 查询订单商品项
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, orderId);
        List<OrderItem> items = orderItemMapper.selectList(wrapper);
        
        for (OrderItem item : items) {
            if (item.getProductId() != null && item.getQuantity() != null) {
                // 增加商品销量
                Product product = productMapper.selectById(item.getProductId());
                if (product != null) {
                    product.setSales(product.getSales() + item.getQuantity());
                    productMapper.updateById(product);
                    System.out.println("增加销量成功，商品：" + product.getName() + "，数量：" + item.getQuantity());
                }
            }
        }
    }

    /**
     * 添加自动确认收货的物流轨迹记录
     */
    private void addAutoConfirmLogisticsTrack(Long orderId) {
        try {
            // 查询物流记录
            LambdaQueryWrapper<Logistics> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Logistics::getOrderId, orderId);
            Logistics logistics = logisticsMapper.selectOne(wrapper);
            
            if (logistics != null) {
                // 添加签收轨迹
                LogisticsTrack track = new LogisticsTrack();
                track.setLogisticsId(logistics.getId());
                track.setStatus(3);
                track.setDescription("订单已自动确认收货，感谢您的购买");
                track.setLocation("用户地址");
                track.setCreateTime(LocalDateTime.now());
                logisticsTrackMapper.insert(track);
                
                // 更新物流状态为已签收
                logistics.setStatus(3);
                logistics.setSignTime(LocalDateTime.now());
                logistics.setUpdateTime(LocalDateTime.now());
                logisticsMapper.updateById(logistics);
            }
        } catch (Exception e) {
            System.err.println("添加物流轨迹失败，订单ID：" + orderId + "，错误：" + e.getMessage());
        }
    }
}
