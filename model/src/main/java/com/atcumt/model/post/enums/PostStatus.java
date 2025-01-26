package com.atcumt.model.post.enums;

import lombok.Getter;

@Getter
public enum PostStatus {
    DELETED("DELETED", "已删除"),
    DRAFT("DRAFT", "草稿"),
    UNDER_REVIEW("UNDER_REVIEW", "审核中"),
    PUBLISHED("PUBLISHED", "已发布"),
    REJECTED("REJECTED", "审核不通过"),
    PRIVATE("PRIVATE", "私密");

    private final String code;
    private final String description;

    PostStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static PostStatus fromCode(String code) {
        for (PostStatus status : PostStatus.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("无效的状态码: " + code);
    }
}