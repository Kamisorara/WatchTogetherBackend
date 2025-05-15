package com.wt.service.system;


import com.wt.entity.resp.RestBean;
import com.wt.entity.system.SysUser;

/**
 * 用户认证服务Service
 * 提供用户登录、注册、注销和令牌刷新等核心认证功能
 * 管理用户身份验证状态和JWT令牌生命周期
 */
public interface LoginService {
    /**
     * 用户登录认证
     * 验证用户凭据，生成访问令牌和刷新令牌
     * 将用户会话信息存储到Redis并设置刷新令牌Cookie
     *
     * @param user 包含用户名和密码的用户对象
     * @return 认证结果响应，成功时包含访问令牌和有效期信息
     */
    RestBean login(SysUser user);

    /**
     * 用户注册
     * 创建新用户账号，验证密码一致性和邮箱唯一性
     * 为新用户分配普通用户角色
     *
     * @param username       用户名
     * @param password       密码
     * @param passwordRepeat 确认密码
     * @param email          电子邮箱
     * @return 注册结果响应，成功或包含失败原因
     */
    RestBean register(String username, String password, String passwordRepeat, String email);

    /**
     * 用户退出登录
     * 清除用户会话信息、Redis缓存和认证Cookie
     * 终止用户的当前认证状态
     *
     * @return 退出操作结果响应
     */
    RestBean logout();

    /**
     * 刷新Access Token
     * 验证并使用Refresh Token生成新的Access Token
     * 如果Refresh Token接近过期，同时更新Refresh Token
     *
     * @return 包含新Access Token的响应，或认证失败信息
     */
    RestBean refreshToken();
}