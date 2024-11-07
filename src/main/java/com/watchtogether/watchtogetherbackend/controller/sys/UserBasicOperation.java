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
public class UserBasicOperation {
    @Resource
    private LoginService loginService;

    /**
     * 登录
     * @param request
     * @return
     */
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

    /**
     * 注册
     * @param request
     * @return
     */
    @PostMapping("/register")
    public RestBean register(HttpServletRequest request) {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String passwordRepeat = request.getParameter("passwordRepeat");
        String email = request.getParameter("email");
        return loginService.register(username, password, passwordRepeat, email);
    }

    /**
     * 退出
     * @return
     */
    @PostMapping("/logout")
    public RestBean logout() {
        return loginService.logout();
    }

}
