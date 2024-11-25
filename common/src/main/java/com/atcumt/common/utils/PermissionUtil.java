package com.atcumt.common.utils;

import com.atcumt.common.enums.PermAction;
import com.atcumt.common.enums.PermModule;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class PermissionUtil {

    /**
     * 动态生成权限字符串
     *
     * @param module 模块
     * @param action 操作
     * @return 权限字符串
     */
    public static String generate(PermModule module, PermAction action) {
        return module.getValue() + "." + action.getValue();
    }

    /**
     * 批量生成权限
     *
     * @param module  模块
     * @param actions 操作集合
     * @return 权限列表
     */
    public static List<String> generateBatch(PermModule module, PermAction... actions) {
        return Arrays.stream(actions)
                .map(action -> generate(module, action))
                .collect(Collectors.toList());
    }

    /**
     * 校验权限是否完全匹配
     *
     * @param requiredPermission 所需权限
     * @param actualPermission   实际权限
     * @return 是否匹配
     */
    public static boolean hasPermission(String requiredPermission, String actualPermission) {
        return requiredPermission.equals(actualPermission);
    }

    /**
     * 校验权限是否任意匹配
     *
     * @param requiredPermissions 所需权限集合
     * @param actualPermissions   实际权限集合
     * @return 是否拥有任意匹配的权限
     */
    public static boolean hasAnyPermission(List<String> requiredPermissions, List<String> actualPermissions) {
        // 检查 requiredPermissions 是否有至少一个在 actualPermissions 中
        return requiredPermissions.stream().anyMatch(actualPermissions::contains);
    }

    /**
     * 校验所有权限是否完全匹配
     *
     * @param requiredPermissions 所需权限集合
     * @param actualPermissions   实际权限集合
     * @return 是否拥有全部权限
     */
    public static boolean hasAllPermissions(List<String> requiredPermissions, List<String> actualPermissions) {
        // 检查 actualPermissions 是否包含所有 requiredPermissions
        return new HashSet<>(actualPermissions).containsAll(requiredPermissions);
    }
}

