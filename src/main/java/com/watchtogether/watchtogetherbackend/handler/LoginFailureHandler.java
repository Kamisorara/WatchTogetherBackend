package com.watchtogether.watchtogetherbackend.handler;

import com.alibaba.fastjson2.JSONObject;
import com.watchtogether.watchtogetherbackend.entity.response.RestBean;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class LoginFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        String ipAddress = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        // 如果通过代理转发，可以尝试获取 X-Forwarded-For 头中的原始 IP 地址
        String forwardedIp = request.getHeader("X-Forwarded-For");
        if (forwardedIp != null && !forwardedIp.isEmpty()) {
            ipAddress = forwardedIp.split(",")[0].trim(); // 取第一个 IP，防止有多个代理
        }
        log.info("ip:{}登录失败, User-Agent:{}", ipAddress, userAgent);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(JSONObject.toJSONString(RestBean.error(400, "账号或密码错误")));
    }
}
