package com.atcumt.model.user.enums;

import lombok.Getter;

@Getter
public enum UserMessage {
    // 关注相关
    CANNOT_FOLLOW_SELF("不能关注自己"),
    USER_NOT_FOUND("该用户不存在"),

    // 状态修改相关
    STATUS_INVALID("无效的状态"),
    STATUS_UPDATE_FAILED("状态更新失败"),

    // 用户隐私相关
    PRIVACY_SCOPE_INVALID("无效的隐私范围"),
    PRIVACY_LEVEL_INVALID("无效的隐私级别"),
    PRIVACY_UPDATE_FAILED("隐私设置更新失败"),
    SPECIFIC_USERS_EMPTY("指定用户列表为空"),
    SPECIFIC_USERS_TOO_MANY("指定用户列表过多"),
    FOLLOWING_PRIVACY_DENIED("因为对方的隐私设置，您无法查看对方的关注列表"),
    FOLLOWER_PRIVACY_DENIED("因为对方的隐私设置，您无法查看对方的粉丝列表"),
    LIKE_PRIVACY_DENIED("因为对方的隐私设置，您无法查看对方的喜欢列表"),
    COLLECTION_PRIVACY_DENIED("因为对方的隐私设置，您无法查看对方的收藏列表");

    private final String message; // 提示信息

    UserMessage(String message) {
        this.message = message;
    }
}
