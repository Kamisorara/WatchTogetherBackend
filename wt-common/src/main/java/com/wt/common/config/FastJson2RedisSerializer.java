package com.wt.common.config;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.filter.Filter;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.nio.charset.Charset;

/**
 * Redis使用Fastjson2序列化
 *
 * @author Waylon
 */
public class FastJson2RedisSerializer<T> implements RedisSerializer<T> {

    // 默认字符集为 UTF-8
    public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    // 泛型类型的 Class 对象，用于确定序列化和反序列化的数据类型
    private final Class<T> clazz;

    // 修改这里，添加 LoginUser 类到过滤器中
    private static final Filter AUTO_TYPE_FILTER = JSONReader.autoTypeFilter(
            "org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest",
            "org.springframework.security.oauth2.core.AuthorizationGrantType",
            "org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponseType",
            "org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames",
            "com.wt.service.helper.LoginUser"
    );

    // 构造函数，用于初始化泛型类型的 Class 对象
    public FastJson2RedisSerializer(Class<T> clazz) {
        super();
        this.clazz = clazz;
    }

    @Override
    public byte[] serialize(T t) {
        if (t == null) {
            return new byte[0];
        }
        return JSON.toJSONString(t, JSONWriter.Feature.WriteClassName).getBytes(DEFAULT_CHARSET);
    }

    @Override
    public T deserialize(byte[] bytes) {
        if (bytes == null || bytes.length <= 0) {
            return null;
        }
        String str = new String(bytes, DEFAULT_CHARSET);
        return JSON.parseObject(str, clazz, AUTO_TYPE_FILTER);
    }

    protected JavaType getJavaType(Class<?> clazz) {
        return TypeFactory.defaultInstance().constructType(clazz);
    }
}