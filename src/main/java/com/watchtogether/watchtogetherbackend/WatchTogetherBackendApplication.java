package com.watchtogether.watchtogetherbackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.watchtogether.watchtogetherbackend.mapper")
public class WatchTogetherBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(WatchTogetherBackendApplication.class, args);
    }

}
