package com.atcumt.gpt.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfiguration {
    @Value("${cumt-gpt.gpt.uri}")
    private String gptUri;
    @Value("${cumt-gpt.gpt.port}")
    private String gptPort;

    @Bean
    public WebClient webClient() {
        return WebClient.builder().baseUrl(gptUri + ":" + gptPort).build();
    }
}
