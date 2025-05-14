package com.wt.common.utils;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
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

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private DefaultRedisScript<Long> rateLimiterScript;

    /**
     * 初始化限流脚本
     */
    @PostConstruct
    public void init() {
        try {
            System.out.println("开始加载Redis限流脚本");
            ClassPathResource scriptResource = new ClassPathResource("scripts/rate_limiter.lua");
            if (!scriptResource.exists()) {
                System.err.println("脚本文件不存在: scripts/rate_limiter.lua");
                throw new RuntimeException("脚本文件不存在");
            }

            String scriptContent = StreamUtils.copyToString(
                    scriptResource.getInputStream(),
                    StandardCharsets.UTF_8
            );
            System.out.println("脚本加载成功，内容长度: " + scriptContent.length());

            rateLimiterScript = new DefaultRedisScript<>(scriptContent, Long.class);
            System.out.println("Redis限流脚本初始化完成");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("无法加载限流脚本", e);
        }
    }

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
        try {
            String md5Key = DigestUtils.md5DigestAsHex(key.getBytes());
            String redisKey = "rate_bucket:" + md5Key;

            if (capacity <= 0 || rate <= 0 || requested <= 0) {
                return true;
            }

            // 使用StringRedisTemplate执行脚本
            Long result = stringRedisTemplate.execute(
                    rateLimiterScript,
                    Collections.singletonList(redisKey),
                    String.valueOf(Math.max(capacity, 1)),
                    String.valueOf(System.currentTimeMillis() / 1000),
                    String.valueOf(Math.max(rate, 0.1)),
                    String.valueOf(Math.max(requested, 1))
            );
//            System.out.println(result);
            return result != null && result == 1L;
        } catch (Exception e) {
            throw new RuntimeException("令牌桶限流执行失败", e);
        }
    }

    /**
     * 简易计数器限流
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
