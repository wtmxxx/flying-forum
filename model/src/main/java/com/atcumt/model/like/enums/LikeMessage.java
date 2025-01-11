package com.atcumt.model.like.enums;

import lombok.Getter;

@Getter
public enum LikeMessage {
    SORT_NOT_SUPPORT("不支持的排序方式"),
    CURSOR_FORMAT_INCORRECT("游标格式不正确");

    private final String message;

    LikeMessage(String message) {
        this.message = message;
    }
}
