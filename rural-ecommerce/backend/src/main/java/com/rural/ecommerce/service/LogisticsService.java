package com.rural.ecommerce.service;

import com.rural.ecommerce.entity.Logistics;
import com.rural.ecommerce.entity.LogisticsTrack;

import java.util.List;
import java.util.Map;

public interface LogisticsService {

    /**
     * 创建物流记录（农户发货时调用）
     */
    void createLogistics(Long orderId, String trackingNo, String logisticsCompany);

    /**
     * 获取工作台统计数据
     */
    Map<String, Object> getStats(Long deliveryId);

    /**
     * 获取待揽件列表
     */
    List<Logistics> getPendingList(Long deliveryId, Integer page, Integer size);

    /**
     * 获取配送中列表
     */
    List<Logistics> getDeliveringList(Long deliveryId, Integer page, Integer size);

    /**
     * 获取今日配送任务
     */
    List<Map<String, Object>> getTodayTasks(Long deliveryId);

    /**
     * 获取配送记录
     */
    List<Logistics> getRecords(Long deliveryId, Integer page, Integer size);

    /**
     * 揽件
     */
    boolean pickup(Long logisticsId, Long deliveryId);

    /**
     * 开始派送
     */
    boolean startDeliver(Long logisticsId, Long deliveryId);

    /**
     * 签收
     */
    boolean sign(Long logisticsId, Long deliveryId, String signer);

    /**
     * 获取物流轨迹
     */
    List<LogisticsTrack> getTracks(Long logisticsId);

    /**
     * 根据订单ID获取物流信息
     */
    Logistics getByOrderId(Long orderId);
}
