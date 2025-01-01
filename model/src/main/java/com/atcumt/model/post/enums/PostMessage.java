package com.atcumt.model.post.enums;

import lombok.Getter;

@Getter
public enum PostMessage {
    // 帖子相关
    POST_NOT_FOUND("无此帖子"),
    POST_UNPUBLISHED("帖子未发布"),

    // 标签相关
    TAG_NOT_FOUND("无此标签"),
    TAG_LIST_EMPTY("标签列表不能为空"),
    TAG_COUNT_TOO_MANY("标签数量过多");

    private final String message;

    PostMessage(String message) {
        this.message = message;
    }
}
