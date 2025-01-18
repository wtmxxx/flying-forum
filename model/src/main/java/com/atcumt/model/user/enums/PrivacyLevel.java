package com.atcumt.model.user.enums;

import lombok.Getter;

@Getter
public enum PrivacyLevel {
    PUBLIC("PUBLIC", "公开可见"),
    FOLLOWERS_ONLY("FOLLOWERS_ONLY", "仅粉丝可见"),
    MUTUAL_FOLLOWERS_ONLY("MUTUAL_FOLLOWERS_ONLY", "仅互关可见"),
    SPECIFIC_USERS_ONLY("SPECIFIC_USERS_ONLY", "仅指定用户可见"),
    ;

    private final String value;
    private final String description;

    // 构造方法
    PrivacyLevel(String value, String description) {
        this.value = value;
        this.description = description;
    }

    // 根据值获取枚举
    public static PrivacyLevel fromString(String value) {
        for (PrivacyLevel level : PrivacyLevel.values()) {
            if (level.name().equalsIgnoreCase(value)) {
                return level;
            }
        }
        return null;  // 返回null表示无效的值
    }
}