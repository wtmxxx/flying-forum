package com.atcumt.auth.mapper;

import com.atcumt.model.auth.entity.Role;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {
    List<Role> selectRolesByUserId(String userId);

    List<String> getValidRoleIds(@Param("roleIds") List<String> roleIds);
}
