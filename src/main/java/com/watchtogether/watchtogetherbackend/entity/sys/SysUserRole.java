package com.watchtogether.watchtogetherbackend.entity.sys;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * (SysUserRole)实体类
 *
 * @author Kamisora
 * @since 2024-10-31 17:13:13
 */
@Data
@TableName("sys_user_role")
@NoArgsConstructor
@AllArgsConstructor
public class SysUserRole implements Serializable {
    private static final long serialVersionUID = -36817374667016160L;
    /**
     * 用户唯一id
     */
    private Long userId;
    /**
     * 角色唯一id
     */
    private Long roleId;


    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

}

