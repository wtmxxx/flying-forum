package com.atcumt.auth.api.config;

import com.atcumt.auth.api.client.fallback.SchoolClientFallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfiguration {
    @Bean
    SchoolClientFallback userClientFallback() {
        return new SchoolClientFallback();
    }
}
