package com.atcumt.model.common;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


@Getter
public enum DeviceType {
    APP("APP"),
    PC("PC"),
    UNKNOWN("Unknown");

    private final String description;

    // 构造函数
    DeviceType(String description) {
        this.description = description;
    }

    public static String getDeviceType() {
        HttpServletRequest request = getCurrentHttpRequest();
        if (request == null) {
            return DeviceType.UNKNOWN.getDescription(); // 如果没有请求上下文，则返回 UNKNOWN
        }

        String userAgent = request.getHeader("User-Agent").toLowerCase();

        // 检查常见的移动设备标识
        if (userAgent.contains("mobi")
                || userAgent.contains("mobile")
                || userAgent.contains("android")
                || userAgent.contains("iphone")
                || userAgent.contains("ipod")
                || userAgent.contains("windows phone")
                || userAgent.contains("harmony")
                || userAgent.contains("harmonyos")
        ) {
            return DeviceType.APP.getDescription();
        }

        // 检查桌面设备标识
        if (userAgent.contains("windows")
                || userAgent.contains("linux")
                || userAgent.contains("macintosh")
                || userAgent.contains("macos")
        ) {
            return DeviceType.PC.getDescription();
        }

        // 未识别设备类型
        return DeviceType.UNKNOWN.getDescription();
    }

    // 获取当前请求的 HttpServletRequest
    private static HttpServletRequest getCurrentHttpRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return (attributes != null) ? attributes.getRequest() : null;
    }
}
