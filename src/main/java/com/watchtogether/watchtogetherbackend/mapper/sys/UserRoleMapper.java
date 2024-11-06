package com.watchtogether.watchtogetherbackend.mapper.sys;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.watchtogether.watchtogetherbackend.entity.sys.SysUserRole;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserRoleMapper extends BaseMapper<SysUserRole> {
}
