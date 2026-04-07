package com.rural.ecommerce.service;

import com.rural.ecommerce.entity.OrderInfo;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface OrderService {

    // 获取订单列表
    List<OrderInfo> getOrderList(Long userId, Integer page, Integer size, Integer status);

    // 获取订单详情
    OrderInfo getOrderDetail(Long id, Long userId);

    // 创建订单
    OrderInfo createOrder(OrderInfo orderInfo);

    // 取消订单
    boolean cancelOrder(Long id, Long userId);

    // 确认收货
    boolean confirmReceive(Long id, Long userId);

    // 支付订单
    boolean payOrder(Long id, Long userId);

    // 农户相关方法
    BigDecimal getTodayIncome(Long farmerId);

    int getTodayOrderCount(Long farmerId);

    BigDecimal getTotalIncome(Long farmerId);

    int getTotalOrderCount(Long farmerId);

    int getPendingOrderCount(Long farmerId);

    int getShippedOrderCount(Long farmerId);

    int getCompletedOrderCount(Long farmerId);

    BigDecimal getMonthIncome(Long farmerId);

    List<OrderInfo> getFarmerOrders(Long farmerId, Integer page, Integer size, Integer status, String keyword, String startDate, String endDate);

    OrderInfo getFarmerOrderDetail(Long id, Long farmerId);

    boolean shipOrder(Long id, Long farmerId, String logisticsNo, String logisticsCompany,
                      String senderName, String senderPhone, String senderAddress);

    boolean cancelOrderByFarmer(Long id, Long farmerId);

    // 生成物流单号
    String generateTrackingNo(String company);

    Map<String, Object> getFarmerIncomeStats(Long farmerId, String startDate, String endDate);

    // Dashboard 统计方法
    List<Map<String, Object>> getHotProducts(int limit);

    List<Map<String, Object>> getTopFarmers(int limit);

    List<Map<String, Object>> getTradeTrend(int days);

    // ========== 定时任务相关方法 ==========

    /**
     * 自动确认收货（定时任务调用）
     * 发货后15天自动确认收货
     * @return 处理的订单数量
     */
    int autoConfirmReceive();

    /**
     * 自动取消未支付订单（定时任务调用）
     * 创建后30分钟未支付自动取消
     * @return 处理的订单数量
     */
    int autoCancelUnpaidOrders();

    /**
     * 恢复订单商品的库存
     * @param orderId 订单ID
     */
    void restoreStock(Long orderId);

    /**
     * 增加商品销量统计
     * @param orderId 订单ID
     */
    void increaseProductSales(Long orderId);
}
