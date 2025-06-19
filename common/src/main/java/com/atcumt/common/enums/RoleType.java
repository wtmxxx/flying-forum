package com.atcumt.common.enums;

import lombok.Getter;

@Getter
public enum RoleType {
    DEFAULT("user", "用户"),         // 默认类型
    ADMIN("admin", "管理员"),        // 系统管理员
    USER("user", "用户"),            // 普通用户
    EXAMINER("examiner", "审查员"),  // 审查员
    NEWS_PUBLISHER("news_publisher", "新闻发布者"),  // 新闻发布者
    KNOWLEDGE_BASE_MANAGER("knowledge_base_manager", "知识库管理员"),  // 知识库管理员
    GUEST("guest", "访客");         // 访客

    private final String code;
    private final String description;

    RoleType(String code, String description) {
        this.code = code;
        this.description = description;
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
