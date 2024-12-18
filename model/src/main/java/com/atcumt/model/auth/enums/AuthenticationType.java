package com.atcumt.model.auth.enums;

import lombok.Getter;

@Getter
public enum AuthenticationType {
    UNIFIED_AUTH("unified_auth", "统一身份认证"),
    EMAIL("email", "学校邮箱认证"),
    CHSI("chsi", "学信网认证");

    private final String typeName;
    private final String description;

    AuthenticationType(String typeName, String description) {
        this.typeName = typeName;
        this.description = description;
    }
}
