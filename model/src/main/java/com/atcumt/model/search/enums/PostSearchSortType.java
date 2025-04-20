package com.atcumt.model.search.enums;

import lombok.Getter;

@Getter
public enum PostSearchSortType {
    DEFAULT("default", "默认"),
    MOST_LIKED("most_liked", "最多点赞"),
    MOST_COMMENTED("most_commented", "最多评论"),
    LATEST_POST("latest_post", "最新发布"),
    EARLIEST_POST("earliest_post", "最早发布"),
    ;

    private final String value;
    private final String description;

    PostSearchSortType(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public static PostSearchSortType fromValue(String value) {
        for (PostSearchSortType postSearchSortType : PostSearchSortType.values()) {
            if (postSearchSortType.getValue().equalsIgnoreCase(value)) {
                return postSearchSortType;
            }
        }
        return DEFAULT;
    }
}