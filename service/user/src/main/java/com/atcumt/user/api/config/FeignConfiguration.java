package com.atcumt.user.api.config;

import com.atcumt.common.config.CommonFeignConfiguration;
import com.atcumt.gateway.api.client.fallback.AuthClientFallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(CommonFeignConfiguration.class)
public class FeignConfiguration {

    @Bean
    public AuthClientFallback authClientFallback() {
        return new AuthClientFallback();
    }
}
