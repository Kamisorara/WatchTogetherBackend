package com.wt.web.core.interceptor;

import com.wt.common.utils.RateLimiterUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 限流拦截器
 */
@Component
@Slf4j
public class RateLimitWebSocketInterceptor implements HandshakeInterceptor {

    @Resource
    private RateLimiterUtils rateLimiterUtils;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String ip = getIpAddress(servletRequest);

            // 同一IP的WebSocket连接限流，每10秒最多5次连接
            boolean allowed = rateLimiterUtils.simpleRateLimit("ws:" + ip, 5, 10);
            if (!allowed) {
                log.warn("WebSocket连接频率过高，IP: {}", ip);
                return false;
            }

            // 全局WebSocket连接数限流，避免资源耗尽
            boolean globalAllowed = rateLimiterUtils.simpleRateLimit("ws:global", 300, 60);
            if (!globalAllowed) {
                log.warn("全局WebSocket连接数达到上限");
                return false;
            }
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 握手后处理
    }

    private String getIpAddress(ServletServerHttpRequest request) {
        String ip = request.getServletRequest().getHeader("X-Forwarded-For");
        // IP获取逻辑...
        return ip != null ? ip : request.getServletRequest().getRemoteAddr();
    }
}