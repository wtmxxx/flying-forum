package com.atcumt.auth.utils;

import com.atcumt.auth.mapper.RoleMapper;
import com.atcumt.model.auth.entity.Role;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AuthRedisUtil {
    private final RoleMapper roleMapper;
    private final RedisTemplate<String, String> saRedisTemplate;

    public AuthRedisUtil(
            RoleMapper roleMapper,
            @Qualifier("saRedisTemplate") RedisTemplate<String, String> saRedisTemplate
    ) {
        this.roleMapper = roleMapper;
        this.saRedisTemplate = saRedisTemplate;
    }


    public void deleteRedisUserRole(String userId) {
        String roleKey = "Authorization:userRole:" + userId;

        saRedisTemplate.delete(roleKey);
    }

    public void deleteRedisUserRole(List<String> userIds) {
        List<String> roleKeys = userIds.stream()
                .map(roleKey -> "Authorization:userRole:" + roleKey)
                .toList();

        saRedisTemplate.delete(roleKeys);
    }

    public void deleteRedisRolePermission(String roleName) {
        String permissionKey = "Authorization:rolePermission:" + roleName;

        saRedisTemplate.delete(permissionKey);
    }

    public void deleteRedisRolePermission(List<String> roleNames) {
        List<String> permissionKey = roleNames.stream()
                .map(roleName -> "Authorization:rolePermission:" + roleName).toList();

        saRedisTemplate.delete(permissionKey);
    }

    public void deleteRedisRolePermissionById(String roleId) {
        Role role = roleMapper.selectOne(Wrappers
                .<Role>lambdaQuery()
                .eq(Role::getRoleId, roleId)
                .select(Role::getRoleName)
        );

        if (role == null) throw new IllegalArgumentException("角色不存在");

        String roleName = role.getRoleName();

        String permissionKey = "Authorization:rolePermission:" + roleName;

        saRedisTemplate.delete(permissionKey);
    }

    public void deleteRedisRolePermissionByIds(List<String> roleIds) {
        List<Role> roles = roleMapper.selectByIds(roleIds);

        if (roles == null || roles.isEmpty()) throw new IllegalArgumentException("角色不存在");

        List<String> permissionKey = roles.stream()
                .map(role -> "Authorization:rolePermission:" + role.getRoleName()).toList();

        saRedisTemplate.delete(permissionKey);
    }
}
