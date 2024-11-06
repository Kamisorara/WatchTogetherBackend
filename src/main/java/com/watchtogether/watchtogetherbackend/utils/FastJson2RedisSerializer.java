package com.watchtogether.watchtogetherbackend.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.nio.charset.Charset;

/**
 * Redis使用Fastjson2序列化
 *
 * @author kamisora
 */
public class FastJson2RedisSerializer<T> implements RedisSerializer<T> {

    // 默认字符集为 UTF-8
    public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    // 泛型类型的 Class 对象，用于确定序列化和反序列化的数据类型
    private final Class<T> clazz;

    // 构造函数，用于初始化泛型类型的 Class 对象
    public FastJson2RedisSerializer(Class<T> clazz) {
        super(); // 调用父类的构造方法（此处可省略）
        this.clazz = clazz; // 保存类型信息供后续使用
    }

    @Override
    public byte[] serialize(T t) {
        // 如果对象为 null，返回一个空的字节数组
        if (t == null) {
            return new byte[0];
        }
        // 将对象序列化为 JSON 字符串，并转换为字节数组（使用 UTF-8 编码）
        // JSONWriter.Feature.WriteClassName 确保序列化过程中包含类的类型信息
        return JSON.toJSONString(t, JSONWriter.Feature.WriteClassName).getBytes(DEFAULT_CHARSET);
    }

    @Override
    public T deserialize(byte[] bytes) {
        // 如果字节数组为 null 或长度为 0，返回 null
        if (bytes == null || bytes.length <= 0) {
            return null;
        }
        // 将字节数组转换为字符串（使用 UTF-8 编码）
        String str = new String(bytes, DEFAULT_CHARSET);
        // 解析 JSON 字符串为指定类型的对象，支持自动类型识别
        return JSON.parseObject(str, clazz, JSONReader.Feature.SupportAutoType);
    }

    // 辅助方法，用于获取 JavaType 对象，可以用于 Jackson 框架的类型转换
    protected JavaType getJavaType(Class<?> clazz) {
        return TypeFactory.defaultInstance().constructType(clazz);
    }
}

