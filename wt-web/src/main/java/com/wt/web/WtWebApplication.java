package com.wt.web;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan(basePackages = "com.wt.dao.mapper")
@ComponentScan(basePackages = {
        "com.wt.web",
        "com.wt.dao",
        "com.wt.entity",
        "com.wt.common",
        "com.wt.service"
})
public class WtWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(WtWebApplication.class, args);
    }

}
