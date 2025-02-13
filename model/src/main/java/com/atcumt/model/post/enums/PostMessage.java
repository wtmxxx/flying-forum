package com.atcumt.model.post.enums;

import lombok.Getter;

@Getter
public enum PostMessage {
    // 帖子相关
    POST_NOT_FOUND("无此帖子"),
    POST_UNPUBLISHED("帖子未发布"),
    POST_DELETED("帖子已删除"),

    // 新闻相关
    NEWS_NOT_FOUND("无此新闻"),
    NEWS_UNPUBLISHED("新闻未发布"),
    NEWS_LIST_EMPTY("新闻列表不能为空"),
    NEWS_COUNT_TOO_MANY("新闻数量过多"),

    // 标签相关
    TAG_NOT_FOUND("无此标签"),
    TAG_LIST_EMPTY("标签列表不能为空"),
    TAG_COUNT_TOO_MANY("标签数量过多"),
    TAG_CONTAINS_SENSITIVE_WORD("标签包含敏感词"),
    ;

    private final String message;

    PostMessage(String message) {
        this.message = message;
    }
}
