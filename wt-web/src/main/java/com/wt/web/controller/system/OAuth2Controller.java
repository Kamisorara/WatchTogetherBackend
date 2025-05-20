package com.wt.web.controller.system;

import com.wt.common.annotaion.RateLimit;
import com.wt.entity.resp.RestBean;
import com.wt.service.oauth2.OAuth2Service;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

/**
 * OAuth2 授权登录Controller
 */
@RestController
@RequestMapping("/api/oauth2")
@Slf4j
public class OAuth2Controller {
    @Resource
    private OAuth2Service oAuth2Service;


    /**
     * 获取GitHub授权URL
     * 生成用于OAuth2授权的GitHub登录链接，包含必要的state参数以防止CSRF攻击
     *
     * @return 返回包含授权URL和state的Map数据，使用RestBean包装
     */
    @RateLimit(limit = 5, message = "访问过于频繁")
    @GetMapping("/github/authorize")
    public RestBean<Map<String, String>> getGithubAuthorizeUrl() {
        return oAuth2Service.getGithubAuthorizeUrl();
    }

    /**
     * 处理GitHub OAuth2授权回调
     * GitHub认证成功后的回调处理，验证state参数防止CSRF攻击，
     * 并通过授权码获取访问令牌和用户信息
     *
     * @param code     GitHub授权成功后返回的授权码，用于获取访问令牌
     * @param state    安全校验码，必须与发起授权请求时提供的state一致
     * @param response HTTP响应对象，用于重定向或设置认证cookies
     * @return 返回处理结果，成功时包含用户信息或令牌，失败时包含错误信息
     */
    @GetMapping("/github/callback")
    public RestBean<Object> handleGithubCallback(@RequestParam("code") String code,
                                                 @RequestParam("state") String state,
                                                 HttpServletResponse response) throws IOException {
        return oAuth2Service.handleGithubCallback(code, state, response);
    }

    /**
     * 获取Google授权URL
     *
     * @return
     */
    @GetMapping("/google/authorize")
    public RestBean<Map<String, String>> getGoogleAuthorizeUrl() {
        return oAuth2Service.getGoogleAuthorizeUrl();
    }

    /**
     * 处理Google OAuth2授权回调
     *
     * @param code
     * @param state
     * @param response
     * @return
     * @throws IOException
     */
    @GetMapping("/google/callback")
    public RestBean<Object> handleGoogleCallback(
            @RequestParam("code") String code,
            @RequestParam("state") String state,
            HttpServletResponse response) throws IOException {
        return oAuth2Service.handleGoogleCallback(code, state, response);
    }

    /**
     * 提交邮箱完成OAuth2注册
     * 对于首次通过OAuth2登录的用户，需要提供邮箱等额外信息完成注册流程
     *
     * @param data 包含用户注册信息的Map，必须包含email等关键字段
     * @return 返回注册结果，成功时包含用户登录令牌，失败时包含错误信息
     */
    @PostMapping("/complete-registration")
    public RestBean completeRegistration(@RequestBody Map<String, String> data) {
        return oAuth2Service.completeOAuth2Registration(data);
    }
}