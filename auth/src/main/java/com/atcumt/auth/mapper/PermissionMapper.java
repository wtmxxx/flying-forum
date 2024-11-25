package com.atcumt.auth.mapper;

import com.atcumt.model.auth.entity.Permission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {
    List<Permission> selectPermissionsByUserId(String userId);

    List<Permission> selectPermissionsByRoleId(String roleId);
}
