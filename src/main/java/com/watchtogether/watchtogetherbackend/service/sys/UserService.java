package com.watchtogether.watchtogetherbackend.service.sys;

import com.watchtogether.watchtogetherbackend.entity.response.UserInfoResp;
import jakarta.servlet.http.HttpServletRequest;

/**
 * sys用户相关Service
 */
public interface UserService {
    // 查询用户
    String getUserName(Long userId);

    // 从请求头的token中获取用户id
    Long getUserIdFromServerletRequest(HttpServletRequest request) throws Exception;

    // 从token中获取用户id
    Long getUserIdFromToken(String token) throws Exception;

    // 判断邮箱是否唯一
    Boolean JudgeOnlyEmail(String email);

    // 根据用户id更新用户头像
    void updateUserAvatarByToken(HttpServletRequest request, String avatarUrl) throws Exception;

    // 根据用户Token，获取用户信息
    UserInfoResp getUserInfoByToken(HttpServletRequest request) throws Exception;

    // 更新用户手机号和性别
    Boolean updateUserPhoneAndSexInfo(HttpServletRequest request, String userPhone, String userSex) throws Exception;
}
