package com.wt.web.core.config;

import com.wt.web.core.interceptor.JwtHandshakeInterceptor;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

/**
 * WebSocket配置
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Resource
    private JwtHandshakeInterceptor jwtHandshakeInterceptor;

    @Value("${cors.allowedOrigins}")
    private String allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue"); // 服务器向客户端发送消息
        config.setApplicationDestinationPrefixes("/app"); // 客户端向服务端发送消息
        config.setUserDestinationPrefix("/user"); // 点对点发送消息，给特定用户发送消息
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/websocket")
                .setAllowedOrigins(allowedOrigins)
                .addInterceptors(jwtHandshakeInterceptor)  // jwt interceptor 验证token
                .withSockJS()
                .setInterceptors(new HttpSessionHandshakeInterceptor());
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(64 * 1024)  // 消息大小限制
                .setSendTimeLimit(15 * 1000)      // 发送超时时间
                .setSendBufferSizeLimit(512 * 1024);  // 发送缓冲区大小
    }
}
