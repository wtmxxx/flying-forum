package com.atcumt.model.ai.enums;

import lombok.Getter;

@Getter
public enum FluxType {
    NEW_CONVERSATION("NEW_CONVERSATION", "新建对话"),
    TITLE("TITLE", "标题"),
    TEXT_MESSAGE("TEXT_MESSAGE", "文本消息"),
    REASONING_MESSAGE("REASONING_MESSAGE", "推理消息"),
    WEB_SEARCH_MESSAGE("WEB_SEARCH_MESSAGE", "网页搜索消息"),
    ;

    private final String value;
    private final String description;

    FluxType(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public static FluxType fromValue(String value) {
        for (FluxType type : FluxType.values()) {
            if (type.getValue().equals(value)) {
                return type;
            }
        }
        return null;
    }
}