package com.wt.service.oauth2;

import com.wt.entity.resp.RestBean;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

public interface OAuth2Service {
    // GitHub OAuth2 登录地址返回
    RestBean<Map<String, String>> getGithubAuthorizeUrl();

    // GitHub OAuth2 回调处理
    RestBean<Object> handleGithubCallback(String code, String state, HttpServletResponse response) throws IOException;

    // OAuth2 用户账号注册
    RestBean completeOAuth2Registration(Map<String, String> data);
}
