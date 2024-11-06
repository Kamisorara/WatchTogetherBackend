package com.watchtogether.watchtogetherbackend.service.sys;

import com.watchtogether.watchtogetherbackend.entity.response.RestBean;
import com.watchtogether.watchtogetherbackend.entity.sys.SysUser;

public interface LoginService {
    // 登录
    RestBean login(SysUser user);
}
