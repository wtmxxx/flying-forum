package com.atcumt.common.enums;

import lombok.Getter;

@Getter
public enum RoleType {
    DEFAULT("user"),      // 默认类型
    ADMIN("admin"),       // 系统管理员
    USER("user"),         // 普通用户
    EXAMINER("examiner"), // 审查员
    GUEST("guest");       // 访客

    private final String code;

    RoleType(String code) {
        this.code = code;
    }

    /**
     * 根据代码获取 RoleType
     *
     * @param code 角色代码
     * @return 对应的 RoleType 枚举
     */
    public static RoleType fromCode(String code) {
        for (RoleType role : RoleType.values()) {
            if (role.getCode().equalsIgnoreCase(code)) {
                return role;
            }
        }
        throw new IllegalArgumentException("无效的角色代码: " + code);
    }
}
