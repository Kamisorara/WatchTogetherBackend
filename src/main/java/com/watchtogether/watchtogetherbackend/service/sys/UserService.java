package com.watchtogether.watchtogetherbackend.service.sys;

import jakarta.servlet.http.HttpServletRequest;

/**
 * sys用户相关Service
 */
public interface UserService {
    // 查询用户
    String getUserName(Long userId);

    // 从请求头token中获取用户id
    Long getUserIdFromToken(HttpServletRequest request) throws Exception;

    // 判断邮箱是否唯一
    Boolean JudgeOnlyEmail(String email);

}
