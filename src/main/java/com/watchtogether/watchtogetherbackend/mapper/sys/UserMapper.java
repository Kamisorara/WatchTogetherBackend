package com.watchtogether.watchtogetherbackend.mapper.sys;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.watchtogether.watchtogetherbackend.entity.sys.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper extends BaseMapper<SysUser> {
    // 根据用户Id查找用户名
    String FindUserById(Long userId);

    // 根据用户名查找用户id
    Long selectUserIdByUserName(String userName);

    // 根据用户id获取用户状态（是否启用）
    String getUserStatus(Long userId);

    // 查看同一邮箱的数目
    Integer countUserEmail(String email);

    // 更细用户头像
    Integer updateUserAvatar(@Param("userId") Long userId, @Param("avatarUrl") String avatarUrl);
}
