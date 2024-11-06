package com.watchtogether.watchtogetherbackend.entity.sys;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * (SysRole)实体类
 *
 * @author Kamisora
 * @since 2024-10-31 17:13:13
 */
@Data
@TableName("sys_role")
@NoArgsConstructor
@AllArgsConstructor
public class SysRole implements Serializable {
    private static final long serialVersionUID = 863641301396697161L;
    /**
     * 角色对应id
     */
    private Long id;
    /**
     * 角色名
     */
    private Object roleName;
    /**
     * 角色权限字符串例如admin或user
     */
    private String roleKey;
    /**
     * 角色启用状态（0启用， 1禁用）
     */
    private String roleStatus;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Object getRoleName() {
        return roleName;
    }

    public void setRoleName(Object roleName) {
        this.roleName = roleName;
    }

    public String getRoleKey() {
        return roleKey;
    }

    public void setRoleKey(String roleKey) {
        this.roleKey = roleKey;
    }

    public String getRoleStatus() {
        return roleStatus;
    }

    public void setRoleStatus(String roleStatus) {
        this.roleStatus = roleStatus;
    }

}

