package com.watchtogether.watchtogetherbackend.service.sys;

import com.watchtogether.watchtogetherbackend.entity.response.RestBean;
import com.watchtogether.watchtogetherbackend.entity.sys.SysUser;

public interface LoginService {
    // 登录
    RestBean login(SysUser user);

    // 注册
    RestBean register(String username, String password, String passwordRepeat, String email);

    // 退出
    RestBean logout();
}
