package com.atcumt.search.listener.mongo.template;

import com.atcumt.model.search.dto.SearchPostDTO;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.redisson.api.RedissonClient;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.MessagingException;

import java.util.List;

@Slf4j
public class PostChangeStreamsListener extends AbstractMongoChangeStreamsListener {
    protected RocketMQTemplate rocketMQTemplate;

    protected PostChangeStreamsListener(
            MongoTemplate mongoTemplate,
            RedisTemplate<String, String> redisStringTemplate,
            RedissonClient redissonClient,
            RocketMQTemplate rocketMQTemplate,
            String postType
    ) {
        super(mongoTemplate, redisStringTemplate, redissonClient, postType);
        this.rocketMQTemplate = rocketMQTemplate;
    }

    @Override
    public void watchMongoChangeStreams() {
        log.info("[{}] 🔍 开始监听MongoDB Change Streams", COLLECTION);

        // 监控创建、更新、删除操作
        List<Bson> pipeline = List.of(Aggregates.match(
                Filters.in("operationType", List.of("insert", "update", "delete"))
        ));

        // 设置监听游标
        super.setCursor(pipeline);

        while (locked && cursor.hasNext()) {
            ChangeStreamDocument<Document> next = getNext();

            log.info("[{}] 🔍 ResumeToken: {}", COLLECTION, next.getResumeToken());
            log.info("[{}] 🔍 MongoDB Change Streams 数据: {}", COLLECTION, next.getDocumentKey());
            log.info("[{}] 🔍 MongoDB Change Streams 类型: {}", COLLECTION, next.getOperationTypeString());
            try {
                if (next.getDocumentKey() != null) {
                    SearchPostDTO searchPostDTO = SearchPostDTO
                            .builder()
                            .postId(next.getDocumentKey().get("_id").asNumber().longValue())
                            .postType(COLLECTION)
                            .build();

                    rocketMQTemplate.asyncSend("search:searchPost", searchPostDTO, new SendCallback() {
                        @Override
                        public void onSuccess(SendResult sendResult) {}

                        @Override
                        public void onException(Throwable throwable) {
                            log.error("[{}] 监控Post变更并发送消息失败, postId: {}", COLLECTION, next.getDocumentKey(), throwable);
                        }
                    });
                }
            } catch (MessagingException e) {
                log.error("[{}] 监控Post变更并发送消息失败, postId: {}", COLLECTION, next.getDocumentKey(), e);
            }
        }
    }
}