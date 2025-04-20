package com.atcumt.model.post.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

public enum PostType {

    QUESTION("question", "问答"),
    DISCUSSION("discussion", "杂谈"),
    TEAM("team", "组队"),
    SECOND_HAND("trade", "二手"),
    INTERACTION("help", "互助"),
    ACTIVITY("activity", "活动"),
    NEWS("news", "新闻");

    private final String value;
    @Getter
    private final String description;

    // 构造函数
    PostType(String value, String description) {
        this.value = value;
        this.description = description;
    }

    @JsonValue // 使用@JsonValue来定义枚举值序列化时应该使用的值
    public String getValue() {
        return value;
    }

}
