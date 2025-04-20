package com.atcumt.model.search.enums;

import lombok.Getter;

@Getter
public enum SearchContentType {
    POST("post", "帖子"),
    QUESTION("question", "问答"),
    DISCUSSION("discussion", "杂谈"),
    TEAM("team", "组队"),
    SECOND_HAND("trade", "二手"),
    INTERACTION("help", "互助"),
    ACTIVITY("activity", "活动"),
    NEWS("news", "新闻"),
    USER("user", "用户"),
    TAG("tag", "标签"),
    ;

    private final String value;
    private final String description;

    SearchContentType(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public static SearchContentType fromValue(String value) {
        for (SearchContentType searchContentType : SearchContentType.values()) {
            if (searchContentType.getValue().equalsIgnoreCase(value)) {
                return searchContentType;
            }
        }
        return POST;
    }
}