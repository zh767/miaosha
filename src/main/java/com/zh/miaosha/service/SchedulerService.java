package com.zh.miaosha.service;

import com.zh.miaosha.domain.OrderInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 *
 * </p>
 *
 * @author zh
 * @since 2020/9/6
 */
@Service
public class SchedulerService {
    private static final Logger log = LoggerFactory.getLogger(SchedulerService.class);


    @Autowired
    private OrderService orderService;
    @Autowired
    private Environment env;

    //定时获取status=0的订单并判断是否超过TTL，然后进行失效
    @Scheduled(cron = "0 0/30 * * * ?")
    public void schedulerExpireOrders() {
        try {
            List<OrderInfo> list = orderService.selectExpireOrders();
            if (list != null && !list.isEmpty()) {
                //java8的写法
                list.stream().forEach(i -> {
                    if (i != null && i.getDiffTime() > env.getProperty("scheduler.expire.orders.time", Integer.class)) {
                        orderService.expireOrder(i);
                    }
                });
            }
        } catch (Exception e) {
            log.error("定时获取status=0的订单并判断是否超过TTL，然后进行失效-发生异常：", e.fillInStackTrace());
        }
    }
}
