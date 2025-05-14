package com.wt.common.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * IP地址工具类
 */
public class IpUtils {

    /**
     * 获取客户端真实IP地址
     */
    public static String getIpAddress(HttpServletRequest request) {
        String ip = null;

        // 优先从 X-Forwarded-For 获取
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (!StringUtils.isEmpty(xForwardedFor) && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // 多级反向代理通过逗号分隔，第一个IP为客户端真实IP
            int index = xForwardedFor.indexOf(",");
            if (index != -1) {
                ip = xForwardedFor.substring(0, index);
            } else {
                ip = xForwardedFor;
            }
        }

        // 依次尝试获取IP
        String[] headerNames = {
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };

        for (String header : headerNames) {
            if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader(header);
            }
        }

        // 如果仍未获取到，则使用远程地址
        if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        return "0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : ip;
    }
}
