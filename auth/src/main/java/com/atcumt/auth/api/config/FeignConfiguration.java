package com.atcumt.auth.api.config;

import com.atcumt.auth.api.client.fallback.SchoolClientFallback;
import feign.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfiguration {
    @Bean
    public Logger.Level feignLogLevel() {
        return Logger.Level.FULL;
    }

    @Bean
    SchoolClientFallback userClientFallback() {
        return new SchoolClientFallback();
    }
}
