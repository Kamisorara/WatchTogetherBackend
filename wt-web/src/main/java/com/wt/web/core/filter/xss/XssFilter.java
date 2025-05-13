package com.wt.web.core.filter.xss;


import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * XSS过滤器
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // 这个过滤器设定为最高优先级
public class XssFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // 判断URL是否需要排除或是否为multipart/form-data请求
        if (isExcludeUrl(httpRequest.getRequestURI()) || isMultipartRequest(httpRequest)) {
            chain.doFilter(request, response);
        } else {
            XssHttpServletRequestWrapper xssRequest = new XssHttpServletRequestWrapper(httpRequest);
            chain.doFilter(xssRequest, response);
        }
    }

    /**
     * 判断是否为需要排除的URL
     */
    private boolean isExcludeUrl(String url) {
        // 配置不需要XSS过滤的URL 文件上传类的基本都pass
        String[] excludeUrls = {"/api/sys/minio-upload", "/room/movie-upload"};

        for (String pattern : excludeUrls) {
            if (url.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否为multipart/form-data请求
     */
    private boolean isMultipartRequest(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().contains("multipart/form-data");
    }
}
