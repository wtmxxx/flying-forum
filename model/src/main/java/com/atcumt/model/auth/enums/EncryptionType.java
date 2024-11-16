package com.atcumt.model.auth.enums;

import lombok.Getter;

@Getter
public enum EncryptionType {
    BCRYPT("bcrypt"),
    ARGON2("argon2"),
    SHA256("sha256");

    private final String typeName;

    EncryptionType(String typeName) {
        this.typeName = typeName;
    }

    public static EncryptionType fromString(String typeName) {
        for (EncryptionType type : EncryptionType.values()) {
            if (type.typeName.equalsIgnoreCase(typeName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("不支持的加密类型: " + typeName);
    }

    /**
     * 获取带 {} 的加密类型标识
     *
     * @return 带 {} 的加密类型标识
     */
    public String getTypeWithBraces() {
        return "{" + typeName + "}";
    }
}
