package com.atcumt.common.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class WebUtil {

    /**
     * 获取ServletRequestAttributes
     *
     * @return ServletRequestAttributes
     */
    public static ServletRequestAttributes getServletRequestAttributes() {
        RequestAttributes ra = RequestContextHolder.getRequestAttributes();
        if (ra == null) {
            log.warn("Could not find current ServletRequestAttributes");
            return null;
        }
        return (ServletRequestAttributes) ra;
    }

    /**
     * 获取request
     *
     * @return HttpServletRequest
     */
    public static HttpServletRequest getRequest() {
        ServletRequestAttributes servletRequestAttributes = getServletRequestAttributes();
        if (servletRequestAttributes == null) {
            log.warn("Could not find current HttpServletRequest");
            return null;
        }
        return servletRequestAttributes.getRequest();
    }

    /**
     * 获取response
     *
     * @return HttpServletResponse
     */
    public static HttpServletResponse getResponse() {
        ServletRequestAttributes servletRequestAttributes = getServletRequestAttributes();
        if (servletRequestAttributes == null) {
            log.warn("Could not find current HttpServletResponse");
            return null;
        }
        return servletRequestAttributes.getResponse();
    }

    /**
     * 获取request header中的内容
     *
     * @param headerName 请求头名称
     * @return 请求头的值
     */
    public static String getHeader(String headerName) {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return null;
        }
        return request.getHeader(headerName);
    }

    public static void setResponseHeader(String key, String value) {
        HttpServletResponse response = getResponse();
        if (response == null) {
            return;
        }
        response.setHeader(key, value);
    }

    public static boolean isSuccess() {
        HttpServletResponse response = getResponse();
        return response != null && response.getStatus() < 300;
    }

    /**
     * 获取请求地址中的请求参数组装成 key1=value1&key2=value2
     * 如果key对应多个值，中间使用逗号隔开例如 key1对应value1，key2对应value2，value3， key1=value1&key2=value2,value3
     *
     * @param request 请求体
     * @return 返回拼接字符串
     */
    public static String getParameters(HttpServletRequest request) {
        Map<String, String[]> parameterMap = request.getParameterMap();
        return getParameters(parameterMap);
    }

    /**
     * 获取请求地址中的请求参数组装成 key1=value1&key2=value2
     * 如果key对应多个值，中间使用逗号隔开例如 key1对应value1，key2对应value2，value3， key1=value1&key2=value2,value3
     *
     * @param queries 请求参数
     * @return 返回拼接字符串
     */
    public static <T> String getParameters(final Map<String, T> queries) {
        return queries.entrySet().stream()
                .map(entry -> {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if (value instanceof String[]) {
                        return key + "=" + String.join(",", (String[]) value);
                    } else if (value instanceof Collection) {
                        return key + "=" + CollUtil.join((Collection<?>) value, ",");
                    } else {
                        return key + "=" + value.toString();
                    }
                })
                .collect(Collectors.joining("&"));
    }

    /**
     * 获取请求url中的path
     *
     * @param url 链接
     * @return 返回path
     */
    public static String getPath(String url) {
        if (StrUtil.isBlank(url)) {
            return null;
        }
        try {
            URI uri = new URI(url);
            return uri.getPath();
        } catch (URISyntaxException e) {
            log.error("Invalid URL: {}", url, e);
            return null;
        }
    }

    /**
     * 获取远程客户端的IP地址
     *
     * @return 远程客户端的IP地址
     */
    public static String getRemoteAddr() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return StrUtil.EMPTY;
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0]; // 处理多个IP的情况
        }
        ip = request.getHeader("X-Real-IP");
        if (StrUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }
}
