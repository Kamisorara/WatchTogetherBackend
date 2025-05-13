package com.wt.web.core.filter.xss;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import java.util.HashMap;
import java.util.Map;

/**
 * XSS Request包装
 */
public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {

    public XssHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        return value != null ? clean(value) : null;
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values == null) {
            return null;
        }

        String[] cleanValues = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            cleanValues[i] = clean(values[i]);
        }
        return cleanValues;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> paramMap = super.getParameterMap();
        Map<String, String[]> cleanMap = new HashMap<>();

        for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
            String[] values = entry.getValue();
            String[] cleanValues = new String[values.length];

            for (int i = 0; i < values.length; i++) {
                cleanValues[i] = clean(values[i]);
            }

            cleanMap.put(entry.getKey(), cleanValues);
        }

        return cleanMap;
    }

    @Override
    public String getHeader(String name) {
        String value = super.getHeader(name);
        return value != null ? clean(value) : null;
    }

    /**
     * 使用jsoup清理XSS攻击内容
     */
    private String clean(String value) {
        if (value == null) {
            return null;
        }
        // 使用jsoup的Safelist.basic()允许基本的HTML标签，但移除危险内容
        return Jsoup.clean(value, Safelist.none());
    }
}
