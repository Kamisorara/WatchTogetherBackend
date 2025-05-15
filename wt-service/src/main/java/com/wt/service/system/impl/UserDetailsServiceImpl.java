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
 * 用户详情服务实现类
 * 实现Spring Security的UserDetailsService接口
 * 负责根据用户名加载用户认证信息和授权信息
 * 支持用户登录认证和权限校验的核心组件
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    @Resource
    private UserMapper userMapper;

    @Resource
    private MenuMapper menuMapper;

    /**
     * 根据用户名加载用户详细信息
     * 查询用户基本信息并加载其对应权限列表
     * 用于Spring Security认证和授权过程
     *
     * @param username 用户登录时提供的用户名
     * @return 包含用户信息和权限的UserDetails对象
     * @throws UsernameNotFoundException 当用户名不存在时由Spring Security框架抛出
     * @throws RuntimeException          当用户名或密码错误时抛出
     */
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
