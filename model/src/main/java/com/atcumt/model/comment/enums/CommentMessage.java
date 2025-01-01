package com.atcumt.model.comment.enums;

import lombok.Getter;

@Getter
public enum CommentMessage {
    SENSITIVE_WORD("评论内容含有敏感词，无法发表"),
    COMMENT_NOT_FOUND("评论不存在"),
    MEDIA_FILE_LIMIT("评论图片数量超过限制"),
    SORT_NOT_SUPPORT("不支持的排序方式"),
    CURSOR_FORMAT_INCORRECT("游标格式不正确");

    private final String message;

    CommentMessage(String message) {
        this.message = message;
    }
}
