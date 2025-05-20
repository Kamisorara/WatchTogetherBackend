package com.wt.web.core.interceptor;

import com.wt.common.utils.JWTUtil;
import com.wt.common.utils.RedisCache;
import com.wt.service.helper.LoginUser;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;

/**
 * WebSocket握手拦截器
 */
@Component
public class JwtHandshakeInterceptor implements ChannelInterceptor, HandshakeInterceptor {
    @Resource
    private RedisCache redisCache;

    /**
     * 拦截 STOMP 消息通道上的 CONNECT 消息
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        if (message.getHeaders().get("simpMessageType") == SimpMessageType.CONNECT) {
            Map<String, List<String>> nativeHeaders = (Map<String, List<String>>) message.getHeaders().get("nativeHeaders");
            if (nativeHeaders != null && nativeHeaders.containsKey("token")) {
                String token = nativeHeaders.get("token").get(0);
                if (token == null || !isValidToken(token)) {
                    throw new RuntimeException("Token非法或过期");
                }
            } else {
                throw new RuntimeException("未携带Token");
            }
        }
        return message;
    }

    /**
     * 拦截原生 WebSocket 握手请求
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        // 从请求头中获取token并验证，类似HTTP拦截器的逻辑
        String token = request.getHeaders().getFirst("token");

        if (token != null && isValidToken(token)) {
            // 可以将用户信息存入attributes中，以便后续使用
            Claims claims = JWTUtil.parseJWT(token);
            String userId = claims.getSubject();
            attributes.put("userId", userId);
            return true;
        }

        return false;  // 验证失败，拒绝连接
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 握手完成后的操作，可以为空或添加日志记录
    }

    // 保留原有的token验证方法
    private boolean isValidToken(String token) {
        if (StringUtils.hasText(token)) {
            try {
                Claims claims = JWTUtil.parseJWT(token);
                String userId = claims.getSubject();
                String redisKey = "login:" + userId;
                LoginUser loginUser = redisCache.getCacheObject(redisKey);
                return loginUser != null;
            } catch (Exception e) {
                // 捕获并处理JWT解析异常
            }
        }
        return false;
    }
}