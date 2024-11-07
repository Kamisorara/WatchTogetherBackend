package com.watchtogether.watchtogetherbackend.config;

import com.watchtogether.watchtogetherbackend.filter.JWTAuthenticationTokenFilter;
import com.watchtogether.watchtogetherbackend.handler.ExceptionHandler;
import com.watchtogether.watchtogetherbackend.handler.LoginFailureHandler;
import com.watchtogether.watchtogetherbackend.handler.LoginSuccessHandler;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Resource
    private ExceptionHandler exceptionHandler;

    @Resource
    private LoginFailureHandler loginFailureHandler;

    @Resource
    private LoginSuccessHandler loginSuccessHandler;

    @Resource
    private JWTAuthenticationTokenFilter jwtAuthenticationTokenFilter;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Security核心配置
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth ->
                auth.requestMatchers("/api/sys-test/**", "/api/wt-test/**", "/api/sys/register")
                        .permitAll().anyRequest().authenticated());

        http.formLogin(login ->
                login.loginPage("/api/sys/login")
                        .successHandler(loginSuccessHandler)
                        .failureHandler(loginFailureHandler)
        );

        http.logout(logout ->
                logout.logoutUrl("api/sys/logout")).csrf().disable();
        http.exceptionHandling(exception -> exception.authenticationEntryPoint(exceptionHandler));

        http.cors().configurationSource(corsConfigurationSource());

        // add jwt
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
        configuration.addAllowedOrigin("http://localhost:5173");
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");
        configuration.setAllowCredentials(true); // 允许携带凭证
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
