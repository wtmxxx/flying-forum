package com.atcumt.common.enums;

import lombok.Getter;

@Getter
public enum PermAction {
    CREATE("create", "新增"),                     // 新增操作
    DELETE("delete", "删除"),                     // 删除操作
    UPDATE("update", "更新"),                     // 更新操作
    READ("read", "查看"),                         // 查看操作
    DISABLE("disable", "封禁"),                   // 封禁操作
    UNTIE_DISABLE("untie_disable", "解除封禁");    // 解除封禁操作

    private final String value;
    private final String description;

    PermAction(String value, String description) {
        this.value = value;
        this.description = description;
    }
}
