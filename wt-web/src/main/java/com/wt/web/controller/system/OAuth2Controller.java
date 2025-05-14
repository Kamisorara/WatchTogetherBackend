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
     */
    @RateLimit(limit = 5, message = "访问过于频繁")
    @GetMapping("/github/authorize")
    public RestBean<Map<String, String>> getGithubAuthorizeUrl() {
        return oAuth2Service.getGithubAuthorizeUrl();
    }

    /**
     * 处理GitHub回调
     */
    @GetMapping("/github/callback")
    public RestBean<Object> handleGithubCallback(@RequestParam("code") String code,
                                                 @RequestParam("state") String state,
                                                 HttpServletResponse response) throws IOException {
        return oAuth2Service.handleGithubCallback(code, state, response);
    }

    /**
     * 提交邮箱完成注册
     */
    @PostMapping("/complete-registration")
    public RestBean completeRegistration(@RequestBody Map<String, String> data) {
        return oAuth2Service.completeOAuth2Registration(data);
    }
}