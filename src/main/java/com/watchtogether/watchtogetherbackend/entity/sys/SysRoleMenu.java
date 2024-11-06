package com.watchtogether.watchtogetherbackend.entity.sys;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * (SysRoleMenu)实体类
 *
 * @author Kamisora
 * @since 2024-10-31 17:13:13
 */
@Data
@TableName("sys_role_menu")
@NoArgsConstructor
@AllArgsConstructor
public class SysRoleMenu implements Serializable {
    private static final long serialVersionUID = 327377879071088223L;
    /**
     * 角色id
     */
    private Long roleId;
    /**
     * 权限id
     */
    private Long menuId;


    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Long getMenuId() {
        return menuId;
    }

    public void setMenuId(Long menuId) {
        this.menuId = menuId;
    }

}

