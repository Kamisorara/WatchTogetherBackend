package com.wt.web.controller.system;

import com.wt.common.annotaion.RateLimit;
import com.wt.entity.resp.RestBean;
import com.wt.entity.system.SysUser;
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
    private MinioService minioService;

    /**
     * 登录接口 （兼容React 和 Vue两个屎山前端）
     * 支持三种方式提交登录信息：JSON请求体、表单提交和URL参数
     * 为了兼容不同前端实现方式的登录请求
     *
     * @param request   HTTP请求对象，用于从请求中获取参数
     * @param loginUser JSON请求体中的用户登录信息对象
     * @param username  URL参数或表单中的用户名
     * @param password  URL参数或表单中的密码
     * @return 返回登录结果，包含token信息或错误信息
     */

    @PostMapping("/login")
    @RateLimit(limit = 5, message = "访问过于频繁")
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
     * 用户注册接口 （兼容React 和 Vue两个屎山前端）
     * 支持三种方式提交注册信息：JSON请求体、表单提交和URL参数
     * 为了兼容不同前端实现方式的注册请求
     *
     * @param request             HTTP请求对象，用于从请求中获取参数
     * @param registerBody        JSON请求体中的注册信息映射
     * @param usernameParam       URL参数或表单中的用户名
     * @param passwordParam       URL参数或表单中的密码
     * @param passwordRepeatParam URL参数或表单中的确认密码
     * @param emailParam          URL参数或表单中的邮箱
     * @return 返回注册结果，成功或错误信息
     */
    @PostMapping("/register")
    @RateLimit(limit = 5, message = "访问过于频繁")
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
        if (StringUtils.isEmpty(username)
                || StringUtils.isEmpty(password)
                || StringUtils.isEmpty(passwordRepeat)
                || StringUtils.isEmpty(email)) {
            return RestBean.error(400, "注册失败");
        }
        return loginService.register(username, password, passwordRepeat, email);
    }

    /**
     * 退出登录接口
     * 清除用户的登录状态和相关会话信息
     *
     * @return 返回退出登录结果
     */
    @PostMapping("/logout")
    public RestBean logout() {
        return loginService.logout();
    }

    /**
     * 获取当前登录用户信息
     * 通过请求中的Token获取用户的基本数据
     *
     * @param request HTTP请求对象，用于获取Authorization头信息中的Token
     * @return 返回用户基本信息数据
     */
    @GetMapping("/user-info")
    @RateLimit(key = "#request.getHeader('Authorization') + ':' + #request.getRequestURI()", limit = 5, message = "访问过于频繁")
    public RestBean getUserInfo(HttpServletRequest request) throws Exception {
        return RestBean.success(userService.getUserInfoByToken(request));
    }

//    /**
//     * 测试根据token返回用户id
//     */
//    @RateLimit(limit = 5, message = "访问过于频繁")
//    @GetMapping("/token-test")
//    public RestBean tokenTest(HttpServletRequest request) throws Exception {
//        Long userId = userService.getUserIdFromServerletRequest(request);
//        return RestBean.success(userId);
//    }


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
     * 用户头像上传接口（MinIO存储）
     * 上传用户头像并更新用户信息
     *
     * @param file    上传的头像文件
     * @param request HTTP请求对象，用于获取当前用户信息
     * @return 返回上传结果，包含头像URL
     */
    @PostMapping("/minio-upload")
    @RateLimit(limit = 2, message = "访问过于频繁")
    public RestBean minioUpload(@RequestParam("file") MultipartFile file,
                                HttpServletRequest request) throws Exception {
        String resultUrl = minioService.uploadImg(file);
        userService.updateUserAvatarByToken(request, resultUrl);
        Map<String, String> avatarInfo = Map.of("url", resultUrl);
        return RestBean.success(avatarInfo);
    }

    /**
     * 更新用户个人资料
     * 根据请求中的Token识别用户并更新其个人资料
     *
     * @param request  HTTP请求对象，用于获取当前用户信息
     * @param userInfo 包含需要更新的用户信息的对象
     * @return 返回更新结果，成功或错误信息
     */
    @PostMapping("/update-userDetailInfo")
    @RateLimit(key = "#request.getHeader('Authorization') + ':' + #request.getRequestURI()", limit = 5, message = "访问过于频繁")
    public RestBean updateUserDetailInfo(HttpServletRequest request,
                                         @RequestBody(required = false) SysUser userInfo) throws Exception {
        // 如果是JSON请求体
        if (userInfo != null) {
            if (userService.updateUserInfo(request, userInfo)) {
                return RestBean.success("数据更新成功");
            }
            return RestBean.error(400, "数据更新失败");
        }

//        // 兼容旧屎山
//        String userPhone = request.getParameter("userPhone");
//        String userSex = request.getParameter("userSex");
//
//        if (userSex != null && (userSex.equals("1") || userSex.equals("0") || userSex.equals("2"))) {
//            SysUser user = new SysUser();
//            user.setUserPhone(userPhone);
//            user.setUserSex(userSex);
//            if (userService.updateUserInfo(request, user)) {
//                return RestBean.success("数据更新成功");
//            }
//        }
        return RestBean.error(400, "数据错误，更新失败");
    }

    /**
     * 修改用户密码（兼容React 和 Vue两个屎山前端）
     * 根据请求中的Token识别用户并更新其密码
     *
     * @param request      HTTP请求对象，用于获取当前用户信息
     * @param passwordBody 包含新密码和确认密码的JSON对象
     * @return 返回密码修改结果，成功或错误信息
     */
    @PostMapping("/update-password")
    @RateLimit(key = "#request.getHeader('Authorization') + ':' + #request.getRequestURI()", limit = 5, message = "访问过于频繁")
    public RestBean updatePassword(HttpServletRequest request,
                                   @RequestBody(required = false) Map<String, String> passwordBody) {
        String newPassword;
        String confirmPassword;

        // 优先使用JSON请求体
        if (passwordBody != null) {
            newPassword = passwordBody.get("newPassword");
            confirmPassword = passwordBody.get("confirmPassword");
        } else {
            // 从请求参数获取
            newPassword = request.getParameter("newPassword");
            confirmPassword = request.getParameter("confirmPassword");
        }

        // 校验参数
        if (StringUtils.isEmpty(newPassword) || StringUtils.isEmpty(confirmPassword)) {
            return RestBean.error(400, "参数不完整");
        }

        // 确认两次输入的新密码一致
        if (!newPassword.equals(confirmPassword)) {
            return RestBean.error(400, "两次输入的新密码不一致");
        }

        try {
            // 调用Service方法更新密码（使用加密）
            if (userService.updateUserPassword(request, newPassword)) {
                return RestBean.success("密码修改成功");
            } else {
                return RestBean.error(500, "密码修改失败");
            }
        } catch (Exception e) {
            return RestBean.error(500, "密码修改失败: " + e.getMessage());
        }
    }

    /**
     * 刷新访问令牌
     * 使用refresh token刷新access token，并根据条件对refresh token进行续签
     *
     * @return 返回刷新结果，包含新的token信息或错误信息
     */
    @PostMapping("/refresh-token")
    public RestBean refreshToken() {
        return loginService.refreshToken();
    }

}
