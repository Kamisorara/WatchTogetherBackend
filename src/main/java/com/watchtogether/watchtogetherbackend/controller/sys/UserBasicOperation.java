package com.watchtogether.watchtogetherbackend.controller.sys;

import com.watchtogether.watchtogetherbackend.entity.response.RestBean;
import com.watchtogether.watchtogetherbackend.entity.sys.SysUser;
import com.watchtogether.watchtogetherbackend.service.fastdfs.FastDFSService;
import com.watchtogether.watchtogetherbackend.service.sys.LoginService;
import com.watchtogether.watchtogetherbackend.service.sys.UserService;
import com.watchtogether.watchtogetherbackend.utils.FastDFSClient;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/sys")
public class UserBasicOperation {
    @Resource
    private LoginService loginService;
    @Resource
    private UserService userService;
    @Resource
    private FastDFSService fastDFSService;

    /**
     * 登录
     *
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
     *
     * @param request
     * @return
     */
    @PostMapping("/register")
    public RestBean register(HttpServletRequest request) {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String passwordRepeat = request.getParameter("passwordRepeat");
        String email = request.getParameter("email");
        if (StringUtils.isEmpty(username) ||
                StringUtils.isEmpty(password) ||
                StringUtils.isEmpty(passwordRepeat) ||
                StringUtils.isEmpty(email)) {
            return RestBean.error(400, "注册失败");
        }
        return loginService.register(username, password, passwordRepeat, email);
    }

    /**
     * 退出
     *
     * @return
     */
    @PostMapping("/logout")
    public RestBean logout() {
        return loginService.logout();
    }

    @GetMapping("/user-info")
    public RestBean getUserInfo(HttpServletRequest request) throws Exception {
        return RestBean.success(userService.getUserInfoByToken(request));
    }

    /**
     * 测试根据token返回用户id
     */
    @GetMapping("/token-test")
    public RestBean tokenTest(HttpServletRequest request) throws Exception {
        Long userId = userService.getUserIdFromServerletRequest(request);
        return RestBean.success(userId);
    }


    /**
     * FastDFS上传文件测试
     */
    @PostMapping("/fastdfs-upload")
    public RestBean fastdfsUpload(@RequestParam("file") MultipartFile file, HttpServletRequest request) throws Exception {
        String resultUrl = fastDFSService.uploadImg(file);
        userService.updateUserAvatarByToken(request, resultUrl);
        return RestBean.success("头像更新成功");
    }


    @PostMapping("/update-userDetailInfo")
    public RestBean updateUserDetailInfo(HttpServletRequest request, @RequestParam("userPhone") String userPhone, @RequestParam("userSex") String userSex) throws Exception {
        if (userSex.equals("1") || userSex.equals("0") || userSex.equals("2")) {
            if (userService.updateUserPhoneAndSexInfo(request, userPhone, userSex)) {
                return RestBean.success("数据更新成功");
            }
        }
        return RestBean.error(400, "数据错误，更新失败");
    }

}
