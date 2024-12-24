package com.atcumt.model.post.enums;

import lombok.Getter;

@Getter
public enum PostStatus {
    DELETED(-1, "已删除"),
    DRAFT(0, "草稿"),
    UNDER_REVIEW(1, "审核中"),
    PUBLISHED(2, "已发布"),
    REJECTED(3, "审核不通过"),
    PRIVATE(4, "私密");

    private final int code;
    private final String description;

    // 构造函数
    PostStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    // 根据状态码获取枚举值
    public static PostStatus fromCode(int code) {
        for (PostStatus status : PostStatus.values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("无效的状态码: " + code);
    }
}