package com.wt.web.core.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("WatchTogether API 文档")
                        .description("WatchTogether 项目 API 接口文档")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("AtlasKK")
                                .email("atlaskk.me")));
    }
}