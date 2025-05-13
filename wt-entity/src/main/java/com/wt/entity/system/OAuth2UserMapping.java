package com.wt.entity.system;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 第三方登录用户映射
 *
 * @author Waylon
 */

@Data
@TableName("oauth2_user_mapping")
public class OAuth2UserMapping {
    /**
     * 用户映射id
     */
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 第三方登录提供商
     */
    private String provider;

    /**
     * 第三方登录提供商用户全局唯一id
     */
    private String providerUserId;

    /**
     * 第三方登录提供商用户昵称
     */
    private LocalDateTime createdTime;
}