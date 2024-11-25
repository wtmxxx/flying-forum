package com.atcumt.common.config;

import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.core.bean.BeanUtil;
import com.atcumt.common.api.client.AuthClient;
import com.atcumt.model.auth.entity.Role;
import com.atcumt.model.auth.vo.PermissionVO;
import com.atcumt.model.auth.vo.RoleVO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * 角色和权限鉴定的实现类（sa-token）
 * Pattern: USER-ROLE-PERMISSION
 * Cache: Redis
 */
@Component
@ConditionalOnMissingClass({"com.atcumt.auth.AuthApplication"})
public class StpInterfaceImpl implements StpInterface {
    private final RedisTemplate<String, String> redisTemplate;
    private final AuthClient authClient;

    public StpInterfaceImpl(
            @Qualifier("saRedisTemplate") RedisTemplate<String, String> redisTemplate,
            AuthClient authClient
    ) {
        this.redisTemplate = redisTemplate;
        this.authClient = authClient;
    }

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
        List<Role> roles = BeanUtil.copyToList(
                authClient.getRole(loginId.toString()).getData(),
                Role.class
        );

        // 从数据库获取权限
        for (Role role : roles) {
            List<PermissionVO> partPermissionVOs = BeanUtil.copyToList(
                    authClient.getPermission(role.getRoleId()).getData(),
                    PermissionVO.class
            );

            // 如果没有权限 ID，跳过查询
            if (partPermissionVOs == null || partPermissionVOs.isEmpty()) {
                continue;
            }

            List<String> partPermissionNames = partPermissionVOs.stream()
                    .map(PermissionVO::getPermissionName).toList();

            permissionNames.addAll(partPermissionNames);

            String permissionKey = "Authorization:rolePermission:" + role.getRoleName();
            redisTemplate.opsForValue().set(permissionKey, String.join(",", partPermissionNames), 24, TimeUnit.HOURS);
        }

        // 更新用户角色缓存
        redisTemplate.opsForValue().set(roleKey, String.join(",", roles.stream().map(Role::getRoleName).toList()), 24, TimeUnit.HOURS);

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
            // 缓存未命中，用OpenFeign查询角色
            List<RoleVO> roleVOs = authClient.getRole(loginId.toString()).getData();
            List<String> roleNameList = roleVOs.stream().map(RoleVO::getRoleName).toList();

            // 更新缓存并返回
            redisTemplate.opsForValue().set(roleKey, String.join(",", roleNameList), 24, TimeUnit.HOURS);
            return roleNameList;
        } else {
            // 如果缓存命中，直接返回
            return List.of(roleNames.split(","));
        }
    }
}
