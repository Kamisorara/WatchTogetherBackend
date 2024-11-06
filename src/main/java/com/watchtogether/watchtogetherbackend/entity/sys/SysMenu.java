package com.watchtogether.watchtogetherbackend.entity.sys;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.mapping.Table;

/**
 * (SysMenu)实体类
 *
 * @author Kamisora
 * @since 2024-10-31 17:13:12
 */
@Data
@TableName("sys_menu")
@NoArgsConstructor
@AllArgsConstructor
public class SysMenu implements Serializable {
    private static final long serialVersionUID = 856102293383674597L;
    /**
     * 详细权限对应id
     */
    private Long id;
    /**
     * 权限名
     */
    private String menuName;
    /**
     * 权限表示（例如sys:common:user）
     */
    private String menuPerms;
    /**
     * 权限启用状态（0启用， 1禁用）
     */
    private String menuStatus;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMenuName() {
        return menuName;
    }

    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }

    public String getMenuPerms() {
        return menuPerms;
    }

    public void setMenuPerms(String menuPerms) {
        this.menuPerms = menuPerms;
    }

    public String getMenuStatus() {
        return menuStatus;
    }

    public void setMenuStatus(String menuStatus) {
        this.menuStatus = menuStatus;
    }

}

