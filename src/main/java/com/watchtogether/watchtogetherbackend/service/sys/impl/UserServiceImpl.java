package com.watchtogether.watchtogetherbackend.service.sys.impl;

import com.watchtogether.watchtogetherbackend.mapper.sys.UserMapper;
import com.watchtogether.watchtogetherbackend.service.sys.UserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    @Resource
    private UserMapper userMapper;

    @Override
    public String getUserName(Long userId) {
        return userMapper.FindUserById(userId);
    }
}
