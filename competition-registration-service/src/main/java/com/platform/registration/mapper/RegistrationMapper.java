package com.platform.registration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.registration.entity.Registration;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RegistrationMapper extends BaseMapper<Registration> {
    // 基础 CRUD 由 MyBatis-Plus 自动提供，无需手写
}