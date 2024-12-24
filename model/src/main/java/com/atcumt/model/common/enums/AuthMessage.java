package com.atcumt.model.common.enums;

import lombok.Getter;

@Getter
public enum AuthMessage {
    // 用户名相关
    USERNAME_ALREADY_EXISTS("用户名已存在，请选择其他用户名"),
    USERNAME_NOT_EXISTS("用户名不存在，请先注册"),
    USERNAME_CHANGE_LIMIT_EXCEEDED("用户名修改频率限制，7天内只能修改一次"),

    // 密码相关
    WEAK_PASSWORD("密码强度不够，请使用更复杂的密码"),
    PASSWORD_INCORRECT("密码错误，请检查后重试"),
    PASSWORD_CHANGE_LIMIT_EXCEEDED("密码修改频率限制，7天内只能修改一次"),

    // 邮箱相关
    INVALID_EMAIL_FORMAT("邮箱格式不正确，请输入有效的邮箱地址"),
    EMAIL_ALREADY_REGISTERED("该邮箱已注册，请使用其他邮箱"),
    EMAIL_NOT_EXISTS("邮箱不存在，请先绑定"),
    EMAIL_CHANGE_LIMIT_EXCEEDED("邮箱修改频率限制，7天内只能修改一次"),

    // 验证码相关
    CAPTCHA_CODE_INCORRECT("图形验证码错误，请检查后重试"),
    VERIFICATION_CODE_INCORRECT("验证码错误，请检查后重试"),
    VERIFICATION_CODE_SEND_FAILURE("验证码发送失败，请稍后重试"),
    VERIFICATION_CODE_REQUEST_TOO_FREQUENT("验证码请求过快，请稍后再试"),

    // 注册相关
    SUCCESSFUL_REGISTRATION("注册成功！欢迎加入"),
    SID_ALREADY_REGISTERED("该学号已注册，请检查学号是否正确"),

    // 第三方登录相关
    QQ_ALREADY_BOUND("该QQ账号已绑定到其他用户"),
    QQ_AUTH_FAILURE("QQ认证失败"),
    QQ_NOT_BOUND("该QQ账号未绑定"),
    APPLE_ALREADY_BOUND("该APPLE账号已绑定到其他用户"),
    APPLE_AUTH_FAILURE("APPLE认证失败"),
    APPLE_NOT_BOUND("该APPLE账号未绑定"),
    WECHAT_ALREADY_BOUND("该微信账号已绑定到其他用户"),

    // 权限相关
    REFRESH_TOKEN_NOT_EXISTS("Refresh_Token失效或不存在"),
    PERMISSION_MISMATCH("权限不匹配，您没有足够的权限进行此操作"),

    // 统一认证
    UNIFIED_AUTH_FAILURE("统一身份认证失败"),
    SID_ALREADY_EXISTS("学号已注册"),

    // 系统相关
    SYSTEM_ERROR("系统错误，请稍后重试");

    private final String message;

    AuthMessage(String message) {
        this.message = message;
    }
}
