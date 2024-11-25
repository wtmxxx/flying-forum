package com.atcumt.gpt.api.config;

import com.atcumt.gpt.api.client.fallback.UserClientFallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfiguration {
    @Bean
    UserClientFallback userClientFallback() {
        return new UserClientFallback();
    }
}
