package com.atcumt.auth.api.client.fallback;

import com.atcumt.auth.api.client.SchoolClient;
import com.atcumt.common.exception.UnauthorizedException;
import com.atcumt.model.common.AuthMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

@Slf4j
public class SchoolClientFallback implements FallbackFactory<SchoolClient> {
    @Override
    public SchoolClient create(Throwable cause) {
        return new SchoolClient() {
            @Override
            public String getSchoolCard(String token) {
                throw new UnauthorizedException(AuthMessage.UNIFIED_AUTH_FAILURE.getMessage(), cause);
            }
        };
    }
}
