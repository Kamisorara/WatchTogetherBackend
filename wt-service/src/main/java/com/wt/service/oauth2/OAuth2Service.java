package com.wt.service.oauth2;

import com.wt.entity.resp.RestBean;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/**
 * OAuth2认证服务接口
 * 提供第三方平台OAuth2登录认证相关功能
 * 包括生成授权URL、处理回调和完成用户注册
 * 目前支持GitHub作为OAuth2认证Provider
 */
public interface OAuth2Service {
    /**
     * 获取GitHub OAuth2授权URL
     * 生成带有状态参数的GitHub OAuth2授权地址，用于前端重定向
     * 状态参数将在回调时验证以防止CSRF攻击
     *
     * @return 包含授权URL信息的RestBean响应，成功时返回包含authorizeUrl的Map
     */
    RestBean<Map<String, String>> getGithubAuthorizeUrl();

    /**
     * 处理GitHub OAuth2回调
     * 接收GitHub认证回调，验证授权码并获取用户信息
     * 如用户已绑定则自动登录，否则引导用户完成注册
     *
     * @param code     GitHub授权成功返回的授权码
     * @param state    安全校验码，必须与发起授权请求时提供的state一致
     * @param response HTTP响应对象，用于重定向和设置认证Cookie
     * @return 包含处理结果的RestBean响应
     * @throws IOException 处理过程中可能发生的IO异常
     */
    RestBean<Object> handleGithubCallback(String code, String state, HttpServletResponse response) throws IOException;

    /**
     * 完成OAuth2用户注册
     * 处理第三方OAuth2登录用户的信息补充和账号创建
     * 创建本地用户账号并与第三方账号建立绑定关系
     *
     * @param data 包含用户注册信息的Map，必须包含email、oauthId等字段
     *             通常需包含：{"email": "用户邮箱", "oauthId": "oauth提供商_ID", "username": "用户名"}
     * @return 包含注册结果的RestBean响应，成功时包含用户访问令牌
     */
    RestBean completeOAuth2Registration(Map<String, String> data);
}
