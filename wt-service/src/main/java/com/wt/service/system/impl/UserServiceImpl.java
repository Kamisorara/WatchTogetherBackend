package com.wt.service.system.impl;

import com.wt.common.utils.JWTUtil;
import com.wt.dao.mapper.UserMapper;
import com.wt.entity.resp.UserInfoResp;
import com.wt.entity.system.SysUser;
import com.wt.service.system.UserService;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    @Resource
    private UserMapper userMapper;

    @Override
    public String getUserName(Long userId) {
        return userMapper.FindUserById(userId);
    }

    @Override
    public Long getUserIdFromServerletRequest(HttpServletRequest request) throws Exception {
        // 获取token
        String token = request.getHeader("Authorization");
        Claims claims = JWTUtil.parseJWT(token);
        String userId = claims.get("sub").toString();
        return Long.parseLong(userId);
    }

    @Override
    public Long getUserIdFromToken(String token) throws Exception {
        Claims claims = JWTUtil.parseJWT(token);
        String userId = claims.get("sub").toString();
        return Long.parseLong(userId);
    }

    @Override
    public Boolean JudgeOnlyEmail(String email) {
        Integer emailNum = userMapper.countUserEmail(email);
        return emailNum == 0;
    }

    @Override
    public void updateUserAvatarByToken(HttpServletRequest request, String avatarUrl) throws Exception {
        Long userId = getUserIdFromServerletRequest(request);
        userMapper.updateUserAvatar(userId, avatarUrl);

    }

    @Override
    public UserInfoResp getUserInfoByToken(HttpServletRequest request) throws Exception {
        Long userId = getUserIdFromServerletRequest(request);
        return userMapper.getUserInfoById(userId);
    }

    @Override
    public Boolean updateUserInfo(HttpServletRequest request, SysUser userInfo) throws Exception {
        Long userId = getUserIdFromServerletRequest(request);
        userInfo.setId(userId);  // 设置用户ID
        return userMapper.updateUserDetailInfo(userInfo) > 0;
    }

}
