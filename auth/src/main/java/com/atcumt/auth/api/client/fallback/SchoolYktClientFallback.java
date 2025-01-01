package com.atcumt.auth.api.client.fallback;

import com.atcumt.auth.api.client.SchoolYktClient;
import com.atcumt.common.exception.UnauthorizedException;
import com.atcumt.model.auth.enums.AuthMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

@Slf4j
public class SchoolYktClientFallback implements FallbackFactory<SchoolYktClient> {
    @Override
    public SchoolYktClient create(Throwable cause) {
        return new SchoolYktClient() {
            @Override
            public String getSchoolCard(String token) throws UnauthorizedException {
                throw new UnauthorizedException(AuthMessage.UNIFIED_AUTH_FAILURE.getMessage());
            }
        };
    }
}
