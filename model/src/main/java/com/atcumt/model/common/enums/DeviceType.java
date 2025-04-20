package com.atcumt.model.common.enums;

import lombok.Getter;

@Getter
public enum DeviceType {
    MOBILE_CLIENT("MOBILE_CLIENT"),
    PC_CLIENT("PC_CLIENT"),
    PAD_CLIENT("PAD_CLIENT"),
    MOBILE_WEB("MOBILE_WEB"),
    PC_WEB("PC_WEB"),
    PAD_WEB("PAD_WEB"),
    UNKNOWN("UNKNOWN");

    private final String description;

    // 构造函数
    DeviceType(String description) {
        this.description = description;
    }
}
