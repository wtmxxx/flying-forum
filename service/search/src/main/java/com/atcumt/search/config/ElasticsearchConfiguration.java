package com.atcumt.search.config;

import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
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
public class ElasticsearchConfiguration {

    @Bean
    JsonpMapper jsonpMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 自定义 LocalDateTime 序列化和反序列化
        SimpleModule module = new SimpleModule();
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer());
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer());

        objectMapper.registerModule(module);

        return new JacksonJsonpMapper(objectMapper);
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

    // UTC String (2025-02-11T12:00:00.000Z) -> LocalDateTime (本地时区)
    static class LocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
        @Override
        public LocalDateTime deserialize(com.fasterxml.jackson.core.JsonParser p, DeserializationContext ctxt) throws IOException {
            ZonedDateTime utcDateTime = ZonedDateTime.parse(p.getText());
            return utcDateTime.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        }
    }
}
