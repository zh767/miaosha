package com.zh.miaosha.service;

import com.google.common.util.concurrent.RateLimiter;
import org.springframework.stereotype.Service;

/**
 * 秒杀前的限流.
 * 使用了Google guava的RateLimiter
 */
@Service
public class AccessLimitServiceImpl {
    /**
     * 每秒钟只发出200个令牌，拿到令牌的请求才可以进入秒杀过程
     */
    private RateLimiter seckillRateLimiter = RateLimiter.create(200);

    /**
     * 尝试获取令牌
     *
     * @return
     */
    public boolean tryAcquireSeckill() {
        return seckillRateLimiter.tryAcquire();
    }
}
