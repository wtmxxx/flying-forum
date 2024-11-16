package com.atcumt.auth.config;

import cn.dev33.satoken.stp.StpInterface;
import com.atcumt.auth.mapper.PermissionMapper;
import com.atcumt.auth.mapper.RoleMapper;
import com.atcumt.auth.mapper.RolePermissionMapper;
import com.atcumt.auth.mapper.UserRoleMapper;
import com.atcumt.model.auth.entity.Permission;
import com.atcumt.model.auth.entity.Role;
import com.atcumt.model.auth.entity.RolePermission;
import com.atcumt.model.auth.entity.UserRole;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


/**
 * 角色和权限鉴定的实现类（sa-token）
 * Pattern: USER-ROLE-PERMISSION
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {
    private final RedisTemplate<String, String> redisTemplate;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;

    /**
     * 返回一个账号所拥有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        String roleKey = "Authorization:userRole:" + loginId;

        // 获取缓存中的角色列表
        String roleNames = redisTemplate.opsForValue().get(roleKey);
        List<String> permissionNames = new ArrayList<>();

        if (roleNames != null && !roleNames.isEmpty()) {
            // 如果缓存命中，尝试从缓存中获取权限
            boolean isCached = true;
            for (String roleName : roleNames.split(",")) {
                String permissionKey = "Authorization:rolePermission:" + roleName;
                String permissionName = redisTemplate.opsForValue().get(permissionKey);
                if (permissionName == null || permissionName.isEmpty()) {
                    isCached = false;
                } else {
                    permissionNames.addAll(List.of(permissionName.split(",")));
                }
            }

            // 如果缓存中已经有所有权限，直接返回
            if (isCached) {
                return permissionNames;
            }
        }

        // 如果缓存没有，查询数据库并更新缓存
        List<String> roleIds = userRoleMapper.selectObjs(Wrappers
                .<UserRole>lambdaQuery()
                .eq(UserRole::getUserId, loginId)
                .select(UserRole::getRoleId));

        List<Role> roles = roleMapper.selectList(Wrappers
                .<Role>lambdaQuery()
                .in(Role::getRoleId, roleIds)
        );

        // 从数据库获取权限
        for (Role role : roles) {
            List<String> partPermissionIds = rolePermissionMapper.selectList(Wrappers
                    .<RolePermission>lambdaQuery()
                    .eq(RolePermission::getRoleId, role.getRoleId())
                    .select(RolePermission::getPermissionId)
            ).stream().map(RolePermission::getPermissionId).toList();

            List<String> partPermissionNames = permissionMapper.selectList(Wrappers
                    .<Permission>lambdaQuery()
                    .in(Permission::getPermissionId, partPermissionIds)
                    .select(Permission::getPermissionName)
            ).stream().map(Permission::getPermissionName).toList();

            permissionNames.addAll(partPermissionNames);

            String permissionKey = "Authorization:rolePermission:" + role.getRoleName();
            redisTemplate.opsForValue().set(permissionKey, String.join(",", partPermissionNames));
        }

        // 更新用户角色缓存
        redisTemplate.opsForValue().set(roleKey, String.join(",", roles.stream().map(Role::getRoleName).toList()));

        return permissionNames;
    }

    /**
     * 返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        String roleKey = "Authorization:userRole:" + loginId;

        // 获取角色列表缓存
        String roleNames = redisTemplate.opsForValue().get(roleKey);

        if (roleNames == null || roleNames.isEmpty()) {
            // 缓存未命中，从数据库查询角色
            List<String> roleIds = userRoleMapper.selectObjs(Wrappers
                    .<UserRole>lambdaQuery()
                    .eq(UserRole::getUserId, loginId)
                    .select(UserRole::getRoleId)
            );

            List<String> roleNameList = roleMapper.selectObjs(Wrappers
                    .<Role>lambdaQuery()
                    .in(Role::getRoleId, roleIds)
                    .select(Role::getRoleName)
            );

            // 更新缓存并返回
            redisTemplate.opsForValue().set(roleKey, String.join(",", roleNameList));
            return roleNameList;
        } else {
            // 如果缓存命中，直接返回
            return List.of(roleNames.split(","));
        }
    }
}
