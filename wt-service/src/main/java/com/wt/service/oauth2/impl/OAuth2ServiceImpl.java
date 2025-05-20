package com.wt.service.oauth2.impl;

import com.wt.common.utils.JWTUtil;
import com.wt.common.utils.RedisCache;
import com.wt.dao.mapper.OAuth2UserMappingMapper;
import com.wt.dao.mapper.UserMapper;
import com.wt.dao.mapper.UserRoleMapper;
import com.wt.entity.Enum.OAuth2Provider;
import com.wt.entity.resp.RestBean;
import com.wt.entity.system.OAuth2UserMapping;
import com.wt.entity.system.SysUser;
import com.wt.entity.system.SysUserRole;
import com.wt.service.helper.LoginUser;
import com.wt.service.oauth2.OAuth2Service;
import com.wt.service.oauth2.RedisAuthorizationRequestRepository;
import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * OAuth2认证服务实现类
 * 提供GitHub等第三方平台OAuth2登录认证功能
 * 实现授权URL生成、回调处理、用户注册与绑定等功能
 * 使用Redis存储授权状态和会话信息，保障认证安全性
 */
@Service
@Slf4j
public class OAuth2ServiceImpl implements OAuth2Service {
    @Resource
    private OAuth2UserMappingMapper oAuth2UserMappingMapper;

    @Resource
    private ClientRegistrationRepository clientRegistrationRepository;

    @Resource
    private RedisAuthorizationRequestRepository redisAuthorizationRequestRepository;

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserRoleMapper userRoleMapper;

    @Resource
    private RedisCache redisCache;

    // Github OAuth2
    @Value("${spring.security.oauth2.client.registration.github.client-id}")
    private String githubClientId;

    @Value("${spring.security.oauth2.client.registration.github.client-secret}")
    private String githubClientSecret;

    @Value("${spring.security.oauth2.client.registration.github.redirect-uri}")
    private String githubRedirectUri;

    // Google OAuth2
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String googleRedirectUri;

    @Value("${cors.allowedOrigins}")
    private String frontendBaseUrl;

    @Resource
    private HttpServletResponse response;

    @Resource
    private RestTemplate restTemplate;

    /**
     * 获取GitHub OAuth2授权URL
     * 生成唯一的state参数并创建授权请求链接
     * 将授权状态保存到Redis中用于后续验证，有效期10分钟
     *
     * @return 包含授权URL和state参数的RestBean响应
     */
    @Override
    public RestBean<Map<String, String>> getGithubAuthorizeUrl() {
        return getAuthorizeUrl(OAuth2Provider.GITHUB);
    }

    /**
     * 处理GitHub OAuth2回调
     * 验证state参数有效性，使用授权码交换访问令牌并获取用户信息
     * 若用户已绑定则自动登录并生成JWT令牌，否则引导用户完成注册
     *
     * @param code     GitHub授权成功返回的授权码
     * @param state    安全校验码，必须与发起授权请求时提供的state一致
     * @param response HTTP响应对象，用于设置Cookie和执行重定向
     * @return 包含处理结果的RestBean响应
     * @throws IOException 执行重定向操作时可能抛出的异常
     */
    @Override
    public RestBean<Object> handleGithubCallback(String code, String state, HttpServletResponse response) throws IOException {
        return handleOAuth2Callback(code, state, OAuth2Provider.GITHUB, response);
    }

    /**
     * 获取Google OAuth2授权URL
     * 生成唯一的state参数并创建授权请求链接
     * 将授权状态保存到Redis中用于后续验证，有效期10分钟
     *
     * @return 包含授权URL和state参数的RestBean响应
     */
    @Override
    public RestBean<Map<String, String>> getGoogleAuthorizeUrl() {
        return getAuthorizeUrl(OAuth2Provider.GOOGLE);
    }

