package com.watchtogether.watchtogetherbackend.mapper.sys;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.watchtogether.watchtogetherbackend.entity.sys.SysMenu;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MenuMapper extends BaseMapper<SysMenu> {
    // 根据用户id查询用户对应的权限
    List<String> selectPermsByUserId(Long userId);
}
