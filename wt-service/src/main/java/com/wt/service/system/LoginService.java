package com.wt.service.system;


import com.wt.entity.resp.RestBean;
import com.wt.entity.system.SysUser;

public interface LoginService {
    // 登录
    RestBean login(SysUser user);

    // 注册
    RestBean register(String username, String password, String passwordRepeat, String email);

    // 退出
    RestBean logout();

    // 刷新Token
    RestBean refreshToken();
}