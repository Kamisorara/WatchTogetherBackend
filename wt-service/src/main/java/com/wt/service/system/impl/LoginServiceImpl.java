package com.wt.service.system.impl;

import com.wt.common.utils.JWTUtil;
import com.wt.common.utils.RedisCache;
import com.wt.dao.mapper.UserMapper;
import com.wt.dao.mapper.UserRoleMapper;
import com.wt.entity.resp.RestBean;
import com.wt.entity.system.SysUser;
import com.wt.entity.system.SysUserRole;
import com.wt.service.helper.LoginUser;
import com.wt.service.system.LoginService;
import com.wt.service.system.UserService;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
    @Resource
    private HttpServletResponse response;
    @Resource
    private HttpServletRequest request;

    @Override
    public RestBean login(SysUser user) {
        try {
            // 创建认证Token
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(user.getUserName(), user.getUserPassword());

            // 执行认证
            Authentication authentication = authenticationManager.authenticate(authenticationToken);

            // 获取认证成功的用户信息
            LoginUser loginUser = (LoginUser) authentication.getPrincipal();
            SysUser authenticatedUser = loginUser.getUser();

            // 检查用户状态
            if (authenticatedUser.getUserStatus().equals("1")) {
                return RestBean.error(304, "该账户已被禁用");
            }

            // 获取用户ID
            String userId = authenticatedUser.getId().toString();

            // 生成 ACCESS_TOKEN 时间15分钟
            String accessToken = JWTUtil.createAccessToken(userId);

            // 创建Refresh Token
            String refreshToken = JWTUtil.createRefreshToken(userId);
            log.info("refreshToken: {}", refreshToken);

            // 将Refresh Token存储到HttpOnly Cookie中
            Cookie refreshTokenCookie = new Cookie("refresh_token", refreshToken);
            refreshTokenCookie.setHttpOnly(true);
            refreshTokenCookie.setSecure(true);
            refreshTokenCookie.setAttribute("SameSite", "None");
            refreshTokenCookie.setPath("/");
            refreshTokenCookie.setMaxAge((int) TimeUnit.MILLISECONDS.toSeconds(JWTUtil.REFRESH_TOKEN_TTL)); // 7天
            response.addCookie(refreshTokenCookie);

            // 将用户信息存入Redis
            redisCache.setCacheObject("login:" + userId, loginUser);

            // 存储Refresh Token到Redis 包含时间，用于判断是否延长
            Map<String, Object> refreshTokenMap = new HashMap<>();
            refreshTokenMap.put("token", refreshToken);
            refreshTokenMap.put("userId", userId);
            refreshTokenMap.put("createTime", System.currentTimeMillis());
            redisCache.setCacheObject("refresh_token:" + userId, refreshTokenMap, Math.toIntExact(JWTUtil.REFRESH_TOKEN_TTL), TimeUnit.MILLISECONDS);

            // 返回JWT和必要信息
            Map<String, Object> map = new HashMap<>();
            map.put("token", accessToken);
            map.put("expiresIn", JWTUtil.ACCESS_TOKEN_TTL / 1000); // 过期时间（秒）

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
            // 从 secure contextg 中获取用户信息
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            LoginUser loginuser = (LoginUser) authentication.getPrincipal();
            Long userId = loginuser.getUser().getId();

            // 清除 Redis 中的用户信息和Refresh Token
            redisCache.deleteObject("login:" + userId);
            redisCache.deleteObject("refresh_token:" + userId);

            // 清除刷新TokenCookie
            Cookie cookie = new Cookie("refresh_token", null);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(0);
            response.addCookie(cookie);


            log.info("id:{}用户退出", userId);
            return RestBean.success("退出成功");
        } catch (Exception e) {
            return RestBean.error(400, "发生错误请联系管理员或重试");
        }
    }

    /**
     * 刷新访问Token
     */
    public RestBean refreshToken() {
        log.info("开始刷新Token");
        // 从Cookie获取刷新Token
        Cookie[] cookies = request.getCookies();
        String refreshToken = null;
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refresh_token".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }

        if (refreshToken == null) {
            return RestBean.error(401, "无效的刷新Token");
        }

        try {
            // 解析刷新Token
            Claims claims = JWTUtil.parseJWT(refreshToken);
            String userId = claims.getSubject();

            // 验证Redis中是否存在该刷新Token信息
            Map<String, Object> refreshTokenInfo = redisCache.getCacheObject("refresh_token:" + userId);
            if (refreshTokenInfo == null || !refreshToken.equals(refreshTokenInfo.get("token"))) {
                return RestBean.error(401, "刷新Token已失效");
            }

            // 检查用户信息
            LoginUser loginUser = redisCache.getCacheObject("login:" + userId);
            if (loginUser == null) {
                return RestBean.error(401, "用户会话已失效");
            }

            // 生成新的访问Token
            String newAccessToken = JWTUtil.createAccessToken(userId);
            log.info("新生成的ACCESS Token: {}", newAccessToken);

            // 检查是否需要更新刷新Token（如果接近过期时间，则更新）
            long currentTime = System.currentTimeMillis();
            long createTime = (long) refreshTokenInfo.get("createTime");

            // 刷新Token已经使用超过5天，更新它
            if (currentTime - createTime > 5 * 24 * 60 * 60 * 1000L) {
                String newRefreshToken = JWTUtil.createRefreshToken(userId);

                // 更新Cookie
                Cookie refreshTokenCookie = new Cookie("refresh_token", refreshToken);
                refreshTokenCookie.setHttpOnly(true);
                refreshTokenCookie.setSecure(true);
                refreshTokenCookie.setAttribute("SameSite", "None");
                refreshTokenCookie.setPath("/");
                refreshTokenCookie.setMaxAge((int) TimeUnit.MILLISECONDS.toSeconds(JWTUtil.REFRESH_TOKEN_TTL)); // 7天
                response.addCookie(refreshTokenCookie);

                // 更新Redis
                refreshTokenInfo.put("token", newRefreshToken);
                refreshTokenInfo.put("createTime", currentTime);
                redisCache.setCacheObject("refresh_token:" + userId, refreshTokenInfo, 7, TimeUnit.DAYS);
            }

            // 返回新的访问Token
            Map<String, Object> result = new HashMap<>();
            result.put("token", newAccessToken);
            result.put("expiresIn", JWTUtil.ACCESS_TOKEN_TTL / 1000);

            return RestBean.success(result);

        } catch (Exception e) {
            log.error("刷新Token失败：", e);
            return RestBean.error(401, "刷新Token无效");
        }
    }
}
