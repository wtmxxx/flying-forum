package com.atcumt.auth.api.client;

import cn.hutool.json.JSONObject;
import com.atcumt.auth.api.client.fallback.PortalClientFallback;
import com.atcumt.common.exception.UnauthorizedException;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(value = "school-portal", url = "http://portal.cumt.edu.cn", fallbackFactory = PortalClientFallback.class)
public interface PortalClient {
    @GetMapping("/portal/api/v2/infoplus/me/profile")
    JSONObject getProfile(@RequestHeader("Cookie") String cookie) throws UnauthorizedException;
}
