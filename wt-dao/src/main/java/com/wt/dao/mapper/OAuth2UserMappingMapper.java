package com.wt.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wt.entity.system.OAuth2UserMapping;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OAuth2UserMappingMapper extends BaseMapper<OAuth2UserMapping> {

    // 查询用户第三方授权登录映射
    OAuth2UserMapping selectByProviderAndProviderId(@Param("provider") String provider,
                                                    @Param("providerUserId") String providerId);
}
