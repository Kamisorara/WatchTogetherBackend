package com.wt.web.core.config.security;

import com.wt.common.utils.RedisCache;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * 基于Redis的OAuth2授权请求存储库实现
 * 用于解决使用ngrok等代理时的session丢失问题
 */
@Component
public class RedisAuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final String OAUTH2_AUTHORIZATION_REQUEST_PREFIX = "oauth2_auth_request:";
    private static final int AUTHORIZATION_REQUEST_EXPIRE_TIME = 10; // 10分钟过期时间

    @Resource
    private RedisCache redisCache;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        String stateParameter = getStateParameter(request);
        if (StringUtils.hasText(stateParameter)) {
            String key = OAUTH2_AUTHORIZATION_REQUEST_PREFIX + stateParameter;
            return redisCache.getCacheObject(key);
        }
        return null;
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        if (authorizationRequest == null) {
            String stateParameter = getStateParameter(request);
            if (StringUtils.hasText(stateParameter)) {
                String key = OAUTH2_AUTHORIZATION_REQUEST_PREFIX + stateParameter;
                redisCache.deleteObject(key);
            }
            return;
        }

        String state = authorizationRequest.getState();
        String key = OAUTH2_AUTHORIZATION_REQUEST_PREFIX + state;
        redisCache.setCacheObject(key, authorizationRequest, AUTHORIZATION_REQUEST_EXPIRE_TIME, TimeUnit.MINUTES);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                 HttpServletResponse response) {
        String stateParameter = getStateParameter(request);
        if (StringUtils.hasText(stateParameter)) {
            String key = OAUTH2_AUTHORIZATION_REQUEST_PREFIX + stateParameter;
            OAuth2AuthorizationRequest authRequest = redisCache.getCacheObject(key);
            if (authRequest != null) {
                redisCache.deleteObject(key);
                return authRequest;
            }
        }
        return null;
    }

    private String getStateParameter(HttpServletRequest request) {
        return request.getParameter("state");
    }
}