    /**
     * 处理Google OAuth2回调
     * 验证state参数有效性，使用授权码交换访问令牌并获取用户信息
     * 若用户已绑定则自动登录并生成JWT令牌，否则引导用户完成注册
     *
     * @param code     Google授权成功返回的授权码
     * @param state    安全校验码，必须与发起授权请求时提供的state一致
     * @param response HTTP响应对象，用于设置Cookie和执行重定向
     * @return 包含处理结果的RestBean响应
     * @throws IOException 执行重定向操作时可能抛出的异常
     */
    @Override
    public RestBean<Object> handleGoogleCallback(String code, String state, HttpServletResponse response) throws IOException {
        return handleOAuth2Callback(code, state, OAuth2Provider.GOOGLE, response);
    }

    /**
     * 完成OAuth2用户注册流程
     * 验证邮箱有效性，创建本地用户账号并与OAuth2提供商账号建立关联
     * 生成访问令牌和刷新令牌，设置安全Cookie并将用户状态保存到Redis
     *
     * @param data 包含用户注册信息的Map，必须包含email、oauthId字段
     * @return 包含注册结果的RestBean响应，成功时返回包含访问令牌的Map
     */
    @Override
    public RestBean completeOAuth2Registration(Map<String, String> data) {
        String email = data.get("email");
        String oauthId = data.get("oauthId");
        String username = data.get("username");

        try {
            // 验证邮箱
            if (!isValidEmail(email) || userMapper.countUserEmail(email) > 0) {
                return RestBean.error(400, "邮箱无效或已被使用");
            }

            // 解析oauthId
            String[] parts = oauthId.split("_", 2);
            if (parts.length != 2) {
                return RestBean.error(400, "参数无效");
            }

            String provider = parts[0];
            String providerId = parts[1];

            // 创建新用户
            SysUser user = new SysUser();
            // 使用GitHub用户名或生成随机用户名
            user.setUserName(username != null ? username : provider + "_user_" + UUID.randomUUID().toString().substring(0, 8));
            user.setUserEmail(email);

            // 生成随机密码（用户可稍后设置）
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            String randomPassword = UUID.randomUUID().toString();
            user.setUserPassword(encoder.encode(randomPassword));

            userMapper.insert(user);
            Long userId = userMapper.selectUserIdByUserName(user.getUserName());

            // 分配普通用户角色
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(1L);  // 普通用户角色ID
            userRoleMapper.insert(userRole);

            // 创建OAuth2映射
            OAuth2UserMapping mapping = new OAuth2UserMapping();
            mapping.setUserId(userId);
            mapping.setProvider(provider);
            mapping.setProviderUserId(providerId);
            oAuth2UserMappingMapper.insert(mapping);

            // 生成访问令牌和刷新令牌
            String accessToken = JWTUtil.createAccessToken(userId.toString());
            String refreshToken = JWTUtil.createRefreshToken(userId.toString());

            // 将Refresh Token存储到HttpOnly Cookie中
            Cookie refreshTokenCookie = new Cookie("refresh_token", refreshToken);
            refreshTokenCookie.setHttpOnly(true);
            refreshTokenCookie.setSecure(true);
            refreshTokenCookie.setAttribute("SameSite", "None");
            refreshTokenCookie.setPath("/");
            refreshTokenCookie.setMaxAge((int) TimeUnit.MILLISECONDS.toSeconds(JWTUtil.REFRESH_TOKEN_TTL)); // 7天
            response.addCookie(refreshTokenCookie);

            // 保存用户登录信息到Redis
            LoginUser loginUser = new LoginUser();
            loginUser.setUser(user);
            redisCache.setCacheObject("login:" + userId, loginUser);

            // 存储Refresh Token到Redis
            Map<String, Object> refreshTokenMap = new HashMap<>();
            refreshTokenMap.put("token", refreshToken);
            refreshTokenMap.put("userId", userId.toString());
            refreshTokenMap.put("createTime", System.currentTimeMillis());
            redisCache.setCacheObject("refresh_token:" + userId, refreshTokenMap,
                    Math.toIntExact(JWTUtil.REFRESH_TOKEN_TTL), TimeUnit.MILLISECONDS);

            Map<String, Object> result = new HashMap<>();
            result.put("token", accessToken);
            result.put("expiresIn", JWTUtil.ACCESS_TOKEN_TTL / 1000);

            return RestBean.success(result);
        } catch (Exception e) {
            log.error("OAuth2注册失败", e);
            return RestBean.error(500, "注册失败: " + e.getMessage());
        }
    }

