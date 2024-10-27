package com.atcumt.gpt.config;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Configuration
public class MyBatisPlusConfiguration {

    @Bean
    public IdentifierGenerator idGenerator() {
        return new IdentifierGenerator() {
            @Override
            public Number nextId(Object entity) {
                return IdWorker.getId();
            }

            @Override
            public String nextUUID(Object entity) {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                return (new UUID(random.nextLong(), random.nextLong())).toString();
            }
        };
    }
}