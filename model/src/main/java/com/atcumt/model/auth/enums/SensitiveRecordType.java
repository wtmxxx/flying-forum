package com.atcumt.model.auth.enums;

import lombok.Getter;

@Getter
public enum SensitiveRecordType {
    LOGIN("LOGIN", "登录"),
    CHANGE_PASSWORD("CHANGE_PASSWORD", "修改密码"),
    CHANGE_USERNAME("CHANGE_USERNAME", "修改用户名"),
    CHANGE_EMAIL("CHANGE_EMAIL", "修改邮箱"),
    RESET_PASSWORD("RESET_PASSWORD", "重置密码"),
    BIND_ACCOUNT("BIND_ACCOUNT", "绑定第三方账户"),
    UNBIND_ACCOUNT("UNBIND_ACCOUNT", "解绑第三方账户"),
    CHANGE_PHONE("CHANGE_PHONE", "修改手机号");

    private final String type;
    private final String description;

    SensitiveRecordType(String type, String description) {
        this.type = type;
        this.description = description;
    }
}
