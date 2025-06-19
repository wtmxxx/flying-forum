package com.atcumt.ai.config;

import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchConfiguration {
    @Bean
    JsonpMapper jsonpMapper(ObjectMapper objectMapper) {
        return new JacksonJsonpMapper(objectMapper);
    }
}