package com.rural.ecommerce.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务配置类
 * 启用Spring Boot的定时任务功能
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // 配置类本身不需要额外代码，@EnableScheduling注解启用定时任务功能
}
