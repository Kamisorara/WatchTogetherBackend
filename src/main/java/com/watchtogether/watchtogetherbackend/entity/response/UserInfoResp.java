package com.watchtogether.watchtogetherbackend.entity.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户信息回复类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResp {
    private Long id;
    private String userName;
    private String userEmail;
    private String userPhone;
    private String userAvatar;
    private Character userSex;
}
