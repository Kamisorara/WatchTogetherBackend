package com.wt.service.helper;


import com.alibaba.fastjson2.annotation.JSONField;
import com.wt.entity.system.SysUser;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 登录用户信息封装类，实现 Spring Security 的 UserDetails 接口
 */
@Data
@NoArgsConstructor
public class LoginUser implements UserDetails {
    private SysUser user;
    // 权限字符串列表（如 "sys:user:add"）
    private List<String> permissionsList; // 存储权限信息
    @JSONField(serialize = false) // 不序列化进redis中
    // Spring Security 所需的权限对象集合，不序列化进 Redis
    private List<SimpleGrantedAuthority> authorities;

    public LoginUser(SysUser user, List<String> permissions) {
        this.user = user;
        this.permissionsList = permissions;
    }
    
    /**
     * 返回权限信息集合，供 Spring Security 授权使用
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (authorities == null) {
            return authorities;
        }
        authorities = new ArrayList<>(); // 把permissions中字符串类型的权限信息转换成GrantedAuthority对象存入authorities中
        for (String permission : permissionsList) {
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority(permission);
            authorities.add(authority);
        }
        return null;
    }

    @Override
    public String getPassword() {
        return user.getUserPassword();
    }

    @Override
    public String getUsername() {
        return user.getUserName();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
