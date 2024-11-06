package com.watchtogether.watchtogetherbackend.controller.sys;

import com.watchtogether.watchtogetherbackend.entity.response.RestBean;
import com.watchtogether.watchtogetherbackend.entity.sys.SysUser;
import com.watchtogether.watchtogetherbackend.service.sys.LoginService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sys")
public class UserBaseOperation {
    @Resource
    private LoginService loginService;


    @PostMapping("/login")
    public RestBean login(HttpServletRequest request) {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        SysUser user = new SysUser();
        user.setUserName(username);
        user.setUserPassword(password);
        // 登录操作
        return loginService.login(user);
    }
}
