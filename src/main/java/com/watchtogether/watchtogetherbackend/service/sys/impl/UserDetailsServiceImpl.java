package com.watchtogether.watchtogetherbackend.service.sys.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.watchtogether.watchtogetherbackend.entity.sys.SysUser;
import com.watchtogether.watchtogetherbackend.entity.userdetail.LoginUser;
import com.watchtogether.watchtogetherbackend.mapper.sys.MenuMapper;
import com.watchtogether.watchtogetherbackend.mapper.sys.UserMapper;
import jakarta.annotation.Resource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    @Resource
    private UserMapper userMapper;

    @Resource
    private MenuMapper menuMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getUserName, username);
        SysUser user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }
        List<String> permsList = menuMapper.selectPermsByUserId(user.getId());
        return new LoginUser(user, permsList);
    }
}
