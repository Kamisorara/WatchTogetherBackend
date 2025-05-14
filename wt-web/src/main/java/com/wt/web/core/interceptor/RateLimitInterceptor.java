package com.wt.web.core.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wt.common.annotaion.RateLimit;
import com.wt.common.utils.IpUtils;
import com.wt.common.utils.RateLimiterUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 限流拦截器
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    @Resource
    private RateLimiterUtils rateLimiterUtils;

    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);

        if (rateLimit == null) {
            return true;
        }

        String limitKey = rateLimit.key();
        if (limitKey.isEmpty()) {
            // 默认使用IP地址作为限流键
            limitKey = IpUtils.getIpAddress(request);
        } else if (limitKey.contains("#")) {
            // 解析SpEL表达式
            StandardEvaluationContext context = new StandardEvaluationContext();
            context.setVariable("request", request);
            context.setVariable("ip", IpUtils.getIpAddress(request));

            Expression expression = expressionParser.parseExpression(limitKey);
            limitKey = String.valueOf(expression.getValue(context));
        }

        // 添加API路径信息，避免跨接口共用计数
        String apiPath = request.getRequestURI();
        if (!rateLimit.global()) {
            limitKey = apiPath + ":" + limitKey;
        }

        // 计数器算法
//        boolean allowed = rateLimiterUtils.simpleRateLimit(limitKey, rateLimit.limit(), rateLimit.timeWindow());
        // rate: 令牌生成速率为limit/timeWindow
        // requested: 每个请求消耗1个令牌
        boolean allowed = rateLimiterUtils.tryAcquire(limitKey, rateLimit.limit(),
                (double) rateLimit.limit() / rateLimit.timeWindow(), 1);

        log.info("限流请求: {}, IP: {}, 允许: {}", limitKey, IpUtils.getIpAddress(request), allowed);

        if (!allowed) {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(429);

            Map<String, Object> result = new HashMap<>();
            result.put("code", 429);
            result.put("msg", rateLimit.message());

            response.getOutputStream().write(objectMapper.writeValueAsString(result).getBytes(StandardCharsets.UTF_8));
            return false;
        }

        return true;
    }
}