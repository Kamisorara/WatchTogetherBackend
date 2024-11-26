package com.watchtogether.watchtogetherbackend.service.sys.impl;

import com.watchtogether.watchtogetherbackend.mapper.sys.UserMapper;
import com.watchtogether.watchtogetherbackend.service.sys.UserService;
import com.watchtogether.watchtogetherbackend.utils.JWTUtil;
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
        String token = request.getHeader("token");
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

}
