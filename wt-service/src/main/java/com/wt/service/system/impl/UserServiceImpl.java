package com.wt.service.system.impl;

import com.wt.common.utils.JWTUtil;
import com.wt.dao.mapper.UserMapper;
import com.wt.entity.system.SysUser;
import com.wt.service.system.UserService;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户Service实现类
 */
@Service
public class UserServiceImpl implements UserService {
    @Resource
    private UserMapper userMapper;

    @Resource
    private PasswordEncoder passwordEncoder;

    /**
     * 根据用户ID查询用户名
     * 通过数据库查询获取指定用户的用户名
     *
     * @param userId 用户ID
     * @return 用户名字符串
     */
    @Override
    public String getUserName(Long userId) {
        return userMapper.FindUserById(userId);
    }

    /**
     * 根据用户Id查询用户信息
     * 通过sys_user id查询获取指定用户的用户信息
     *
     * @param userId 用户id
     * @return 用户信息
     */
    @Override
    public SysUser getUserInfo(Long userId) {
        return userMapper.getUserInfoById(userId);
    }

    /**
     * 从HTTP请求头中提取用户ID
     * 解析请求头中的Authorization中的Access Token并提取用户标识
     *
     * @param request HTTP请求对象，包含Authorization头
     * @return 用户ID
     * @throws Exception 令牌解析失败时抛出异常
     */
    @Override
    public Long getUserIdFromServerletRequest(HttpServletRequest request) throws Exception {
        // 获取token
        String token = request.getHeader("Authorization");
        Claims claims = JWTUtil.parseJWT(token);
        String userId = claims.get("sub").toString();
        return Long.parseLong(userId);
    }

    /**
     * 从Access Token字符串中提取用户ID
     * 直接解析Access Token并获取用户标识
     *
     * @param token JWT认证令牌
     * @return 用户ID
     * @throws Exception 令牌解析失败时抛出异常
     */
    @Override
    public Long getUserIdFromToken(String token) throws Exception {
        Claims claims = JWTUtil.parseJWT(token);
        String userId = claims.get("sub").toString();
        return Long.parseLong(userId);
    }

    /**
     * 判断邮箱是否唯一
     * 检查系统中是否已存在该邮箱地址
     *
     * @param email 待检查的邮箱地址
     * @return 邮箱未被使用返回true，否则返回false
     */
    @Override
    public Boolean JudgeOnlyEmail(String email) {
        Integer emailNum = userMapper.countUserEmail(email);
        return emailNum == 0;
    }

    /**
     * 更新用户头像
     * 根据请求中的用户Access Token识别用户并更新其头像URL
     *
     * @param request   HTTP请求对象，用于获取用户身份
     * @param avatarUrl 新的头像URL地址
     * @throws Exception 令牌解析失败时抛出异常
     */
    @Override
    public void updateUserAvatarByToken(HttpServletRequest request, String avatarUrl) throws Exception {
        Long userId = getUserIdFromServerletRequest(request);
        userMapper.updateUserAvatar(userId, avatarUrl);

    }

    /**
     * 获取用户详细信息
     * 根据请求中的用户Access Token获取完整的用户信息
     *
     * @param request HTTP请求对象，用于获取用户身份
     * @return 包含用户详细信息的UserInfoResp对象
     * @throws Exception 令牌解析失败时抛出异常
     */
    @Override
    public SysUser getUserInfoByToken(HttpServletRequest request) throws Exception {
        Long userId = getUserIdFromServerletRequest(request);
        return userMapper.getUserInfoById(userId);
    }

    /**
     * 更新用户资料信息
     * 根据请求中的用户Access Token识别用户并更新其个人资料
     *
     * @param request  HTTP请求对象，用于获取用户身份
     * @param userInfo 包含更新内容的用户对象
     * @return 更新成功返回true，失败返回false
     * @throws Exception 令牌解析失败时抛出异常
     */
    @Override
    public Boolean updateUserInfo(HttpServletRequest request, SysUser userInfo) throws Exception {
        Long userId = getUserIdFromServerletRequest(request);
        userInfo.setId(userId);  // 设置用户ID
        return userMapper.updateUserDetailInfo(userInfo) > 0;
    }

    /**
     * 更新用户密码
     * 将新密码进行加密处理后更新到数据库
     *
     * @param request     HTTP请求对象，用于获取用户身份
     * @param newPassword 新的密码（明文，会被加密存储）
     * @return 更新成功返回true，失败返回false
     * @throws Exception 令牌解析失败时抛出异常
     */
    @Override
    public Boolean updateUserPassword(HttpServletRequest request, String newPassword) throws Exception {
        // 从request中获取用户ID
        Long userId = getUserIdFromServerletRequest(request);
        // 对新密码进行加密
        String encodedPassword = passwordEncoder.encode(newPassword);

        // 创建用户对象并设置加密后的密码
        SysUser user = new SysUser();
        user.setId(userId);
        user.setUserPassword(encodedPassword);

        // 调用Mapper更新密码
        return userMapper.updateUserDetailInfo(user) > 0;
    }

}
