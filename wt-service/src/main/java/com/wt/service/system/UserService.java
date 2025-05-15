package com.wt.service.system;

import com.wt.entity.resp.UserInfoResp;
import com.wt.entity.system.SysUser;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 用户Service
 * 提供用户信息查询、令牌解析、用户资料管理等功能
 * 支持基于Token的用户身份识别和用户数据操作
 */
public interface UserService {

    /**
     * 根据用户ID查询用户名
     * 通过数据库查询获取指定用户的用户名
     *
     * @param userId 用户ID
     * @return 用户名字符串
     */
    String getUserName(Long userId);

    /**
     * 从HTTP请求头中提取用户ID
     * 解析请求头中的Authorization中的Access Token并提取用户标识
     *
     * @param request HTTP请求对象，包含Authorization头
     * @return 用户ID
     * @throws Exception 令牌解析失败时抛出异常
     */
    Long getUserIdFromServerletRequest(HttpServletRequest request) throws Exception;

    /**
     * 从Access Token字符串中提取用户ID
     * 直接解析Access Token并获取用户标识
     *
     * @param token JWT认证令牌
     * @return 用户ID
     * @throws Exception 令牌解析失败时抛出异常
     */
    Long getUserIdFromToken(String token) throws Exception;

    /**
     * 判断邮箱是否唯一
     * 检查系统中是否已存在该邮箱地址
     *
     * @param email 待检查的邮箱地址
     * @return 邮箱未被使用返回true，否则返回false
     */
    Boolean JudgeOnlyEmail(String email);

    /**
     * 更新用户头像
     * 根据请求中的用户Access Token识别用户并更新其头像URL
     *
     * @param request   HTTP请求对象，用于获取用户身份
     * @param avatarUrl 新的头像URL地址
     * @throws Exception 令牌解析失败时抛出异常
     */
    void updateUserAvatarByToken(HttpServletRequest request, String avatarUrl) throws Exception;

    /**
     * 获取用户详细信息
     * 根据请求中的用户Access Token获取完整的用户信息
     *
     * @param request HTTP请求对象，用于获取用户身份
     * @return 包含用户详细信息的UserInfoResp对象
     * @throws Exception 令牌解析失败时抛出异常
     */
    UserInfoResp getUserInfoByToken(HttpServletRequest request) throws Exception;

    /**
     * 更新用户资料信息
     * 根据请求中的用户Access Token识别用户并更新其个人资料
     *
     * @param request  HTTP请求对象，用于获取用户身份
     * @param userInfo 包含更新内容的用户对象
     * @return 更新成功返回true，失败返回false
     * @throws Exception 令牌解析失败时抛出异常
     */
    Boolean updateUserInfo(HttpServletRequest request, SysUser userInfo) throws Exception;

    /**
     * 更新用户密码
     * 将新密码进行加密处理后更新到数据库
     *
     * @param request     HTTP请求对象，用于获取用户身份
     * @param newPassword 新的密码（明文，会被加密存储）
     * @return 更新成功返回true，失败返回false
     * @throws Exception 令牌解析失败时抛出异常
     */
    Boolean updateUserPassword(HttpServletRequest request, String newPassword) throws Exception;
}
