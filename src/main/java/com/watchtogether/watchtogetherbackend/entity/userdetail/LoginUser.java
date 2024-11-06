package com.watchtogether.watchtogetherbackend.entity.userdetail;

import com.alibaba.fastjson2.annotation.JSONField;
import com.watchtogether.watchtogetherbackend.entity.sys.SysUser;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Data
@NoArgsConstructor
public class LoginUser implements UserDetails {
    private SysUser user;
    private List<String> permissionsList; // 存储权限信息
    @JSONField(serialize = false) // 不序列化进redis中
    private List<SimpleGrantedAuthority> authorities; // 存储SpringSecurity所需要的权限信息集合

    public LoginUser(SysUser user, List<String> permissions) {
        this.user = user;
        this.permissionsList = permissions;
    }


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
