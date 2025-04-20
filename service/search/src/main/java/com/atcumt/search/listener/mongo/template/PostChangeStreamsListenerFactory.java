package com.atcumt.search.listener.mongo.template;

import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RedissonClient;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostChangeStreamsListenerFactory {
    private final MongoTemplate mongoTemplate;
    private final RedisTemplate<String, String> redisStringTemplate;
    private final RedissonClient redissonClient;
    private final RocketMQTemplate rocketMQTemplate;

    public PostChangeStreamsListener createListener(String postType) {
        return new PostChangeStreamsListener(
                mongoTemplate,
                redisStringTemplate,
                redissonClient,
                rocketMQTemplate,
                postType
        );
    }
}