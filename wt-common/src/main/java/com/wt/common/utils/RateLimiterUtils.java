package com.wt.common.utils;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 基于Redis限流工具
 */
@Component
public class RateLimiterUtils {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RedisCache redisCache;

    /**
     * 令牌桶算法限流脚本
     * capacity：令牌桶的容量
     * timestamp：当前时间戳，用于计算自上次更新以来的时间差。
     * rate：令牌生成速率（每秒生成多少个令牌）。
     * app：当前请求需要消耗的令牌数。
     * fill_time：填满令牌桶所需的时间（桶的容量 / 令牌生成速率）。
     * ttl：令牌桶状态的过期时间，设置为填满时间的两倍，确保状态不会过早丢失。
     * current_tokens：当前桶中的令牌数，初始值为桶的容量。
     * last_tokens_time：上次更新令牌桶的时间戳，默认为 0。
     * delta：当前时间与上次更新时间的差值，即有多少时间过去了。
     * filled_tokens：计算新的令牌数，按照时间差 delta 补充令牌，但不会超过桶的容量。
     * allowed：判断是否允许请求。如果桶中令牌数 filled_tokens 大于等于请求所需的令牌数 app，则允许请求。
     * new_tokens：更新后的令牌数。如果允许请求，则从桶中扣除相应数量的令牌；否则令牌数不变。
     * 1：表示允许请求（有足够令牌）。
     * 0：表示拒绝请求（令牌不足）。
     */
    private static final String RATE_LIMITER_SCRIPT =
            "local key = KEYS[1] " +
                    "local capacity = tonumber(ARGV[1]) " +
                    "local timestamp = tonumber(ARGV[2]) " +
                    "local rate = tonumber(ARGV[3]) " +
                    "local app = tonumber(ARGV[4]) " +
                    "local fill_time = capacity / rate " +
                    "local ttl = math.floor(fill_time * 2) " +
                    "local current_tokens = tonumber(redis.call('get', key) or capacity) " +
                    "local last_tokens_time = tonumber(redis.call('get', key .. ':timestamp') or 0) " +
                    "local delta = math.max(0, timestamp - last_tokens_time) " +
                    "local filled_tokens = math.min(capacity, current_tokens + (delta * rate)) " +
                    "local allowed = filled_tokens >= app " +
                    "local new_tokens = filled_tokens " +
                    "if allowed then " +
                    "    new_tokens = filled_tokens - app " +
                    "end " +
                    "redis.call('setex', key, ttl, new_tokens) " +
                    "redis.call('setex', key .. ':timestamp', ttl, timestamp) " +
                    "return allowed and 1 or 0";

    /**
     * 使用令牌桶算法 尝试获取令牌
     *
     * @param key       限流关键字 一般是用户请求的唯一标识
     * @param capacity  令牌桶容量
     * @param rate      令牌生成速率(个/秒)
     * @param requested 请求令牌数
     * @return 是否获取到令牌
     */
    public boolean tryAcquire(String key, int capacity, double rate, int requested) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(RATE_LIMITER_SCRIPT, Long.class);
        List<String> keys = Arrays.asList("rate_limiter", key);
        Long result = redisTemplate.execute(script, keys, capacity, System.currentTimeMillis() / 1000, rate, requested);
        // 请求获取到足够令牌 就pass
        return result != null && result == 1L;
    }


    /**
     * 使用简易计数器限流
     *
     * @param key    限流关键字
     * @param limit  限制次数
     * @param period 时间窗口(秒)
     * @return 是否通过限流检查
     */
    public boolean simpleRateLimit(String key, int limit, int period) {
        String limitKey = "rate_limit:" + key;
        Long count = redisCache.getCacheObject(limitKey);

        if (count == null) {
            redisCache.setCacheObject(limitKey, 1L, period, TimeUnit.SECONDS);
            return true;
        } else if (count < limit) {
            redisCache.setCacheObject(limitKey, count + 1L);
            return true;
        }
        // 如果当前计数超过或等于限制值 limit 直接拒绝请求。
        return false;
    }
}
