package com.atcumt.auth.utils;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.atcumt.common.utils.WebUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
@RequiredArgsConstructor
public class IpUtil {
    private final BaiduIp2Region baiduIp2Region;
    private final WebClient webClient;

    public static boolean isValidIp(String ip) {
        try {
            InetAddress inetAddress = InetAddress.getByName(ip);
            if (inetAddress instanceof java.net.Inet4Address) {
                return true;
            } else if (inetAddress instanceof java.net.Inet6Address) {
                return true;
            }
        } catch (UnknownHostException e) {
            return false;
        }

        return false;
    }

    public String convertToIpv4(String ip) {
        try {
            InetAddress inetAddress = InetAddress.getByName(ip);
            if (inetAddress instanceof java.net.Inet6Address) {
                byte[] ipv4Bytes = inetAddress.getAddress();
                return (ipv4Bytes[12] & 0xFF) + "." +
                        (ipv4Bytes[13] & 0xFF) + "." +
                        (ipv4Bytes[14] & 0xFF) + "." +
                        (ipv4Bytes[15] & 0xFF);
            }
            return ip; // 已经是 IPv4
        } catch (UnknownHostException e) {
            return null; // 非法地址直接返回
        }
    }

    public String getRemoteAddr() {
        HttpServletRequest request = WebUtil.getRequest();
        if (request == null) {
            return "";
        }

        String ip = null;
        String[] headers = new String[]{
                "x-forwarded-for", "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP",
                "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"
        };

        for (String header : headers) {
            ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                ip = ip.split(",")[0].trim(); // 多个 IP 的情况下取第一个
                if (isValidIp(ip)) { // 验证 IP 格式
//                    System.out.println("ip: " + ip);
                    return ip;
                }
            }
        }

        ip = request.getRemoteAddr();
        if (isValidIp(ip)) { // 验证最终 IP 格式
            return ip;
        }

        return null; // 如果都不合法，返回空字符串
    }

    public String getRegionByIp(String ip) {
        if (ip.equals("127.0.0.1")) return "本地局域网";

        String region = ip2RegionByUserAgentInfo(ip);

        if (region == null) {
            region = ip2RegionByOpenDataBaidu(ip);
        }

        if (region == null) {
            region = ip2RegionByBaidu(ip);
        }

        // 未知地区置空
        if (region.isEmpty()) region = null;
        else region = region.trim();
        return region;
    }

    public String ip2RegionByUserAgentInfo(String ip) {
        // https://ip.useragentinfo.com/jsonp?callback=json&ip=127.0.0.1
        try {
            ip = convertToIpv4(ip);
            if (ip == null || ip.isEmpty()) return null;
            String finalIp = ip;
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("ip.useragentinfo.com")
                            .path("/jsonp")
                            .queryParam("callback", "json")
                            .queryParam("ip", finalIp)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response == null || response.isEmpty()) {
                return null;
            }

            response = response.substring(response.indexOf("(") + 1, response.lastIndexOf(")"));
            JSONObject jsonObject = JSONUtil.parseObj(response);

            if (jsonObject.get("code").equals(200)) {
                String country = jsonObject.get("country").toString();
                String province = jsonObject.get("province").toString();
                String city = jsonObject.get("city").toString();
                String isp = jsonObject.get("isp").toString();

                if ("保留地址".equals(country)) {
                    return "";
                }

                return country + province + city + " " + isp;
            }
        } catch (Exception e) {
            return null;
        }

        return null;
    }

    public String ip2RegionByOpenDataBaidu(String ip) {
        // https://opendata.baidu.com/api.php?query=127.0.0.1&resource_id=6006&oe=utf8
        try {
            ip = convertToIpv4(ip);
            if (ip == null || ip.isEmpty()) return null;
            String finalIp = ip;
            JSONObject response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("opendata.baidu.com")
                            .path("/api.php")
                            .queryParam("query", finalIp)
                            .queryParam("resource_id", "6006")
                            .queryParam("oe", "utf8")
                            .build())
                    .retrieve()
                    .bodyToMono(JSONObject.class)
                    .block();

            if (response == null || response.isEmpty() || !response.get("status").equals("0")) {
                return null;
            }

            String region = response.getByPath("data[0].location", String.class);

            if (region.contains("移动") || region.contains("联通") || region.contains("电信")) {
                return "中国" + region;
            }

            return region;
        } catch (Exception e) {
            return null;
        }
    }

    public String ip2RegionByBaidu(String ip) {
        ip = convertToIpv4(ip);
        if (ip == null || ip.isEmpty()) return null;
        return baiduIp2Region.getRegion(ip);
    }
}
