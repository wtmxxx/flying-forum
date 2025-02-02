package com.atcumt.common.config;

import cn.dev33.satoken.same.SaSameUtil;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.atcumt.model.auth.entity.Role;
import com.atcumt.model.auth.vo.PermissionVO;
import com.atcumt.model.auth.vo.RoleVO;
import com.atcumt.model.common.entity.Result;
import com.atcumt.model.common.enums.ResultCode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
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
@ConditionalOnMissingBean(StpInterface.class)
@Slf4j
public class StpInterfaceImpl implements StpInterface {
    private final RedisTemplate<String, String> redisTemplate;
    private final LoadBalancerClient loadBalancerClient;
    private final WebClient.Builder webClientBuilder;
    private WebClient webClient;

    @Autowired
    public StpInterfaceImpl(
            @Qualifier("saRedisTemplate") RedisTemplate<String, String> redisTemplate,
            LoadBalancerClient loadBalancerClient,
            WebClient.Builder webClientBuilder
    ) {
        this.redisTemplate = redisTemplate;
        this.loadBalancerClient = loadBalancerClient;
        this.webClientBuilder = webClientBuilder;
    }

    @PostConstruct
    public void init() {
        ServiceInstance instance = loadBalancerClient.choose("auth-service");

        if (instance != null) {
//            System.out.println("instance = " + instance.getUri());

            webClient = webClientBuilder
                    .baseUrl(instance.getUri() + "/api/auth/admin")
                    .build();
        } else {
            log.warn("未找到 auth-service 服务实例");
        }
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
                this.getRole(loginId.toString()).getData(),
                Role.class
        );

        // 从数据库获取权限
        for (Role role : roles) {
            List<PermissionVO> partPermissionVOs = BeanUtil.copyToList(
                    this.getPermission(role.getRoleId()).getData(),
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
            List<RoleVO> roleVOs = this.getRole(loginId.toString()).getData();
            List<String> roleNameList = roleVOs.stream().map(RoleVO::getRoleName).toList();

            // 更新缓存并返回
            redisTemplate.opsForValue().set(roleKey, String.join(",", roleNameList), 24, TimeUnit.HOURS);
            return roleNameList;
        } else {
            // 如果缓存命中，直接返回
            return List.of(roleNames.split(","));
        }
    }

    public Result<List<RoleVO>> getRole(String userId) {
        init();
        // 使用 ParameterizedTypeReference 来指定泛型类型
        try {
            return webClient.get()
                    .uri("/role/v1/user")
                    .header(SaSameUtil.SAME_TOKEN, SaSameUtil.getToken())
                    .header(StpUtil.getTokenName(), StpUtil.getTokenValueNotCut())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Result<List<RoleVO>>>() {
                    })
                    .doOnError(e ->
                            log.error("远程调用AuthClient#getRole方法出现异常，参数：{}", userId, e)
                    )
                    .block(); // 使用 block() 来阻塞等待结果
        } catch (Exception e) {
            throw new RuntimeException(ResultCode.INTERNAL_SERVER_ERROR.getMessage());
        }
    }

    public Result<List<PermissionVO>> getPermission(String roleId) {
        init();
        // 使用 ParameterizedTypeReference 来指定泛型类型
        try {
            return webClient.get()
                    .uri("/permission/v1/role?roleId={roleId}", roleId)
                    .header(SaSameUtil.SAME_TOKEN, SaSameUtil.getToken())
                    .header(StpUtil.getTokenName(), StpUtil.getTokenValueNotCut())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Result<List<PermissionVO>>>() {
                    })
                    .doOnError(e ->
                            log.error("远程调用AuthClient#getPermission方法出现异常，参数：{}", roleId, e)
                    )
                    .block(); // 使用 block() 来阻塞等待结果
        } catch (Exception e) {
            throw new RuntimeException(ResultCode.INTERNAL_SERVER_ERROR.getMessage());
        }
    }
}
