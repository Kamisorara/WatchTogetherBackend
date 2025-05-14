package com.wt.common.annotaion;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    /**
     * 限流关键字，支持SpringEL表达式，如 #user.id, #ip
     */
    String key() default "";

    /**
     * 限流时间窗口(秒) 默认1秒
     */
    int timeWindow() default 1;

    /**
     * 限制次数 默认10次
     */
    int limit() default 10;

    /**
     * 是否应用于全局
     */
    boolean global() default false;

    /**
     * 限流失败提示信息
     */
    String message() default "请求过于频繁，请稍后再试";
}
