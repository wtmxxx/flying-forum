package com.atcumt.auth.api.config;

import com.atcumt.auth.api.client.fallback.PortalClientFallback;
import com.atcumt.auth.api.client.fallback.SchoolYktClientFallback;
import com.atcumt.common.config.CommonFeignConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(CommonFeignConfiguration.class)
public class FeignConfiguration {
    @Bean
    SchoolYktClientFallback userClientFallback() {
        return new SchoolYktClientFallback();
    }

    @Bean
    PortalClientFallback portalClientFallback() {
        return new PortalClientFallback();
    }
}
