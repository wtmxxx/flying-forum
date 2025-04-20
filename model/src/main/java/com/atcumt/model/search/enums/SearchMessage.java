package com.atcumt.model.search.enums;

import lombok.Getter;

@Getter
public enum SearchMessage {
    SEARCH_TEXT_TOO_LONG("搜索文本过长"),
    SEARCH_CONTENT_TYPE_NOT_SUPPORTED("不支持的搜索类型"),
    ;

    private final String message;

    SearchMessage(String message) {
        this.message = message;
    }
}