    /**
     * 验证邮箱格式
     * 检查邮箱地址是否符合基本格式规范
     *
     * @param email 待验证的邮箱地址
     * @return 邮箱格式有效返回true，否则返回false
     */
    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[\\w.-]+@([\\w-]+\\.)+[\\w-]{2,4}$");
    }

    // ------------------- 重构 私有方法 ------------------ //

    /**
     * 获取OAuth2授权URL
     *
     * @param provider OAuth2 Provider枚举
     * @return 包含授权URL和state参数的RestBean响应
     */
    private RestBean<Map<String, String>> getAuthorizeUrl(OAuth2Provider provider) {
        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(provider.getRegistrationId());

        String state = UUID.randomUUID().toString();

        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest
                .authorizationCode().
                clientId(clientRegistration.getClientId())
                .authorizationUri
                        (clientRegistration
                                .getProviderDetails()
                                .getAuthorizationUri()
                        )
                .redirectUri(clientRegistration.getRedirectUri())
                .scopes(clientRegistration.getScopes())
                .state(state)
                .build();

        redisCache.setCacheObject("oauth2_state:" + state, true, 10, TimeUnit.MINUTES);
        redisAuthorizationRequestRepository.saveAuthorizationRequest(authorizationRequest, null, null);

        Map<String, String> result = new HashMap<>();
        result.put("url", authorizationRequest.getAuthorizationRequestUri());
        result.put("state", state);

        return RestBean.success(result);
    }


    /**
     * 处理OAuth2回调
     * 验证state参数防止CSRF攻击，使用授权码获取访问令牌及用户信息
     * 若用户已绑定本地账户则自动登录，否则引导至注册完善页面
     *
     * @param code     授权码，OAuth2服务提供商在用户授权成功后返回的临时凭证
     * @param state    安全校验码，用于防止CSRF攻击，必须匹配发起授权请求时提供的state
     * @param provider OAuth2提供商枚举，标识授权请求来源(GitHub/Google等)
     * @param response HTTP响应对象，用于设置Cookie和执行重定向操作
     * @return 包含处理结果的RestBean对象，登录成功或重定向信息
     * @throws IOException 执行重定向操作可能抛出的异常
     */
    private RestBean<Object> handleOAuth2Callback(String code, String state, OAuth2Provider provider, HttpServletResponse response) throws IOException {
        log.info("收到{}回调，code: {}, state: {}", provider.name(), code, state);

        Boolean validState = redisCache.getCacheObject("oauth2_state:" + state);
        if (validState == null || !validState) {
            return RestBean.error(400, "无效的请求状态");
        }

        redisCache.deleteObject("oauth2_state:" + state);

        try {
            ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(provider.getRegistrationId());

            // 获取访问令牌
            String accessToken = getAccessToken(code, clientRegistration, provider);
            if (accessToken == null) {
                return RestBean.error(400, "获取访问令牌失败");
            }

            // 获取用户信息
            Map<String, Object> userInfo = getUserInfo(accessToken, clientRegistration);

            // 处理用户信息并登录或注册
            return processUserInfo(userInfo, provider, response);
        } catch (Exception e) {
            log.error("处理{}回调时发生错误", provider.name(), e);
            return RestBean.error(500, "第三方登录失败: " + e.getMessage());
        }
    }

    /**
     * 获取OAuth2 Access Token
     * 通过授权码向OAuth2服务提供商请求访问令牌
     * 构建必要的HTTP请求头和请求体，使用RestTemplate发送POST请求
     *
     * @param code               授权码，OAuth2服务提供商在用户授权成功后返回的临时凭证
     * @param clientRegistration 客户端注册信息，包含OAuth2提供商的端点URL等配置
     * @param provider           OAuth2提供商枚举，用于区分不同的第三方登录提供商(GitHub/Google等)
     * @return 成功时返回访问令牌字符串，失败时返回null
     */
    private String getAccessToken(String code, ClientRegistration clientRegistration, OAuth2Provider provider) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("Content-Type", "application/x-www-form-urlencoded");

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("client_id", provider == OAuth2Provider.GITHUB ? githubClientId : googleClientId);
        requestBody.add("client_secret", provider == OAuth2Provider.GITHUB ? githubClientSecret : googleClientSecret);
        requestBody.add("code", code);
        requestBody.add("redirect_uri", provider == OAuth2Provider.GITHUB ? githubRedirectUri : googleRedirectUri);
        requestBody.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(clientRegistration.getProviderDetails().getTokenUri(), requestEntity, Map.class);

        return (String) tokenResponse.getBody().get("access_token");
    }


    /**
     * 获取OAuth2授权用户信息
     * 使用访问令牌向OAuth2服务提供商请求用户资料
     * 构建带授权的HTTP请求头并发送GET请求获取用户数据
     *
     * @param accessToken        访问令牌，用于授权获取用户信息
     * @param clientRegistration 客户端注册信息，包含用户信息端点等配置
     * @return 包含用户基本信息的Map对象，如用户ID、名称、邮箱等
     */
    private Map<String, Object> getUserInfo(String accessToken, ClientRegistration clientRegistration) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("Accept", "application/json");

        HttpEntity<String> userRequestEntity = new HttpEntity<>(null, headers);
        ResponseEntity<Map> userResponse = restTemplate.exchange(clientRegistration.getProviderDetails().getUserInfoEndpoint().getUri(), HttpMethod.GET, userRequestEntity, Map.class);

        return userResponse.getBody();
    }

    /**
     * 处理OAuth2用户信息
     * 解析OAuth2提供商返回的用户数据，并执行后续登录或注册流程
     * 如果用户已绑定本地账号则自动登录，否则跳转到注册页面
     *
     * @param userInfo 从OAuth2提供商获取的用户信息Map
     * @param provider OAuth2提供商枚举，用于区分不同的第三方登录提供商
     * @param response HTTP响应对象，用于执行重定向操作
     * @return 包含处理结果的RestBean对象
     * @throws IOException 重定向操作可能抛出的异常
     */
    private RestBean<Object> processUserInfo(Map<String, Object> userInfo,
                                             OAuth2Provider provider,
                                             HttpServletResponse response) throws IOException {
        String providerId;
        String login;
        String name = null;
        String email = null;

        if (provider == OAuth2Provider.GITHUB) {
            providerId = userInfo.get("id").toString();
            login = (String) userInfo.get("login");
        } else { // GOOGLE
            providerId = (String) userInfo.get("sub");
            email = (String) userInfo.get("email");
            name = (String) userInfo.get("name");
            login = email;
        }

        // 检查用户是否已经绑定
        OAuth2UserMapping mapping = oAuth2UserMappingMapper.selectByProviderAndProviderId(provider.getRegistrationId(), providerId);

        if (mapping != null) {
            return loginUser(mapping.getUserId(), response);
        } else {
            return redirectToRegistration(provider.getRegistrationId(), providerId, login, name, email, response);
        }
    }

    /**
     * 执行用户登录流程
     * 验证用户状态，生成JWT访问令牌和刷新令牌
     * 设置安全Cookie、保存用户登录状态到Redis并重定向到前端成功页面
     *
     * @param userId   用户ID，用于查询用户信息和生成令牌
     * @param response HTTP响应对象，用于设置Cookie和执行重定向
     * @return 包含登录结果的RestBean对象
     * @throws IOException 重定向操作可能抛出的异常
     */
    private RestBean<Object> loginUser(Long userId, HttpServletResponse response) throws IOException {
        SysUser user = userMapper.selectById(userId);

        if (user == null || "1".equals(user.getUserStatus())) {
            return RestBean.error(403, "用户已被禁用");
        }

        String webAccessToken = JWTUtil.createAccessToken(userId.toString());
        String refreshToken = JWTUtil.createRefreshToken(userId.toString());

        // 设置刷新令牌到HttpOnly Cookie
        setRefreshTokenCookie(refreshToken, response);
        // 保存用户登录状态到Redis
        saveUserLoginState(userId, user, refreshToken);

        response.sendRedirect(frontendBaseUrl + "/oauth2/success?token=" + webAccessToken);
        return RestBean.success("登录成功");
    }


    /**
     * 重定向到快捷登录用户注册信息完善页面
     * 构建包含OAuth2用户信息的URL，并将用户重定向到前端注册页面
     * 将OAuth2提供商ID、用户登录名等信息作为URL参数传递
     *
     * @param provider   OAuth2提供商标识
     * @param providerId 第三方平台的用户唯一标识
     * @param login      用户登录名或用户名
     * @param name       用户显示名称，可能为空
     * @param email      用户邮箱，可能为空
     * @param response   HTTP响应对象，用于执行重定向
     * @return 包含操作结果的RestBean对象
     * @throws IOException 重定向操作可能抛出的异常
     */
    private RestBean<Object> redirectToRegistration(String provider,
                                                    String providerId,
                                                    String login,
                                                    String name,
                                                    String email,
                                                    HttpServletResponse response) throws IOException {
        // 构建重定向URL
        StringBuilder redirectUrl = new StringBuilder(frontendBaseUrl + "/oauth2/register?");
        redirectUrl.append("oauthId=").append(URLEncoder.encode(provider + "_" + providerId, "UTF-8"));
        redirectUrl.append("&login=").append(URLEncoder.encode(login, "UTF-8"));

        if (name != null) {
            redirectUrl.append("&name=").append(URLEncoder.encode(name, "UTF-8"));
        }

        if (email != null) {
            redirectUrl.append("&email=").append(URLEncoder.encode(email, "UTF-8"));
        }

        response.sendRedirect(redirectUrl.toString());
        return RestBean.success();
    }

    /**
     * 设置刷新令牌到 Cookie
     * 创建包含刷新令牌的HttpOnly、Secure安全Cookie
     * 设置适当的SameSite策略、路径和过期时间
     *
     * @param refreshToken 刷新令牌字符串
     * @param response     HTTP响应对象，用于添加Cookie
     */
    private void setRefreshTokenCookie(String refreshToken, HttpServletResponse response) {
        Cookie refreshTokenCookie = new Cookie("refresh_token", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setAttribute("SameSite", "None");
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge((int) TimeUnit.MILLISECONDS.toSeconds(JWTUtil.REFRESH_TOKEN_TTL)); // 7天
        response.addCookie(refreshTokenCookie);
    }


    /**
     * 保存用户登录状态到 Redis
     * 将用户信息缓存到Redis并设置过期时间
     * 同时保存刷新令牌及相关信息到Redis，用于后续令牌刷新验证
     *
     * @param userId       用户ID，作为Redis缓存的键前缀
     * @param user         用户对象，包含用户基本信息
     * @param refreshToken 刷新令牌，用于后续更新访问令牌
     */
    private void saveUserLoginState(Long userId, SysUser user, String refreshToken) {
        // 保存登录状态
        LoginUser loginUser = new LoginUser();
        loginUser.setUser(user);
        redisCache.setCacheObject("login:" + userId, loginUser);

        // 存储 Refresh Token 到 Redis
        Map<String, Object> refreshTokenMap = new HashMap<>();
        refreshTokenMap.put("token", refreshToken);
        refreshTokenMap.put("userId", userId.toString());
        refreshTokenMap.put("createTime", System.currentTimeMillis());
        redisCache.setCacheObject("refresh_token:" + userId, refreshTokenMap, Math.toIntExact(JWTUtil.REFRESH_TOKEN_TTL), TimeUnit.MILLISECONDS);
    }

}
