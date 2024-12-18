package com.atcumt.auth.utils;

import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class BaiduIp2Region {

    private final WebClient webClient;
    @Value("${baidu.ip.ak}")
    public String AK;
    @Value("${baidu.ip.sk}")
    public String SK;
    @Value("${baidu.ip.url}")
    public String URL;

    public String getRegion(String ip) {
        return getRegion(ip, URL, AK, SK);
    }

    public String getRegion(String ip, String url, String ak, String sk) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("ip", ip);
        params.put("coor", "bd09ll");
        params.put("ak", ak);
        params.put("sn", this.calculateSn(ip, ak, sk));

        return this.requestGetSN(url, params);
    }

    public String requestGetSN(String strUrl, Map<String, String> param) {
        try {
            if (strUrl == null || strUrl.isEmpty() || param == null || param.isEmpty()) {
                return null;
            }

            StringBuilder queryString = new StringBuilder(strUrl);
            for (Map.Entry<?, ?> pair : param.entrySet()) {
                queryString.append(pair.getKey()).append("=");
                queryString.append(UriUtils.encode((String) pair.getValue(), "UTF-8"))
                        .append("&");
            }

            if (!queryString.isEmpty()) {
                queryString.deleteCharAt(queryString.length() - 1);
            }

            JSONObject response = webClient.get()
                    .uri(queryString.toString())
                    .retrieve()
                    .bodyToMono(JSONObject.class)
                    .block();

            // Parse response and check status
            if (response == null || !response.containsKey("status")) {
                return null;
            }

            int status = (int) response.get("status");
            if (status != 0) {
                return null;
            }

            if (response.get("address", String.class).contains("CN")) {
                return "中国" + response.getByPath("content.address", String.class);
            }

            return response.getByPath("content.address", String.class);

        } catch (Exception e) {
            return null;
        }
    }

    public String calculateSn(String ip, String ak, String sk) {

        Map<String, String> paramsMap = new LinkedHashMap<>();
        paramsMap.put("ip", ip);
        paramsMap.put("coor", "bd09ll");
        paramsMap.put("ak", ak);

        String paramsStr = this.toQueryString(paramsMap);
        String wholeStr = "/location/ip?" + paramsStr + sk;

        String tempStr = URLEncoder.encode(wholeStr, StandardCharsets.UTF_8);

        return this.MD5(tempStr);
    }

    public String toQueryString(Map<?, ?> data) {
        StringBuilder queryString = new StringBuilder();
        for (Map.Entry<?, ?> pair : data.entrySet()) {
            queryString.append(pair.getKey()).append("=");
            queryString.append(UriUtils.encode((String) pair.getValue(), "UTF-8")).append("&");
        }
        if (!queryString.isEmpty()) {
            queryString.deleteCharAt(queryString.length() - 1);
        }
        return queryString.toString();
    }

    public String MD5(String md5) {
        java.security.MessageDigest md = null;
        try {
            md = java.security.MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
        byte[] array = md.digest(md5.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : array) {
            sb.append(Integer.toHexString((b & 0xFF) | 0x100), 1, 3);
        }
        return sb.toString();
    }
}
