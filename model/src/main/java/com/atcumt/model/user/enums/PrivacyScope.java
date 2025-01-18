package com.atcumt.model.user.enums;

import lombok.Getter;

@Getter
public enum PrivacyScope {
    FOLLOWING("FOLLOWING", "关注对象的隐私设置"),
    FOLLOWER("FOLLOWER", "粉丝的隐私设置"),
    LIKE("LIKE", "喜欢内容的隐私设置"),
    COLLECTION("COLLECTION", "收藏内容的隐私设置");

    private final String value;
    private final String description;

    // 构造方法
    PrivacyScope(String value, String description) {
        this.value = value;
        this.description = description;
    }

    // 根据值获取枚举
    public static PrivacyScope fromString(String value) {
        for (PrivacyScope scope : PrivacyScope.values()) {
            if (scope.value.equalsIgnoreCase(value)) {
                return scope;
            }
        }
        return null;  // 返回null表示无效的值
    }
}
