package com.wt.service.system.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wt.dao.mapper.MenuMapper;
import com.wt.dao.mapper.UserMapper;
import com.wt.entity.system.SysUser;
import com.wt.service.helper.LoginUser;
import jakarta.annotation.Resource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 根据用户名加载用户信息实现类
 */

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
