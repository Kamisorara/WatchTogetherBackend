package com.watchtogether.watchtogetherbackend.controller.test.sys;

import com.watchtogether.watchtogetherbackend.entity.response.RestBean;
import com.watchtogether.watchtogetherbackend.service.sys.UserService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sys-test")
public class UserTestController {

    @Resource
    private UserService userService;

    @PostMapping("/hello")
    public RestBean Hello(@RequestParam("id") Long userId) {
        String userName = userService.getUserName(userId);
        return RestBean.success(userName);
    }
}
