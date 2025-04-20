package com.atcumt.model.search.enums;

import lombok.Getter;

@Getter
public enum UserSearchSortType {
    DEFAULT("default", "默认"),
    MOST_FOLLOWER("most_follower", "最多粉丝"),
    LEAST_FOLLOWER("least_follower", "最少粉丝"),
    HIGHEST_LEVEL("highest_level", "最高等级"),
    LOWEST_LEVEL("lowest_level", "最低等级"),
    ;

    private final String value;
    private final String description;

    UserSearchSortType(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public static UserSearchSortType fromValue(String value) {
        for (UserSearchSortType userSearchSortType : UserSearchSortType.values()) {
            if (userSearchSortType.getValue().equalsIgnoreCase(value)) {
                return userSearchSortType;
            }
        }
        return DEFAULT;
    }
}