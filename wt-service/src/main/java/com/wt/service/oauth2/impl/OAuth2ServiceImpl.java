package com.wt.service.oauth2.impl;

import com.wt.common.utils.JWTUtil;
import com.wt.common.utils.RedisCache;
import com.wt.dao.mapper.OAuth2UserMappingMapper;
import com.wt.dao.mapper.UserMapper;
import com.wt.dao.mapper.UserRoleMapper;
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
        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId("github");

        // 生成状态值
        String state = UUID.randomUUID().toString();

        // 构建授权请求
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .clientId(clientRegistration.getClientId())
                .authorizationUri(clientRegistration.getProviderDetails().getAuthorizationUri())
                .redirectUri(clientRegistration.getRedirectUri())
                .scopes(clientRegistration.getScopes())
                .state(state)
                .build();

        // 保存状态到Redis（添加10分钟过期时间）
        redisCache.setCacheObject("oauth2_state:" + state, true, 10, TimeUnit.MINUTES);

        // 使用 RedisAuthorizationRequestRepository 保存授权请求
        redisAuthorizationRequestRepository.saveAuthorizationRequest(authorizationRequest, null, null);

        Map<String, String> result = new HashMap<>();
        result.put("url", authorizationRequest.getAuthorizationRequestUri());
        result.put("state", state);

        return RestBean.success(result);
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
        // 日志记录
        log.info("收到GitHub回调，code: {}, state: {}", code, state);

        // 从 Redis 验证 state
        Boolean validState = redisCache.getCacheObject("oauth2_state:" + state);
        if (validState == null || !validState) {
            return RestBean.error(400, "无效的请求状态");
        }

        // 清除已使用的 state
        redisCache.deleteObject("oauth2_state:" + state);

        try {
            // 使用授权码获取 access token
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");

            // 准备请求体
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("client_id", githubClientId);
            requestBody.put("client_secret", githubClientSecret);
            requestBody.put("code", code);

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

            // 交换授权码获取 access token
            ResponseEntity<Map> tokenResponse = restTemplate.postForEntity("https://github.com/login/oauth/access_token", requestEntity, Map.class);

            String accessToken = (String) tokenResponse.getBody().get("access_token");
            if (accessToken == null) {
                return RestBean.error(400, "获取访问令牌失败");
            }

            // 使用 access token 获取用户信息
            headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.set("Accept", "application/json");

            HttpEntity<String> userRequestEntity = new HttpEntity<>(null, headers);
            ResponseEntity<Map> userResponse = restTemplate.exchange("https://api.github.com/user", HttpMethod.GET, userRequestEntity, Map.class);

            // 解析用户信息
            Map<String, Object> githubUser = userResponse.getBody();
            String githubId = githubUser.get("id").toString();
            String login = (String) githubUser.get("login");

            // 检查用户是否已经绑定
            OAuth2UserMapping mapping = oAuth2UserMappingMapper.selectByProviderAndProviderId("github", githubId);

            if (mapping != null) {
                // 用户已绑定，执行登录
                Long userId = mapping.getUserId();
                SysUser user = userMapper.selectById(userId);

                if (user == null || "1".equals(user.getUserStatus())) {
                    return RestBean.error(403, "用户已被禁用");
                }

                // 生成 JWT
                String webAccessToken = JWTUtil.createAccessToken(userId.toString());

                // 创建Refresh Token
                String refreshToken = JWTUtil.createRefreshToken(userId.toString());
                log.info("refreshToken: {}", refreshToken);

                // 将Refresh Token存储到HttpOnly Cookie中
                Cookie refreshTokenCookie = new Cookie("refresh_token", refreshToken);
                refreshTokenCookie.setHttpOnly(true);
                refreshTokenCookie.setSecure(true);
                refreshTokenCookie.setAttribute("SameSite", "None");
                refreshTokenCookie.setPath("/");
                refreshTokenCookie.setMaxAge((int) TimeUnit.MILLISECONDS.toSeconds(JWTUtil.REFRESH_TOKEN_TTL)); // 7天
                response.addCookie(refreshTokenCookie);

                // 保存登录状态
                LoginUser loginUser = new LoginUser();
                loginUser.setUser(user);
                redisCache.setCacheObject("login:" + userId, loginUser);

                // 存储Refresh Token到Redis
                Map<String, Object> refreshTokenMap = new HashMap<>();
                refreshTokenMap.put("token", refreshToken);
                refreshTokenMap.put("userId", userId.toString());
                refreshTokenMap.put("createTime", System.currentTimeMillis());
                redisCache.setCacheObject("refresh_token:" + userId, refreshTokenMap, Math.toIntExact(JWTUtil.REFRESH_TOKEN_TTL), TimeUnit.MILLISECONDS);

                // 重定向到前端成功页面
                response.sendRedirect(frontendBaseUrl + "/oauth2/success?token=" + webAccessToken);
                return RestBean.success("登录成功");
            } else {
                // 用户未绑定，返回信息让用户完成注册
                Map<String, Object> userData = new HashMap<>();
                userData.put("oauthId", "github_" + githubId);
                userData.put("login", login);

                // 重定向到前端注册页面
                String redirectUrl = frontendBaseUrl + "/oauth2/register?" +
                        "oauthId=" + URLEncoder.encode("github_" + githubId, "UTF-8") +
                        "&login=" + URLEncoder.encode(login, "UTF-8");

                response.sendRedirect(redirectUrl);
                return RestBean.success(userData);
            }
        } catch (Exception e) {
            log.error("处理GitHub回调时发生错误", e);
            return RestBean.error(500, "第三方登录失败: " + e.getMessage());
        }
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
        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId("google");

        // 生成状态值
        String state = UUID.randomUUID().toString();

        // 构建授权请求
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .clientId(clientRegistration.getClientId())
                .authorizationUri(clientRegistration.getProviderDetails().getAuthorizationUri())
                .redirectUri(clientRegistration.getRedirectUri())
                .scopes(clientRegistration.getScopes())
                .state(state)
                .build();

        // 保存状态到Redis（添加10分钟过期时间）
        redisCache.setCacheObject("oauth2_state:" + state, true, 10, TimeUnit.MINUTES);

        // 使用 RedisAuthorizationRequestRepository 保存授权请求
        redisAuthorizationRequestRepository.saveAuthorizationRequest(authorizationRequest, null, null);

        Map<String, String> result = new HashMap<>();
        result.put("url", authorizationRequest.getAuthorizationRequestUri());
        result.put("state", state);

        return RestBean.success(result);
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
        // 日志记录
        log.info("收到Google回调，code: {}, state: {}", code, state);

        // 从 Redis 验证 state
        Boolean validState = redisCache.getCacheObject("oauth2_state:" + state);
        if (validState == null || !validState) {
            return RestBean.error(400, "无效的请求状态");
        }

        // 清除已使用的 state
        redisCache.deleteObject("oauth2_state:" + state);

        try {
            ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId("google");

            // 使用授权码获取 access token
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
            headers.set("Content-Type", "application/x-www-form-urlencoded");

            // 准备请求体
            MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
            requestBody.add("client_id", googleClientId);
            requestBody.add("client_secret", googleClientSecret);
            requestBody.add("code", code);
            requestBody.add("redirect_uri", googleRedirectUri);
            requestBody.add("grant_type", "authorization_code");

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

            // 交换授权码获取 access token
            ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(
                    clientRegistration.getProviderDetails().getTokenUri(),
                    requestEntity,
                    Map.class);

            String accessToken = (String) tokenResponse.getBody().get("access_token");
            if (accessToken == null) {
                return RestBean.error(400, "获取访问令牌失败");
            }

            // 使用 access token 获取用户信息
            headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.set("Accept", "application/json");

            HttpEntity<String> userRequestEntity = new HttpEntity<>(null, headers);
            ResponseEntity<Map> userResponse = restTemplate.exchange(
                    clientRegistration.getProviderDetails().getUserInfoEndpoint().getUri(),
                    HttpMethod.GET,
                    userRequestEntity,
                    Map.class);

            // 解析用户信息
            Map<String, Object> googleUser = userResponse.getBody();
            String googleId = (String) googleUser.get("sub");  // Google使用sub作为唯一标识符
            String email = (String) googleUser.get("email");
            String name = (String) googleUser.get("name");

            // 检查用户是否已经绑定
            OAuth2UserMapping mapping = oAuth2UserMappingMapper.selectByProviderAndProviderId("google", googleId);

            if (mapping != null) {
                // 用户已绑定，执行登录
                Long userId = mapping.getUserId();
                SysUser user = userMapper.selectById(userId);

                if (user == null || "1".equals(user.getUserStatus())) {
                    return RestBean.error(403, "用户已被禁用");
                }

                // 生成 JWT
                String webAccessToken = JWTUtil.createAccessToken(userId.toString());

                // 创建Refresh Token
                String refreshToken = JWTUtil.createRefreshToken(userId.toString());
                log.info("refreshToken: {}", refreshToken);

                // 将Refresh Token存储到HttpOnly Cookie中
                Cookie refreshTokenCookie = new Cookie("refresh_token", refreshToken);
                refreshTokenCookie.setHttpOnly(true);
                refreshTokenCookie.setSecure(true);
                refreshTokenCookie.setAttribute("SameSite", "None");
                refreshTokenCookie.setPath("/");
                refreshTokenCookie.setMaxAge((int) TimeUnit.MILLISECONDS.toSeconds(JWTUtil.REFRESH_TOKEN_TTL)); // 7天
                response.addCookie(refreshTokenCookie);

                // 保存登录状态
                LoginUser loginUser = new LoginUser();
                loginUser.setUser(user);
                redisCache.setCacheObject("login:" + userId, loginUser);

                // 存储Refresh Token到Redis
                Map<String, Object> refreshTokenMap = new HashMap<>();
                refreshTokenMap.put("token", refreshToken);
                refreshTokenMap.put("userId", userId.toString());
                refreshTokenMap.put("createTime", System.currentTimeMillis());
                redisCache.setCacheObject("refresh_token:" + userId, refreshTokenMap, Math.toIntExact(JWTUtil.REFRESH_TOKEN_TTL), TimeUnit.MILLISECONDS);

                // 重定向到前端成功页面
                response.sendRedirect(frontendBaseUrl + "/oauth2/success?token=" + webAccessToken);
                return RestBean.success("登录成功");
            } else {
                // 用户未绑定，返回信息让用户完成注册
                Map<String, Object> userData = new HashMap<>();
                userData.put("oauthId", "google_" + googleId);
                userData.put("login", email); // 使用邮箱作为默认用户名
                userData.put("name", name);
                userData.put("email", email); // Google会提供验证过的邮箱

                // 重定向到前端注册页面
                String redirectUrl = frontendBaseUrl + "/oauth2/register?" +
                        "oauthId=" + URLEncoder.encode("google_" + googleId, "UTF-8") +
                        "&login=" + URLEncoder.encode(email, "UTF-8") +
                        "&name=" + URLEncoder.encode(name, "UTF-8") +
                        "&email=" + URLEncoder.encode(email, "UTF-8");

                response.sendRedirect(redirectUrl);
                return RestBean.success(userData);
            }
        } catch (Exception e) {
            log.error("处理Google回调时发生错误", e);
            return RestBean.error(500, "第三方登录失败: " + e.getMessage());
        }
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
            redisCache.setCacheObject("refresh_token:" + userId, refreshTokenMap, Math.toIntExact(JWTUtil.REFRESH_TOKEN_TTL), TimeUnit.MILLISECONDS);

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
}
