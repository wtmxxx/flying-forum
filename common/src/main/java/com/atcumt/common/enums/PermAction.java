package com.atcumt.common.enums;

import lombok.Getter;

@Getter
public enum PermAction {
    CREATE("create"),           // 新增操作
    DELETE("delete"),     // 删除操作
    UPDATE("update"),     // 更新操作
    READ("read");         // 查看操作

    private final String value;

    PermAction(String value) {
        this.value = value;
    }
}
