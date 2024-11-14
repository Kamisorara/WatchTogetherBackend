package com.watchtogether.watchtogetherbackend.handler;

import com.alibaba.fastjson2.JSONObject;
import com.watchtogether.watchtogetherbackend.entity.response.RestBean;
import com.watchtogether.watchtogetherbackend.entity.sys.SysUser;
import com.watchtogether.watchtogetherbackend.entity.userdetail.LoginUser;
import com.watchtogether.watchtogetherbackend.utils.JWTUtil;
import com.watchtogether.watchtogetherbackend.utils.RedisCache;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class LoginSuccessHandler implements AuthenticationSuccessHandler {
    @Resource
    private RedisCache redisCache;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
//        System.out.println(authentication.toString());
        response.setCharacterEncoding("UTF-8");
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        SysUser user = loginUser.getUser();
        if (user.getUserStatus().equals("1")) {
            response.getWriter().write(JSONObject.toJSONString(RestBean.error(304, "该账户已被禁用。")));
        } else {
            String id = loginUser.getUser().getId().toString();
            String jwt = JWTUtil.createJWT(id);
            Map<String, Object> map = new HashMap<>();
            map.put("token", jwt);
            redisCache.setCacheObject("login:" + id, loginUser);
            log.info("id:{}用户登录成功", id);
            response.getWriter().write(JSONObject.toJSONString(RestBean.success(map)));
        }
    }
}
