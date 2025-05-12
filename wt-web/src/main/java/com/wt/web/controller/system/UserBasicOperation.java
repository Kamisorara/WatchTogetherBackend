package com.wt.web.controller.system;

import com.wt.entity.resp.RestBean;
import com.wt.entity.system.SysUser;
import com.wt.service.fastdfs.FastDFSService;
import com.wt.service.minio.MinioService;
import com.wt.service.system.LoginService;
import com.wt.service.system.UserService;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 用户基本操作Controller
 */

@RestController
@RequestMapping("/api/sys")
public class UserBasicOperation {
    @Resource
    private LoginService loginService;
    @Resource
    private UserService userService;
    @Resource
    private FastDFSService fastDFSService;
    @Resource
    private MinioService minioService;

    /**
     * 登录 (支持JSON格式、表单提交和URL参数) 为了兼容两个屎山前端
     */
    @PostMapping("/login")
    public RestBean login(HttpServletRequest request,
                          @RequestBody(required = false) SysUser loginUser,
                          @RequestParam(name = "username", required = false) String username,
                          @RequestParam(name = "password", required = false) String password) {
        SysUser user = new SysUser();

        // 优先使用JSON请求中的数据
        if (loginUser != null && loginUser.getUserName() != null) {
            user = loginUser;
        } else if (username != null && password != null) {
            // 其次使用 RequestParam
            user.setUserName(username);
            user.setUserPassword(password);
        } else {
            // 最后尝试从request中获取参数 兼容原有方式
            username = request.getParameter("username");
            password = request.getParameter("password");
            user.setUserName(username);
            user.setUserPassword(password);
        }
        // 登录操作
        return loginService.login(user);
    }

    /**
     * 注册 (支持JSON格式、表单提交和URL参数) 为了兼容两个屎山前端
     */
    @PostMapping("/register")
    public RestBean register(HttpServletRequest request,
                             @RequestBody(required = false) Map<String, String> registerBody,
                             @RequestParam(name = "username", required = false) String usernameParam,
                             @RequestParam(name = "password", required = false) String passwordParam,
                             @RequestParam(name = "passwordRepeat", required = false) String passwordRepeatParam,
                             @RequestParam(name = "email", required = false) String emailParam) {
        String username;
        String password;
        String passwordRepeat;
        String email;

        // 优先使用JSON request body
        if (registerBody != null) {
            username = registerBody.get("username");
            password = registerBody.get("password");
            passwordRepeat = registerBody.get("passwordRepeat");
            email = registerBody.get("email");
        } else if (usernameParam != null) {
            // 其次使用RequestParam
            username = usernameParam;
            password = passwordParam;
            passwordRepeat = passwordRepeatParam;
            email = emailParam;
        } else {
            // 最后从request中获取
            username = request.getParameter("username");
            password = request.getParameter("password");
            passwordRepeat = request.getParameter("passwordRepeat");
            email = request.getParameter("email");
        }
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
     */
    @PostMapping("/logout")
    public RestBean logout() {
        return loginService.logout();
    }

    /**
     * 从Token中获取用户基本数据
     */
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


//    /**
//     * FastDFS上传更改用户头像
//     */
//    @PostMapping("/fastdfs-upload")
//    public RestBean fastdfsUpload(@RequestParam("file") MultipartFile file, HttpServletRequest request) throws Exception {
//        String resultUrl = fastDFSService.uploadImg(file);
//        userService.updateUserAvatarByToken(request, resultUrl);
//        return RestBean.success("头像更新成功");
//    }

    /**
     * MinIO上传头像
     */
    @PostMapping("/minio-upload")
    public RestBean minioUpload(@RequestParam("file") MultipartFile file, HttpServletRequest request) throws Exception {
        String resultUrl = minioService.uploadImg(file);
        userService.updateUserAvatarByToken(request, resultUrl);
        return RestBean.success("头像更新成功");
    }


    /**
     * 更新用户资料 兼容屎山
     */
    @PostMapping("/update-userDetailInfo")
    public RestBean updateUserDetailInfo(HttpServletRequest request,
                                         @RequestBody(required = false) Map<String, String> requestBody,
                                         @RequestParam(name = "userPhone", required = false) String userPhoneParam,
                                         @RequestParam(name = "userSex", required = false) String userSexParam) throws Exception {

        String userPhone;
        String userSex;

        // 优先使用JSON请求体
        if (requestBody != null && !requestBody.isEmpty()) {
            userPhone = requestBody.get("userPhone");
            userSex = requestBody.get("userSex");
        } else if (userPhoneParam != null) {
            // 其次使用RequestParam
            userPhone = userPhoneParam;
            userSex = userSexParam;
        } else {
            // 最后从request中获取
            userPhone = request.getParameter("userPhone");
            userSex = request.getParameter("userSex");
        }

        if (userSex != null && (userSex.equals("1") || userSex.equals("0") || userSex.equals("2"))) {
            if (userService.updateUserPhoneAndSexInfo(request, userPhone, userSex)) {
                return RestBean.success("数据更新成功");
            }
        }
        return RestBean.error(400, "数据错误，更新失败");
    }

}
