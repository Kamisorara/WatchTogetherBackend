package com.watchtogether.watchtogetherbackend.interceptor;

import com.watchtogether.watchtogetherbackend.entity.userdetail.LoginUser;
import com.watchtogether.watchtogetherbackend.utils.JWTUtil;
import com.watchtogether.watchtogetherbackend.utils.RedisCache;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Component
public class JwtHandshakeInterceptor implements ChannelInterceptor {
    @Resource
    private RedisCache redisCache;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        // 只在 CONNECT 类型消息中处理 token
//        System.out.println(message);
        if (message.getHeaders().get("simpMessageType") == SimpMessageType.CONNECT) {
            // 从 nativeHeaders 中获取 token
            Map<String, List<String>> nativeHeaders = (Map<String, List<String>>) message.getHeaders().get("nativeHeaders");
            if (nativeHeaders != null && nativeHeaders.containsKey("token")) {
                String token = nativeHeaders.get("token").get(0); // 获取 token
//                System.out.println("Received token: " + token);

                // 验证 token
                if (token == null || !isValidToken(token)) {
                    throw new RuntimeException("Invalid or expired token");
                }
            } else {
                throw new RuntimeException("Missing token");
            }
        }
        return message;
    }

    private boolean isValidToken(String token) {
        if (StringUtils.hasText(token)) {
            try {
                Claims claims = JWTUtil.parseJWT(token);
                String userId = claims.getSubject();

                // 从Redis中获取对应的LoginUser
                String redisKey = "login:" + userId;
                LoginUser loginUser = redisCache.getCacheObject(redisKey);
                // 检查Redis缓存中是否存在用户数据
                return loginUser != null;
            } catch (Exception e) {
                // 捕获并处理JWT解析异常
//                System.out.println("JWT非法或已过期: " + e.getMessage());
            }
        }
        return false;
    }
}
