package com.watchtogether.watchtogetherbackend.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * JWT工具类，用于生成和解析JSON Web Token（JWT）
 *
 * @author Kamisora
 */
public class JWTUtil {

    // 定义JWT的有效期，7天（以毫秒为单位）
    public static final Long JWT_TTL = 60 * 60 * 24 * 7 * 1000L; // 7天

    // 设置密钥的明文
    public static final String JWT_KEY = "TestTokenSecretKey";

    /**
     * 生成UUID
     *
     * @return 去掉“-”的UUID字符串
     */
    public static String getUUID() {
        String token = UUID.randomUUID().toString().replaceAll("-", "");
        return token;
    }

    /**
     * 生成JWT
     *
     * @param subject token中要存放的数据（通常为JSON格式）
     * @return 生成的JWT字符串
     */
    public static String createJWT(String subject) {
        JwtBuilder builder = getJwtBuilder(subject, null, getUUID()); // 不指定过期时间，使用默认有效期
        return builder.compact();
    }

    /**
     * 生成JWT，指定超时时间
     *
     * @param subject   token中要存放的数据
     * @param ttlMillis token超时时间（以毫秒为单位）
     * @return 生成的JWT字符串
     */
    public static String createJWT(String subject, Long ttlMillis) {
        JwtBuilder builder = getJwtBuilder(subject, ttlMillis, getUUID()); // 设置过期时间
        return builder.compact();
    }

    /**
     * 获取JWT构造器
     *
     * @param subject   token中要存放的数据
     * @param ttlMillis token有效期
     * @param uuid      token的唯一标识
     * @return JwtBuilder对象，用于生成JWT
     */
    private static JwtBuilder getJwtBuilder(String subject, Long ttlMillis, String uuid) {
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256; // 设置签名算法为HS256
        SecretKey secretKey = generalKey(); // 生成秘钥
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);

        // 如果ttlMillis为null，则使用默认有效期
        if (ttlMillis == null) {
            ttlMillis = JWTUtil.JWT_TTL;
        }

        // 计算过期时间
        long expMillis = nowMillis + ttlMillis;
        Date expDate = new Date(expMillis);

        // 构建JWT
        return Jwts.builder()
                .setId(uuid)              // 设置唯一ID
                .setSubject(subject)       // 设置主题，可以是JSON数据
                .setIssuer("Kamisora")     // 签发者
                .setIssuedAt(now)          // 签发时间
                .signWith(signatureAlgorithm, secretKey) // 设置签名算法和密钥
                .setExpiration(expDate);   // 设置过期时间
    }

    /**
     * 创建JWT，指定ID、subject和超时时间
     *
     * @param id        token的唯一标识
     * @param subject   token中要存放的数据
     * @param ttlMillis token超时时间
     * @return 生成的JWT字符串
     */
    public static String createJWT(String id, String subject, Long ttlMillis) {
        JwtBuilder builder = getJwtBuilder(subject, ttlMillis, id); // 设置过期时间
        return builder.compact();
    }

    public static void main(String[] args) throws Exception {
        // 示例JWT字符串
        String token = "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJiZTI2YmRhOWNlMGI0MTQxOGIzMDA1YTVkNGQ2YjcwZSIsInN1YiI6IjMiLCJpc3MiOiJLYW1pc29yYSIsImlhdCI6MTY1MjMzNTU3MSwiZXhwIjoxNjUyOTQwMzcxfQ.OjQfcu8Cdyn3LCoxFuEhlgmJypn7mPaYlU4Ca2Jy2XU";

        // 解析JWT
        Claims claims = parseJWT(token);

        // 输出JWT的相关信息
        System.out.println(claims.get("iss"));      // 签发者
        System.out.println(claims.get("sub"));      // 主题
        System.out.println(claims.getIssuer());     // 获取签发者
        System.out.println(claims);                 // 输出整个Claims
    }

    /**
     * 生成加密后的秘钥
     *
     * @return 加密后的SecretKey对象
     */
    public static SecretKey generalKey() {
        // 将JWT_KEY进行Base64解码并生成AES加密的秘钥
        byte[] encodedKey = Base64.getDecoder().decode(JWTUtil.JWT_KEY);
        SecretKey key = new SecretKeySpec(encodedKey, 0, encodedKey.length, "AES");
        return key;
    }

    /**
     * 解析JWT
     *
     * @param jwt 要解析的JWT字符串
     * @return 解析后的Claims对象，包含JWT中的信息
     * @throws Exception 解析失败时抛出异常
     */
    public static Claims parseJWT(String jwt) throws Exception {
        SecretKey secretKey = generalKey(); // 生成解密用的秘钥
        return Jwts.parser()
                .setSigningKey(secretKey)     // 设置秘钥
                .parseClaimsJws(jwt)          // 解析JWT
                .getBody();                   // 获取解析后的Claims
    }
}
