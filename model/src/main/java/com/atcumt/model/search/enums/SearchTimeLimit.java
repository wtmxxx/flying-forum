package com.atcumt.model.search.enums;

import lombok.Getter;

@Getter
public enum SearchTimeLimit {
    ALL("all", "全部"),
    ONE_DAY("one_day", "一天内"),
    THREE_DAYS("three_days", "三天内"),
    ONE_WEEK("one_week", "一周内"),
    ONE_MONTH("one_month", "一个月内"),
    THREE_MONTHS("three_months", "三个月内"),
    HALF_YEAR("half_year", "半年内"),
    ONE_YEAR("one_year", "一年内"),
    ;

    private final String value;
    private final String description;

    SearchTimeLimit(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public static SearchTimeLimit fromValue(String value) {
        for (SearchTimeLimit searchTimeLimit : SearchTimeLimit.values()) {
            if (searchTimeLimit.getValue().equalsIgnoreCase(value)) {
                return searchTimeLimit;
            }
        }
        return ALL;
    }
}