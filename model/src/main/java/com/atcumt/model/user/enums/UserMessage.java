package com.atcumt.model.user.enums;

import lombok.Getter;

@Getter
public enum UserMessage {
    // 关注相关
    CANNOT_FOLLOW_SELF("不能关注自己"),
    USER_NOT_FOUND("该用户不存在"),

    // 状态修改相关
    STATUS_INVALID("无效的状态"),
    STATUS_UPDATE_FAILED("状态更新失败");

    private final String message; // 提示信息

    UserMessage(String message) {
        this.message = message;
    }
}
