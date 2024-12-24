package com.atcumt.auth.api.client;

import com.atcumt.auth.api.client.fallback.SchoolYktClientFallback;
import com.atcumt.common.exception.UnauthorizedException;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(value = "school-ykt", url = "https://yktm.cumt.edu.cn", fallbackFactory = SchoolYktClientFallback.class)
public interface SchoolYktClient {

    @GetMapping("/berserker-app/ykt/tsm/getCampusCards?synAccessSource=pc")
    String getSchoolCard(@RequestHeader("Synjones-Auth") String token) throws UnauthorizedException;
}
