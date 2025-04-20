package com.atcumt.common.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfiguration {

    /**
     * 配置 Jackson 的 ObjectMapper，用于全局的 JSON 序列化与反序列化设置。
     *
     * @return 配置好的 ObjectMapper 实例
     */
    @Bean
    public ObjectMapper objectMapper() {
        // 创建 ObjectMapper 实例
        ObjectMapper objectMapper = new ObjectMapper();

        // 注册 JavaTimeModule 使 Jackson 支持 Java 8 日期时间 API（如 LocalDateTime）
        objectMapper.registerModule(new JavaTimeModule());

        // 在反序列化时，忽略 JSON 中存在但 Java 对象中不存在的属性
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 禁用将日期写为时间戳，使用标准的 ISO 8601 格式
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        // 在序列化时，忽略值为 null 的属性
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // 在序列化时，忽略值为默认值的属性（如 0，空字符串等）
        objectMapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_DEFAULT);

        return objectMapper;
    }
}
