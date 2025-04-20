package com.atcumt.auth.utils;

import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.atcumt.model.common.enums.DeviceType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.regex.Pattern;

public class DeviceUtil {
    public static String getDeviceType() {
        HttpServletRequest request = getCurrentHttpRequest();
        if (request == null) {
            return DeviceType.UNKNOWN.getDescription(); // 如果没有请求上下文，则返回 UNKNOWN
        }

        // 优先检查前端传递的设备类型（请求头或请求参数）
        String deviceTypeHeader = request.getHeader("Device-Type"); // 如果前端通过请求头传递设备类型
        if (deviceTypeHeader != null && !deviceTypeHeader.isEmpty()) {
            return parseDeviceType(deviceTypeHeader);
        }

        String deviceTypeParam = request.getParameter("deviceType"); // 如果前端通过 URL 参数传递设备类型
        if (deviceTypeParam != null && !deviceTypeParam.isEmpty()) {
            return parseDeviceType(deviceTypeParam);
        }

        // 如果没有设备类型信息，默认通过 User-Agent 判断设备类型
        String userAgent = request.getHeader("User-Agent");

        // 检查设备类型
        if (userAgent.isEmpty()) {
            return DeviceType.UNKNOWN.getDescription(); // 如果 User-Agent 为空，则返回 UNKNOWN
        }

        UserAgent ua = UserAgentUtil.parse(userAgent);

        // 首先判断是否为正常平台
        if (ua.getPlatform().isUnknown()) {
            return DeviceType.UNKNOWN.getDescription();
        }
        // 其次判断是否为浏览器
        // 若不是浏览器
        if (ua.getBrowser().isUnknown()) {
            // 判断是否为平板 || 移动 || 电脑
            if (ua.getPlatform().isIPad() || Pattern.compile("(?i).*(pad|tablet).*").matcher(userAgent).find()) {
                return DeviceType.PAD_CLIENT.getDescription();
            } else if (ua.isMobile()) {
                return DeviceType.MOBILE_CLIENT.getDescription();
            } else {
                return DeviceType.PC_CLIENT.getDescription();
            }
        } else {
            // 判断是否为平板 || 移动 || 电脑
            if (ua.getPlatform().isIPad() || Pattern.compile("(?i).*(pad|tablet).*").matcher(userAgent).find()) {
                return DeviceType.PAD_WEB.getDescription();
            } else if (ua.isMobile()) {
                return DeviceType.MOBILE_WEB.getDescription();
            } else {
                return DeviceType.PC_WEB.getDescription();
            }
        }
    }

    public static String getDeviceId() {
        HttpServletRequest request = getCurrentHttpRequest();
        if (request == null) {
            return getDeviceType(); // 如果没有请求上下文，则返回 设备类型 充当设备 ID
        }

        // 优先检查前端传递的设备 ID（请求头或请求参数）
        String deviceIdHeader = request.getHeader("Device-Id"); // 如果前端通过请求头传递设备 ID
        if (deviceIdHeader != null && !deviceIdHeader.isEmpty()) {
            return deviceIdHeader;
        }

        String deviceIdParam = request.getParameter("deviceId"); // 如果前端通过 URL 参数传递设备 ID
        if (deviceIdParam != null && !deviceIdParam.isEmpty()) {
            return deviceIdParam;
        }

        return getDeviceType(); // 如果没有设备 ID 信息，返回 设备类型 充当设备 ID
    }

    public static String getDeviceName() {
        HttpServletRequest request = getCurrentHttpRequest();
        if (request == null) {
            return "未知设备"; // 如果没有请求上下文，则返回 未知设备
        }

        // 优先检查前端传递的设备名称（请求头或请求参数）
        String deviceNameHeader = request.getHeader("Device-Name"); // 如果前端通过请求头传递设备名称
        if (deviceNameHeader != null && !deviceNameHeader.isEmpty()) {
            return deviceNameHeader;
        }

        String deviceNameParam = request.getParameter("deviceName"); // 如果前端通过 URL 参数传递设备名称
        if (deviceNameParam != null && !deviceNameParam.isEmpty()) {
            return deviceNameParam;
        }

        return "未知设备"; // 如果没有设备名称信息，返回 未知设备
    }

    // 获取当前请求的 HttpServletRequest
    private static HttpServletRequest getCurrentHttpRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        return (attributes != null) ? attributes.getRequest() : null;
    }

    // 辅助方法，解析设备类型
    private static String parseDeviceType(String deviceType) {
        try {
            // 判断请求中的设备类型是否是枚举中定义的值
            return DeviceType.valueOf(deviceType.toUpperCase()).getDescription();
        } catch (IllegalArgumentException e) {
            return DeviceType.UNKNOWN.getDescription(); // 如果传递的设备类型不是枚举值，返回 UNKNOWN
        }
    }
}
