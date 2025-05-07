package com.wt.web.core.config.security;

import com.wt.web.core.filter.JWTAuthenticationTokenFilter;
import com.wt.web.core.handler.ExceptionHandler;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * SpringSecurity配置
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Resource
    private RedisAuthorizationRequestRepository redisAuthorizationRequestRepository;
    

    @Resource
    private ExceptionHandler exceptionHandler;

    @Resource
    private JWTAuthenticationTokenFilter jwtAuthenticationTokenFilter;

    @Value("${cors.allowedOrigins}")
    private String allowedOrigins;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Security核心配置
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthorizationRequestRepository authorizationRequestRepository) throws Exception {
        http.authorizeHttpRequests(auth -> auth.requestMatchers("/api/sys-test/**",
                "/api/wt-test/**",
                "/api/sys/register",
                "/websocket/**",
                "/api/sys/login",
                "/api/oauth2/**",
                "/login/oauth2/**",
                "/oauth2/authorization/**").permitAll().anyRequest().authenticated());

//        // OAuth2 登录配置
//        http.oauth2Login(oauth2 -> oauth2
//                .loginPage("/api/oauth2/login")
//                .authorizationEndpoint(authorization -> authorization
//                        .authorizationRequestRepository(redisAuthorizationRequestRepository))
//                .successHandler(oAuth2LoginSuccessHandler)
//                .failureHandler(oAuth2LoginFailureHandler)
//        );

        http.logout(logout -> logout.logoutUrl("api/sys/logout"));

        http.csrf(AbstractHttpConfigurer::disable);
        http.exceptionHandling(exception -> exception.authenticationEntryPoint(exceptionHandler));

        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        // JWT过滤
        http.addFilterBefore(jwtAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 将 AuthenticationManager 作为 Spring Bean 注入
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * 跨域配置
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 使用addAllowedOriginPattern代替addAllowedOrigin解决通配符问题
        configuration.addAllowedOriginPattern(allowedOrigins);
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
