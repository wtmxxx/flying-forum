package com.atcumt.common.enums;

import lombok.Getter;

@Getter
public enum PermModule {
    ROLE("role"),                         // 角色模块
    USER_ROLE("user_role"),               // 用户角色模块
    PERMISSION("permission"),             // 权限模块
    ROLE_PERMISSION("role_permission"),   // 角色权限模块
    ARTICLE("article"),                   // 文章模块
    COMMENT("comment"),                   // 评论模块
    USER("user");                         // 用户模块

    private final String value;

    PermModule(String value) {
        this.value = value;
    }
}
