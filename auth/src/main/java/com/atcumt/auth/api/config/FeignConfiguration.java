package com.atcumt.auth.api.config;

import com.atcumt.auth.api.client.fallback.SchoolClientFallback;
import com.atcumt.common.config.CommonFeignConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(CommonFeignConfiguration.class)
public class FeignConfiguration {
    @Bean
    SchoolClientFallback userClientFallback() {
        return new SchoolClientFallback();
    }
}
