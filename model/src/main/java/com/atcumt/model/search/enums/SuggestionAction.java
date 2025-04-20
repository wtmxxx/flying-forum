package com.atcumt.model.search.enums;

import lombok.Getter;

@Getter
public enum SuggestionAction {
    SEARCH("SEARCH", "搜索"),
    TAG("TAG", "标签"),
    CUSTOM("CUSTOM", "自定义"),
    DELETE("DELETE", "删除"),
    ;

    private final String action;
    private final String description;

    SuggestionAction(String action, String description) {
        this.action = action;
        this.description = description;
    }
}