package com.wt.web.core.config;

import com.wt.web.core.interceptor.JwtHandshakeInterceptor;
import com.wt.web.core.interceptor.RateLimitWebSocketInterceptor;
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
 * WebSocket通信配置类
 * 负责配置WebSocket消息代理、端点注册和传输参数设置
 * 启用STOMP协议支持，并集成JWT认证验证和请求限流功能
 * 实现了WebSocketMessageBrokerConfigurer接口来定制WebSocket行为
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Resource
    private JwtHandshakeInterceptor jwtHandshakeInterceptor;

    @Resource
    private RateLimitWebSocketInterceptor rateLimitWebSocketInterceptor;

    @Value("${cors.allowedOrigins}")
    private String allowedOrigins;

    /**
     * 配置WebSocket消息代理
     * 设置消息代理的目标前缀和客户端请求的应用前缀
     *
     * @param config 消息代理注册器，用于配置消息通道和目标前缀
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue"); // 服务器向客户端发送消息
        config.setApplicationDestinationPrefixes("/app"); // 客户端向服务端发送消息
        config.setUserDestinationPrefix("/user"); // 点对点发送消息，给特定用户发送消息
    }

    /**
     * 注册STOMP协议的端点
     * 配置WebSocket连接端点，设置跨域源和拦截器
     * 启用SockJS支持，为不支持WebSocket的客户端提供降级选项
     *
     * @param registry STOMP端点注册器，用于注册WebSocket端点和相关配置
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/websocket")
                .setAllowedOrigins(allowedOrigins)
                .addInterceptors(rateLimitWebSocketInterceptor, jwtHandshakeInterceptor)  // jwt interceptor 验证token + ws限流拦截器
                .withSockJS()
                .setInterceptors(new HttpSessionHandshakeInterceptor());
    }

    /**
     * 配置WebSocket传输参数
     * 设置消息大小限制(2MB)、发送超时时间(20秒)和缓冲区大小(4MB)
     * 用于控制WebSocket连接的性能和资源使用
     *
     * @param registration WebSocket传输注册器，用于配置传输层参数
     */
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(2 * 1024 * 1024)  // 消息大小限制
                .setSendTimeLimit(20 * 1000)      // 发送超时时间
                .setSendBufferSizeLimit(4 * 1024 * 1024);  // 发送缓冲区大小
    }
}
