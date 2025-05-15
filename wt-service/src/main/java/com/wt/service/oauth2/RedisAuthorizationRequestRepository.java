package com.wt.service.oauth2;

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
 * 将 OAuth2 授权请求存储在 Redis 中，而不是依赖于默认的 HttpSession 存储方式
 */
@Component
public class RedisAuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final String OAUTH2_AUTHORIZATION_REQUEST_PREFIX = "oauth2_auth_request:";
    private static final int AUTHORIZATION_REQUEST_EXPIRE_TIME = 10; // 10分钟过期时间

    @Resource
    private RedisCache redisCache;

    /**
     * 从Redis加载OAuth2授权请求
     * 根据请求中的state参数查找并返回存储的授权请求对象
     *
     * @param request HTTP请求，包含OAuth2授权请求的state参数
     * @return 返回与请求state参数关联的OAuth2授权请求对象，如果不存在则返回null
     */
    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        String stateParameter = getStateParameter(request);
        if (StringUtils.hasText(stateParameter)) {
            String key = OAUTH2_AUTHORIZATION_REQUEST_PREFIX + stateParameter;
            return redisCache.getCacheObject(key);
        }
        return null;
    }

    /**
     * 将OAuth2授权请求保存到Redis
     * 使用授权请求中的state参数作为键，设置10分钟的过期时间
     * 如果授权请求为null，则尝试删除现有的授权请求
     *
     * @param authorizationRequest 要保存的OAuth2授权请求对象，可能为null
     * @param request              HTTP请求对象
     * @param response             HTTP响应对象
     */
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

    /**
     * 从Redis中移除并返回OAuth2授权请求
     * 根据请求中的state参数查找、删除并返回存储的授权请求
     *
     * @param request  HTTP请求对象，包含state参数
     * @param response HTTP响应对象
     * @return 返回被移除的OAuth2授权请求对象，如果不存在则返回null
     */
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

    /**
     * 从HTTP请求中提取OAuth2 state参数
     * state参数用于关联授权请求和回调，防止CSRF攻击
     *
     * @param request HTTP请求对象
     * @return 返回请求中的state参数值，如果不存在则返回null
     */
    private String getStateParameter(HttpServletRequest request) {
        return request.getParameter("state");
    }
}