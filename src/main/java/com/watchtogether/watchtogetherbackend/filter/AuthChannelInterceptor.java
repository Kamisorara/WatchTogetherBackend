package com.watchtogether.watchtogetherbackend.filter;

import com.watchtogether.watchtogetherbackend.entity.userdetail.LoginUser;
import com.watchtogether.watchtogetherbackend.utils.JWTUtil;
import com.watchtogether.watchtogetherbackend.utils.RedisCache;
import jakarta.annotation.Resource;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthChannelInterceptor implements ChannelInterceptor {
    @Resource
    private RedisCache redisCache;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        // 获取 "token" 请求头
        String token = accessor.getFirstNativeHeader("token");
        if (token != null && !token.isEmpty()) {
            try {
                // 验证 token 并获取用户信息
                String userId = JWTUtil.parseJWT(token).getSubject();
                String redisKey = "login:" + userId;
                LoginUser loginUser = redisCache.getCacheObject(redisKey);

                if (loginUser != null) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                // 如果 token 无效，您可以记录错误或拒绝连接
                return null; // 返回 null 以拒绝不合法的连接
            }
        }
        return message; // 如果验证成功，则继续处理
    }
}
