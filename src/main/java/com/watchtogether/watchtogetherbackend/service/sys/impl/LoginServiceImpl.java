package com.watchtogether.watchtogetherbackend.service.sys.impl;

import com.watchtogether.watchtogetherbackend.entity.response.RestBean;
import com.watchtogether.watchtogetherbackend.entity.sys.SysUser;
import com.watchtogether.watchtogetherbackend.entity.userdetail.LoginUser;
import com.watchtogether.watchtogetherbackend.mapper.sys.UserMapper;
import com.watchtogether.watchtogetherbackend.service.sys.LoginService;
import com.watchtogether.watchtogetherbackend.utils.JWTUtil;
import com.watchtogether.watchtogetherbackend.utils.RedisCache;
import jakarta.annotation.Resource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class LoginServiceImpl implements LoginService {
    @Resource
    private AuthenticationManager authenticationManager;
    @Resource
    private UserMapper userMapper;
    @Resource
    private RedisCache redisCache;

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
        Authentication authenticate = authenticationManager.authenticate(authenticationToken);
        if (Objects.isNull(authenticate)) {
            throw new RuntimeException("登录失败");
        }
        Long userId = userMapper.selectUserIdByUserName(user.getUserName());
        String userStatus = userMapper.getUserStatus(userId);
        // 用户如果处于禁用状态则无法登录
        if (userStatus.equals("1")) {
            return RestBean.error(304, "该账户已被禁用，请联系管理员");
        } else {
            LoginUser loginUser = (LoginUser) authenticate.getPrincipal();
            String id = loginUser.getUser().getId().toString();
            String jwt = JWTUtil.createJWT(id);
            Map<String, Object> map = new HashMap<>();
            map.put("token", jwt);
            redisCache.setCacheObject("login:" + id, loginUser);
            return RestBean.success(map);
        }
    }
}
