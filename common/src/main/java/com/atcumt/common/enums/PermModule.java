package com.atcumt.common.enums;

import lombok.Getter;

@Getter
public enum PermModule {
    ROLE("role", "角色"),                            // 角色模块
    USER_ROLE("user_role", "用户角色"),               // 用户角色模块
    PERMISSION("permission", "权限"),                // 权限模块
    ROLE_PERMISSION("role_permission", "角色权限"),   // 角色权限模块
    ACCOUNT("account", "账号"),                      // 账号模块
    QA("qa", "问答"),                                // 问答模块
    DISCUSSION("discussion", "杂谈"),                // 杂谈模块
    TEAM("team", "组队"),                            // 组队模块
    TRADE("trade", "交易"),                          // 交易模块
    HELP("help", "互助"),                            // 互助模块
    ACTIVITY("activity", "活动"),                    // 活动模块
    COMMENT("comment", "评论"),                      // 评论模块
    TAG("tag", "标签"),                              // 标签模块
    USER("user", "用户");                            // 用户模块

    private final String value;
    private final String description;

    PermModule(String value, String description) {
        this.value = value;
        this.description = description;
    }
}
