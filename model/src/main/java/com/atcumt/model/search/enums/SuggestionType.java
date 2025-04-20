package com.atcumt.model.search.enums;

import lombok.Getter;

@Getter
public enum SuggestionType {
    SEARCH("SEARCH", "搜索"),
    TAG("TAG", "标签"),
    CUSTOM("CUSTOM", "自定义"),
    WHATEVER_TYPE("WHATEVER_TYPE", "无论何种类型"),
    ;

    private final String value;
    private final String description;

    SuggestionType(String value, String description) {
        this.value = value;
        this.description = description;
    }
}