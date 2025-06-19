package com.atcumt.common.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;

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

        // 自定义 LocalDateTime 和 LocalDate 的序列化/反序列化
        SimpleModule module = new SimpleModule();
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer());
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer());
        module.addSerializer(LocalDate.class, new LocalDateSerializer());
        module.addDeserializer(LocalDate.class, new LocalDateDeserializer());

        return objectMapper;
    }

    // LocalDateTime -> UTC String (2025-02-11T12:00:00.000Z)
    static class LocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {
        private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .withZone(ZoneOffset.UTC);

        @Override
        public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            ZonedDateTime utcDateTime = value.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC);
            gen.writeString(formatter.format(utcDateTime));
        }
    }

    // UTC String -> LocalDateTime (本地时区)
    static class LocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
        @Override
        public LocalDateTime deserialize(com.fasterxml.jackson.core.JsonParser p, DeserializationContext ctxt) throws IOException {
            ZonedDateTime utcDateTime = ZonedDateTime.parse(p.getText());
            return utcDateTime.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        }
    }

    // LocalDate -> String (2025-02-11)
    static class LocalDateSerializer extends JsonSerializer<LocalDate> {
        private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        @Override
        public void serialize(LocalDate value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(formatter.format(value));
        }
    }

    // String (2025-02-11) -> LocalDate
    static class LocalDateDeserializer extends JsonDeserializer<LocalDate> {
        private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        @Override
        public LocalDate deserialize(com.fasterxml.jackson.core.JsonParser p, DeserializationContext ctxt) throws IOException {
            return LocalDate.parse(p.getText(), formatter);
        }
    }
}
