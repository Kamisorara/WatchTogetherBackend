package com.wt.entity.Enum;

import lombok.Getter;

/**
 * OAuth2认证Provider枚举
 */
@Getter
public enum OAuth2Provider {
    GITHUB("github", "id"),
    GOOGLE("google", "sub");

    private final String registrationId;
    private final String userIdAttribute;

    OAuth2Provider(String registrationId, String userIdAttribute) {
        this.registrationId = registrationId;
        this.userIdAttribute = userIdAttribute;
    }

}
