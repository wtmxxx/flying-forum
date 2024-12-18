package com.atcumt.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@ConditionalOnMissingBean(WebClient.class)
public class WebClientConfiguration {
    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        // 配置默认的缓冲大小，适用于较大的响应体
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(16 * 1024 * 1024) // 设置最大内存缓冲为 16 MB
                ).build();

        return builder
                .exchangeStrategies(exchangeStrategies) // 配置缓冲策略
                .build();
    }
}