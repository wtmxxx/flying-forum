package com.atcumt.model.common;

import lombok.Getter;

@Getter
public enum AuthMessage {
    USERNAME_ALREADY_EXISTS("用户名已存在，请选择其他用户名"),
    USERNAME_NOT_EXISTS("用户名不存在，请先注册"),
    EMAIL_ALREADY_REGISTERED("该邮箱已注册，请使用其他邮箱"),
    STUDENT_ID_ALREADY_REGISTERED("该学号已注册，请检查学号是否正确"),
    WEAK_PASSWORD("密码强度不够，请使用更复杂的密码"),
    INVALID_EMAIL_FORMAT("邮箱格式不正确，请输入有效的邮箱地址"),
    SUCCESSFUL_REGISTRATION("注册成功！欢迎加入"),
    UNIFIED_AUTH_FAILURE("统一身份认证失败"),
    QQ_OAUTH_ID_ALREADY_BOUND("该QQ账号已绑定到其他用户"),
    WECHAT_OAUTH_ID_ALREADY_BOUND("该微信账号已绑定到其他用户"),
    SYSTEM_ERROR("系统错误，请稍后重试");

    private final String message;

    AuthMessage(String message) {
        this.message = message;
    }
}
