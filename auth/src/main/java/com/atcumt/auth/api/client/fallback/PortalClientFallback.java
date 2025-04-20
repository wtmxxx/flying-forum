package com.atcumt.auth.api.client.fallback;

import com.atcumt.auth.api.client.PortalClient;
import com.atcumt.common.exception.UnauthorizedException;
import com.atcumt.model.auth.enums.AuthMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

@Slf4j
public class PortalClientFallback implements FallbackFactory<PortalClient> {
    @Override
    public PortalClient create(Throwable cause) {
        return cookie -> {
            throw new UnauthorizedException(AuthMessage.UNIFIED_AUTH_FAILURE.getMessage());
        };
    }
}
