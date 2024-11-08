package com.atcumt.auth.api.client;

import com.atcumt.auth.api.client.fallback.SchoolClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(value = "school-auth-server", url = "${school.auth-server.url}", fallbackFactory = SchoolClientFallback.class)
public interface SchoolClient {

    @GetMapping("/portal/api/v2/infoplus/me/profile")
    String getProfile(@RequestHeader("Authorization") String token);
}
