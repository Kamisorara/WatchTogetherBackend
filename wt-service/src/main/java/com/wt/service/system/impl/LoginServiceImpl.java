package com.wt.service.system.impl;

import com.wt.common.utils.JWTUtil;
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

import java.util.HashMap;
import java.util.Map;

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
        try {
            // 创建认证令牌
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(user.getUserName(), user.getUserPassword());

            // 执行认证
            Authentication authentication = authenticationManager.authenticate(authenticationToken);

            // 获取认证成功的用户信息
            LoginUser loginUser = (LoginUser) authentication.getPrincipal();
            SysUser authenticatedUser = loginUser.getUser();

            // 检查用户状态
            if (authenticatedUser.getUserStatus().equals("1")) {
                return RestBean.error(304, "该账户已被禁用");
            }

            // 生成JWT令牌
            String userId = authenticatedUser.getId().toString();
            String jwt = JWTUtil.createJWT(userId);

            // 将用户信息存入Redis
            redisCache.setCacheObject("login:" + userId, loginUser);

            // 返回JWT和必要信息
            Map<String, Object> map = new HashMap<>();
            map.put("token", jwt);

            log.info("id:{}用户登录成功", userId);
            return RestBean.success(map);

        } catch (Exception e) {
            log.info("登录失败: {}", e.getMessage());
            return RestBean.error(400, "账号或密码错误");
        }
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
