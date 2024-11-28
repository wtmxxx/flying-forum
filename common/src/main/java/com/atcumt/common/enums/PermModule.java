package com.atcumt.common.enums;

import lombok.Getter;

@Getter
public enum PermModule {
    ROLE("role", "角色"),                            // 角色模块
    USER_ROLE("user_role", "用户角色"),               // 用户角色模块
    PERMISSION("permission", "权限"),                // 权限模块
    ROLE_PERMISSION("role_permission", "角色权限"),   // 角色权限模块
    ACCOUNT("account", "账号"),                      // 账号模块
    QUESTION("question", "问题"),                    // 问题模块
    ANSWER("answer", "回答"),                        // 回答模块
    COMMENT("comment", "评论"),                      // 评论模块
    ARTICLE("article", "文章"),                      // 文章模块
    USER("user", "用户");                            // 用户模块

    private final String value;
    private final String description;

    PermModule(String value, String description) {
        this.value = value;
        this.description = description;
    }
}
