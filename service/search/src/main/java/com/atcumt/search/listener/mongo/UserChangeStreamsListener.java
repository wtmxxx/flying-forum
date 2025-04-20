package com.atcumt.search.listener.mongo;

import com.atcumt.model.search.dto.SearchUserDTO;
import com.atcumt.search.listener.mongo.template.AbstractMongoChangeStreamsListener;
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
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class UserChangeStreamsListener extends AbstractMongoChangeStreamsListener {
    protected RocketMQTemplate rocketMQTemplate;

    protected UserChangeStreamsListener(
            MongoTemplate mongoTemplate,
            RedisTemplate<String, String> redisStringTemplate,
            RedissonClient redissonClient,
            RocketMQTemplate rocketMQTemplate
    ) {
        super(mongoTemplate, redisStringTemplate, redissonClient, "user_info");
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

            try {
                if (next.getDocumentKey() != null) {
                    SearchUserDTO searchUserDTO = SearchUserDTO
                            .builder()
                            .userId(next.getDocumentKey().get("_id").asString().getValue())
                            .isUserAuth(false)
                            .build();

                    rocketMQTemplate.asyncSend("search:searchUser", searchUserDTO, new SendCallback() {
                        @Override
                        public void onSuccess(SendResult sendResult) {}

                        @Override
                        public void onException(Throwable throwable) {
                            log.error("[{}] 监控User变更并发送消息失败, postId: {}", COLLECTION, next.getDocumentKey(), throwable);
                        }
                    });
                }
            } catch (MessagingException e) {
                log.error("[{}] 监控User变更并发送消息失败, postId: {}", COLLECTION, next.getDocumentKey(), e);
            }
        }
    }
}