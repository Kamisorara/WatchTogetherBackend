package com.wt.service.system.impl;

import com.wt.common.utils.RedisCache;
import com.wt.dao.mapper.UserMapper;
import com.wt.dao.mapper.UserRoleMapper;
import com.wt.entity.resp.RestBean;
import com.wt.entity.system.SysUser;
import com.wt.entity.system.SysUserRole;
import com.wt.service.system.LoginService;
import com.wt.service.system.UserService;
import com.wt.service.helper.LoginUser;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LoginServiceImpl implements LoginService {
    @Resource
    private AuthenticationManager authenticationManager;

    @Resource
    private UserService userService;
    @Resource
    private UserMapper userMapper;
    @Resource
    private UserRoleMapper userRoleMapper;
    @Resource
    private RedisCache redisCache;

    @Override
    public RestBean login(SysUser user) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(user.getUserName(), user.getUserPassword());
        authenticationManager.authenticate(authenticationToken);
        return RestBean.success();
    }

    @Override
    public RestBean register(String username, String password, String passwordRepeat, String email) {
        try {
            if (password.equals(passwordRepeat) && userService.JudgeOnlyEmail(email)) {
                SysUser user = new SysUser();
                user.setUserName(username);
                BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
                String encodedPassword = encoder.encode(password);
                user.setUserPassword(encodedPassword);
                user.setUserEmail(email);
                userMapper.insert(user);
                Long userId = userMapper.selectUserIdByUserName(username);
                SysUserRole userRole = new SysUserRole();
                userRole.setUserId(userId);
                userRole.setRoleId(1L);
                userRoleMapper.insert(userRole);
                log.info("id:{}用户注册成功", userId);
                return RestBean.success("注册成功");
            } else {
                return RestBean.error(304, "注册失败");
            }
        } catch (Exception e) {
            System.out.println(e);
            return RestBean.error(400, "发生未知错误，请重试或联系管理员");
        }
    }

    @Override
    public RestBean logout() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            LoginUser loginuser = (LoginUser) authentication.getPrincipal();
            Long userId = loginuser.getUser().getId();
            redisCache.deleteObject("login:" + userId);
            log.info("id:{}用户退出", userId);
            return RestBean.success("退出成功");
        } catch (Exception e) {
            return RestBean.error(400, "发生错误请联系管理员或重试");
        }
    }
}
