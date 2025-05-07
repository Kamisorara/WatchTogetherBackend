package com.wt.web.controller.system;

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
import com.wt.web.core.config.security.RedisAuthorizationRequestRepository;
import jakarta.annotation.Resource;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/oauth2")
@Slf4j
public class OAuth2Controller {
    @Value("${spring.security.oauth2.client.registration.github.client-id}")
    private String githubClientId;

    @Value("${spring.security.oauth2.client.registration.github.redirect-uri}")
    private String githubRedirectUri;

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserRoleMapper userRoleMapper;

    @Resource
    private OAuth2UserMappingMapper oAuth2UserMappingMapper;

    @Resource
    private RedisCache redisCache;

    @Value("${spring.security.oauth2.client.registration.github.client-secret}")
    private String githubClientSecret;

    @Value("${cors.allowedOrigins}")
    private String frontendBaseUrl;

    @Resource
    private ClientRegistrationRepository clientRegistrationRepository;

    @Resource
    private RedisAuthorizationRequestRepository redisAuthorizationRequestRepository;


    /**
     * 获取GitHub授权URL
     */
    @GetMapping("/github/authorize")
    public RestBean<Map<String, String>> getGithubAuthorizeUrl() {
        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId("github");

        // 生成状态值
        String state = UUID.randomUUID().toString();

        // 构建授权请求
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode().clientId(clientRegistration.getClientId()).authorizationUri(clientRegistration.getProviderDetails().getAuthorizationUri()).redirectUri(clientRegistration.getRedirectUri()).scopes(clientRegistration.getScopes()).state(state).build();

        // 保存状态到Redis（添加10分钟过期时间）
        redisCache.setCacheObject("oauth2_state:" + state, true, 10, TimeUnit.MINUTES);

        // 使用 RedisAuthorizationRequestRepository 保存授权请求
        redisAuthorizationRequestRepository.saveAuthorizationRequest(authorizationRequest, null, null);

        Map<String, String> result = new HashMap<>();
        result.put("url", authorizationRequest.getAuthorizationRequestUri());
        result.put("state", state);

        return RestBean.success(result);
    }

    @GetMapping("/github/callback")
    public RestBean<Object> handleGithubCallback(@RequestParam("code") String code, @RequestParam("state") String state, HttpServletResponse response) throws IOException {

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
            RestTemplate restTemplate = new RestTemplate();
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

            // 移除获取邮箱信息的代码，不再请求用户邮箱

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
                String jwt = JWTUtil.createJWT(userId.toString());

                // 保存登录状态
                LoginUser loginUser = new LoginUser();
                loginUser.setUser(user);
                redisCache.setCacheObject("login:" + userId, loginUser);

                // 重定向到前端成功页面
                response.sendRedirect(frontendBaseUrl + "/oauth2/success?token=" + jwt);
                return RestBean.success("登录成功");
            } else {
                // 用户未绑定，返回信息让用户完成注册
                Map<String, Object> userData = new HashMap<>();
                userData.put("oauthId", "github_" + githubId);
                userData.put("login", login);

                // 重定向到前端注册页面，不再传递邮箱参数
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
     * 提交邮箱完成注册
     */
    @PostMapping("/complete-registration")
    public RestBean completeRegistration(@RequestBody Map<String, String> data) {
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

            // 生成JWT
            String jwt = JWTUtil.createJWT(userId.toString());

            LoginUser loginUser = new LoginUser();
            loginUser.setUser(user);
            redisCache.setCacheObject("login:" + userId, loginUser);

            Map<String, Object> result = new HashMap<>();
            result.put("token", jwt);

            return RestBean.success(result);
        } catch (Exception e) {
            return RestBean.error(500, "注册失败: " + e.getMessage());
        }
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[\\w.-]+@([\\w-]+\\.)+[\\w-]{2,4}$");
    }
}