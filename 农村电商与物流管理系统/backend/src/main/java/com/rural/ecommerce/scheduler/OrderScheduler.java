package com.rural.ecommerce.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rural.ecommerce.entity.OrderStats;
import com.rural.ecommerce.entity.ScheduledTaskLog;
import com.rural.ecommerce.entity.User;
import com.rural.ecommerce.mapper.OrderInfoMapper;
import com.rural.ecommerce.mapper.OrderStatsMapper;
import com.rural.ecommerce.mapper.ScheduledTaskLogMapper;
import com.rural.ecommerce.mapper.UserMapper;
import com.rural.ecommerce.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * 订单相关定时任务调度器
 * 包含自动确认收货、自动取消订单、订单统计等定时任务
 */
@Component
public class OrderScheduler {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderStatsMapper orderStatsMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ScheduledTaskLogMapper scheduledTaskLogMapper;

    /**
     * 自动确认收货定时任务
     * 每天凌晨2点执行，处理发货后15天未确认的订单
     * cron表达式：秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void autoConfirmReceiveTask() {
        String taskName = "autoConfirmReceive";
        String taskDesc = "自动确认收货（发货后15天）";
        LocalDateTime startTime = LocalDateTime.now();
        
        System.out.println("【定时任务】开始执行自动确认收货任务，时间：" + startTime);
        
        try {
            // 执行自动确认收货
            int count = orderService.autoConfirmReceive();
            
            LocalDateTime endTime = LocalDateTime.now();
            long executionTime = java.time.Duration.between(startTime, endTime).toMillis();
            
            // 记录执行日志
            saveTaskLog(taskName, taskDesc, 1, "成功处理 " + count + " 个订单", startTime, endTime, executionTime, null);
            
            System.out.println("【定时任务】自动确认收货任务执行完成，处理了 " + count + " 个订单，耗时：" + executionTime + "ms");
        } catch (Exception e) {
            LocalDateTime endTime = LocalDateTime.now();
            long executionTime = java.time.Duration.between(startTime, endTime).toMillis();
            
            // 记录失败日志
            saveTaskLog(taskName, taskDesc, 0, "执行失败", startTime, endTime, executionTime, e.getMessage());
            
            System.err.println("【定时任务】自动确认收货任务执行失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 自动取消未支付订单定时任务
     * 每5分钟执行一次，处理创建后30分钟未支付的订单
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void autoCancelUnpaidOrdersTask() {
        String taskName = "autoCancelUnpaidOrders";
        String taskDesc = "自动取消未支付订单（30分钟）";
        LocalDateTime startTime = LocalDateTime.now();
        
        System.out.println("【定时任务】开始执行自动取消未支付订单任务，时间：" + startTime);
        
        try {
            // 执行自动取消订单
            int count = orderService.autoCancelUnpaidOrders();
            
            LocalDateTime endTime = LocalDateTime.now();
            long executionTime = java.time.Duration.between(startTime, endTime).toMillis();
            
            // 记录执行日志
            String result = count > 0 ? "成功取消 " + count + " 个订单" : "没有需要取消的订单";
            saveTaskLog(taskName, taskDesc, 1, result, startTime, endTime, executionTime, null);
            
            System.out.println("【定时任务】自动取消未支付订单任务执行完成，" + result + "，耗时：" + executionTime + "ms");
        } catch (Exception e) {
            LocalDateTime endTime = LocalDateTime.now();
            long executionTime = java.time.Duration.between(startTime, endTime).toMillis();
            
            // 记录失败日志
            saveTaskLog(taskName, taskDesc, 0, "执行失败", startTime, endTime, executionTime, e.getMessage());
            
            System.err.println("【定时任务】自动取消未支付订单任务执行失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 每日订单统计定时任务
     * 每天凌晨1点执行，生成前一天的订单统计报表
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void dailyOrderStatsTask() {
        String taskName = "dailyOrderStats";
        String taskDesc = "每日订单统计报表生成";
        LocalDateTime startTime = LocalDateTime.now();
        
        System.out.println("【定时任务】开始执行每日订单统计任务，时间：" + startTime);
        
        try {
            // 统计前一天的数据
            LocalDate yesterday = LocalDate.now().minusDays(1);
            
            // 检查是否已存在该日期的统计记录
            OrderStats existingStats = orderStatsMapper.selectByDate(yesterday, 1);
            if (existingStats != null) {
                System.out.println("【定时任务】日期 " + yesterday + " 的统计记录已存在，跳过生成");
                
                LocalDateTime endTime = LocalDateTime.now();
                long executionTime = java.time.Duration.between(startTime, endTime).toMillis();
                saveTaskLog(taskName, taskDesc, 1, "统计记录已存在，跳过", startTime, endTime, executionTime, null);
                return;
            }
            
            // 生成统计报表
            OrderStats stats = generateDailyStats(yesterday);
            
            // 保存统计记录
            orderStatsMapper.insert(stats);
            
            LocalDateTime endTime = LocalDateTime.now();
            long executionTime = java.time.Duration.between(startTime, endTime).toMillis();
            
            String result = String.format("成功生成 %s 的统计报表，订单数：%d，金额：%.2f",
                    yesterday, stats.getTotalOrders(), stats.getTotalAmount());
            saveTaskLog(taskName, taskDesc, 1, result, startTime, endTime, executionTime, null);
            
            System.out.println("【定时任务】每日订单统计任务执行完成，" + result + "，耗时：" + executionTime + "ms");
        } catch (Exception e) {
            LocalDateTime endTime = LocalDateTime.now();
            long executionTime = java.time.Duration.between(startTime, endTime).toMillis();
            
            saveTaskLog(taskName, taskDesc, 0, "执行失败", startTime, endTime, executionTime, e.getMessage());
            
            System.err.println("【定时任务】每日订单统计任务执行失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 每周订单统计定时任务
     * 每周一凌晨3点执行，生成上周的订单统计报表
     */
    @Scheduled(cron = "0 0 3 ? * MON")
    @Transactional
    public void weeklyOrderStatsTask() {
        String taskName = "weeklyOrderStats";
        String taskDesc = "每周订单统计报表生成";
        LocalDateTime startTime = LocalDateTime.now();
        
        System.out.println("【定时任务】开始执行每周订单统计任务，时间：" + startTime);
        
        try {
            // 统计上周的数据（上周一到上周日）
            LocalDate lastWeekEnd = LocalDate.now().minusDays(1); // 上周日
            LocalDate lastWeekStart = lastWeekEnd.minusDays(6); // 上周一
            
            OrderStats stats = generatePeriodStats(lastWeekStart, lastWeekEnd, 2);
            
            // 保存统计记录
            orderStatsMapper.insert(stats);
            
            LocalDateTime endTime = LocalDateTime.now();
            long executionTime = java.time.Duration.between(startTime, endTime).toMillis();
            
            String result = String.format("成功生成 %s 至 %s 的周统计报表",
                    lastWeekStart, lastWeekEnd);
            saveTaskLog(taskName, taskDesc, 1, result, startTime, endTime, executionTime, null);
            
            System.out.println("【定时任务】每周订单统计任务执行完成，" + result + "，耗时：" + executionTime + "ms");
        } catch (Exception e) {
            LocalDateTime endTime = LocalDateTime.now();
            long executionTime = java.time.Duration.between(startTime, endTime).toMillis();
            
            saveTaskLog(taskName, taskDesc, 0, "执行失败", startTime, endTime, executionTime, e.getMessage());
            
            System.err.println("【定时任务】每周订单统计任务执行失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 每月订单统计定时任务
     * 每月1号凌晨4点执行，生成上月的订单统计报表
     */
    @Scheduled(cron = "0 0 4 1 * ?")
    @Transactional
    public void monthlyOrderStatsTask() {
        String taskName = "monthlyOrderStats";
        String taskDesc = "每月订单统计报表生成";
        LocalDateTime startTime = LocalDateTime.now();
        
        System.out.println("【定时任务】开始执行每月订单统计任务，时间：" + startTime);
        
        try {
            // 统计上月的数据
            LocalDate lastMonthEnd = LocalDate.now().minusDays(1); // 上月最后一天
            LocalDate lastMonthStart = lastMonthEnd.withDayOfMonth(1); // 上月第一天
            
            OrderStats stats = generatePeriodStats(lastMonthStart, lastMonthEnd, 3);
            
            // 保存统计记录
            orderStatsMapper.insert(stats);
            
            LocalDateTime endTime = LocalDateTime.now();
            long executionTime = java.time.Duration.between(startTime, endTime).toMillis();
            
            String result = String.format("成功生成 %s 年 %s 月的月统计报表",
                    lastMonthStart.getYear(), lastMonthStart.getMonthValue());
            saveTaskLog(taskName, taskDesc, 1, result, startTime, endTime, executionTime, null);
            
            System.out.println("【定时任务】每月订单统计任务执行完成，" + result + "，耗时：" + executionTime + "ms");
        } catch (Exception e) {
            LocalDateTime endTime = LocalDateTime.now();
            long executionTime = java.time.Duration.between(startTime, endTime).toMillis();
            
            saveTaskLog(taskName, taskDesc, 0, "执行失败", startTime, endTime, executionTime, e.getMessage());
            
            System.err.println("【定时任务】每月订单统计任务执行失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 生成每日订单统计
     */
    private OrderStats generateDailyStats(LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.atTime(LocalTime.MAX);
        
        return generatePeriodStats(dayStart, dayEnd, date, 1);
    }

    /**
     * 生成时间段订单统计
     */
    private OrderStats generatePeriodStats(LocalDate startDate, LocalDate endDate, int statType) {
        LocalDateTime periodStart = startDate.atStartOfDay();
        LocalDateTime periodEnd = endDate.atTime(LocalTime.MAX);
        
        OrderStats stats = generatePeriodStats(periodStart, periodEnd, endDate, statType);
        return stats;
    }

    /**
     * 生成订单统计数据
     */
    private OrderStats generatePeriodStats(LocalDateTime startTime, LocalDateTime endTime, LocalDate statDate, int statType) {
        OrderStats stats = new OrderStats();
        stats.setStatDate(statDate);
        stats.setStatType(statType);
        
        // 使用自定义SQL查询统计数据
        Map<String, Object> result = orderInfoMapper.selectOrderStats(startTime, endTime);
        
        if (result != null) {
            stats.setTotalOrders(((Number) result.getOrDefault("totalOrders", 0)).intValue());
            stats.setPaidOrders(((Number) result.getOrDefault("paidOrders", 0)).intValue());
            stats.setCancelledOrders(((Number) result.getOrDefault("cancelledOrders", 0)).intValue());
            stats.setCompletedOrders(((Number) result.getOrDefault("completedOrders", 0)).intValue());
            stats.setTotalAmount(new BigDecimal(result.getOrDefault("totalAmount", 0).toString()));
            stats.setPaidAmount(new BigDecimal(result.getOrDefault("paidAmount", 0).toString()));
            stats.setRefundAmount(new BigDecimal(result.getOrDefault("refundAmount", 0).toString()));
        } else {
            stats.setTotalOrders(0);
            stats.setPaidOrders(0);
            stats.setCancelledOrders(0);
            stats.setCompletedOrders(0);
            stats.setTotalAmount(BigDecimal.ZERO);
            stats.setPaidAmount(BigDecimal.ZERO);
            stats.setRefundAmount(BigDecimal.ZERO);
        }
        
        // 统计新增用户数
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.ge(User::getCreateTime, startTime)
                   .le(User::getCreateTime, endTime);
        Integer newUsers = userMapper.selectCount(userWrapper).intValue();
        stats.setNewUsers(newUsers);
        
        // 统计活跃用户数（简化计算：有订单的用户）
        Integer activeUsers = orderInfoMapper.selectActiveUserCount(startTime, endTime);
        stats.setActiveUsers(activeUsers != null ? activeUsers : 0);
        
        return stats;
    }

    /**
     * 保存定时任务执行日志
     */
    private void saveTaskLog(String taskName, String taskDesc, Integer status, String result,
                             LocalDateTime startTime, LocalDateTime endTime, Long executionTime, String errorMsg) {
        try {
            ScheduledTaskLog log = new ScheduledTaskLog();
            log.setTaskName(taskName);
            log.setTaskDesc(taskDesc);
            log.setStatus(status);
            log.setResult(result);
            log.setStartTime(startTime);
            log.setEndTime(endTime);
            log.setExecutionTime(executionTime);
            log.setErrorMsg(errorMsg);
            
            scheduledTaskLogMapper.insert(log);
        } catch (Exception e) {
            System.err.println("保存定时任务日志失败：" + e.getMessage());
        }
    }
}
