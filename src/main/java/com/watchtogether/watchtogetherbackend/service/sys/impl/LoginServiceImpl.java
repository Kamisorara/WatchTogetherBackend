package com.watchtogether.watchtogetherbackend.service.sys.impl;

import com.watchtogether.watchtogetherbackend.entity.response.RestBean;
import com.watchtogether.watchtogetherbackend.entity.sys.SysUser;
import com.watchtogether.watchtogetherbackend.service.sys.LoginService;
import jakarta.annotation.Resource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class LoginServiceImpl implements LoginService {
    @Resource
    private AuthenticationManager authenticationManager;

    /**
     * 登录
     *
     * @param user
     * @return
     */
    @Override
    public RestBean login(SysUser user) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(user.getUserName(), user.getUserPassword());
        authenticationManager.authenticate(authenticationToken);
        return RestBean.success();
    }
}
