package com.atcumt.common.config;

import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.atcumt.common.api.auth.PermissionDubboService;
import com.atcumt.common.api.auth.RoleDubboService;
import com.atcumt.model.auth.entity.Role;
import com.atcumt.model.auth.vo.PermissionVO;
import com.atcumt.model.auth.vo.RoleVO;
import com.atcumt.model.common.enums.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;


/**
 * 角色和权限鉴定的实现类（sa-token）<br>
 * Pattern: USER-ROLE-PERMISSION <br>
 * Cache: Redis <br>
 * Remote: WebClient <br>
 * Attention: 请为每个角色设定至少一个权限，否则会导致必然的网络请求
 */
@Component
@ConditionalOnClass(StpUtil.class)
@ConditionalOnMissingBean({StpInterface.class})
@ConditionalOnMissingClass("com.atcumt.gateway.GatewayApplication")
@Slf4j
public class StpInterfaceImpl implements StpInterface {
    private final RedisTemplate<String, String> redisTemplate;

    @DubboReference
    private RoleDubboService roleDubboService;
    @DubboReference
    private final PermissionDubboService permissionDubboService;

    @Autowired
    public StpInterfaceImpl(
            @Qualifier("saRedisTemplate") RedisTemplate<String, String> redisTemplate,
            PermissionDubboService permissionDubboService) {
        this.redisTemplate = redisTemplate;
        this.permissionDubboService = permissionDubboService;
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

        Set<String> roleNameSet = new HashSet<>();

        if (roleNames != null && !roleNames.isEmpty()) {
            // 如果缓存命中，尝试从缓存中获取权限
            boolean isCached = true;
            for (String roleName : roleNames.split(",")) {
                String permissionKey = "Authorization:rolePermission:" + roleName;
                String permissionName = redisTemplate.opsForValue().get(permissionKey);
                if (permissionName == null || permissionName.isEmpty()) {
                    isCached = false;
                } else {
                    roleNameSet.add(roleName);
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
                this.getRole(loginId.toString()),
                Role.class
        );

        // 从数据库获取权限
        for (Role role : roles) {
            if (roleNameSet.contains(role.getRoleName())) {
                continue;
            }
            List<PermissionVO> partPermissionVOs = BeanUtil.copyToList(
                    this.getPermission(role.getRoleId()),
                    PermissionVO.class
            );

            // 如果没有权限 ID，跳过查询（这里会导致没有权限的角色永远不会被缓存）
            if (partPermissionVOs == null || partPermissionVOs.isEmpty()) {
                continue;
            }

            List<String> partPermissionNames = partPermissionVOs.stream()
                    .map(PermissionVO::getPermissionName).toList();

            permissionNames.addAll(partPermissionNames);

            String permissionKey = "Authorization:rolePermission:" + role.getRoleName();
            redisTemplate.opsForValue().set(permissionKey, String.join(",", partPermissionNames), 7, TimeUnit.DAYS);
        }

        // 更新用户角色缓存
        redisTemplate.opsForValue().set(roleKey, String.join(",", roles.stream().map(Role::getRoleName).toList()), 7, TimeUnit.DAYS);

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
            List<RoleVO> roleVOs = this.getRole(loginId.toString());
            List<String> roleNameList = roleVOs.stream().map(RoleVO::getRoleName).toList();

            // 更新缓存并返回
            redisTemplate.opsForValue().set(roleKey, String.join(",", roleNameList), 7, TimeUnit.DAYS);
            return roleNameList;
        } else {
            // 如果缓存命中，直接返回
            return List.of(roleNames.split(","));
        }
    }

    public List<RoleVO> getRole(String userId) {
        try {
            return roleDubboService.getUserRole(userId);
        } catch (Exception e) {
            log.error("获取用户角色失败", e);
            throw new RuntimeException(ResultCode.INTERNAL_SERVER_ERROR.getMessage());
        }
    }

    public List<PermissionVO> getPermission(String roleId) {
        try {
            return permissionDubboService.getRolePermissions(roleId);
        } catch (Exception e) {
            log.error("获取角色权限失败", e);
            throw new RuntimeException(ResultCode.INTERNAL_SERVER_ERROR.getMessage());
        }
    }
}
