package com.zh.miaosha.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.Executors;

/**
 * <p>
 * 定时任务多线程处理的通用化配置
 * </p>
 *
 * @author zh
 */
@Configuration
public class SchedulerConfig implements SchedulingConfigurer {
    //针对定时任务调度-配置多线程（线程池）
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(Executors.newScheduledThreadPool(10));
    }
}
