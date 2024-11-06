package com.watchtogether.watchtogetherbackend.controller.test.wt;

import com.watchtogether.watchtogetherbackend.entity.response.RestBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wt-test")
public class WTTestController {

    @GetMapping("/hello")
    public RestBean hello() {
        return RestBean.success("Hello World");
    }
}
