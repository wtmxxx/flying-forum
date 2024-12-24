package com.atcumt.auth.api.client.fallback;

import cn.hutool.json.JSONObject;
import com.atcumt.auth.api.client.PortalClient;
import com.atcumt.common.exception.UnauthorizedException;
import com.atcumt.model.common.enums.AuthMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

@Slf4j
public class PortalClientFallback implements FallbackFactory<PortalClient> {
    @Override
    public PortalClient create(Throwable cause) {
        return new PortalClient() {
            @Override
            public JSONObject getProfile(String cookie) throws UnauthorizedException {
                throw new UnauthorizedException(AuthMessage.UNIFIED_AUTH_FAILURE.getMessage());
            }
        };
    }
}
