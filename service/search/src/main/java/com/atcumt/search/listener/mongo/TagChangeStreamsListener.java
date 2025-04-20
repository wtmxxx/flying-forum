package com.atcumt.search.listener.mongo;

import com.atcumt.model.search.dto.SearchSuggestionDTO;
import com.atcumt.model.search.enums.SuggestionAction;
import com.atcumt.model.search.enums.SuggestionType;
import com.atcumt.search.listener.mongo.template.AbstractMongoChangeStreamsListener;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.MessagingException;

import java.util.List;

@Slf4j
//@Component
public class TagChangeStreamsListener extends AbstractMongoChangeStreamsListener {
    private final RocketMQTemplate rocketMQTemplate;

    @Autowired
    TagChangeStreamsListener(
            MongoTemplate mongoTemplate,
            RedisTemplate<String, String> redisStringTemplate,
            RedissonClient redissonClient,
            RocketMQTemplate rocketMQTemplate
    ) {
        super(mongoTemplate, redisStringTemplate, redissonClient, "tag");
        this.rocketMQTemplate = rocketMQTemplate;
    }

    @Override
    public void watchMongoChangeStreams() {
        log.info("[{}] 🔍 开始监听MongoDB Change Streams", COLLECTION);

        // 监控创建和删除操作
        List<Bson> pipeline = List.of(Aggregates.match(
                Filters.in("operationType", List.of("insert"))
        ));

        // 设置监听游标
        setCursor(pipeline);

        while (locked && cursor.hasNext()) {
            ChangeStreamDocument<Document> next = getNext();

//            log.info("[{}] 🔍 ResumeToken: {}", COLLECTION, next.getResumeToken());
//            log.info("[{}] 🔍 MongoDB Change Streams 数据: {}", COLLECTION, next.getDocumentKey());
            log.info("[{}] 🔍 MongoDB Change Streams 类型: {}", COLLECTION, next.getOperationTypeString());
            try {
                if (next.getFullDocument() != null) {
                    SearchSuggestionDTO searchSuggestionDTO = SearchSuggestionDTO
                            .builder()
                            .action(SuggestionAction.TAG)
                            .suggestion(next.getFullDocument().getString("tagName"))
                            .type(SuggestionType.TAG.getValue())
                            .build();
//                    rocketMQTemplate.convertAndSend("search:searchSuggestion", searchSuggestionDTO);
                } else {
                    log.warn("[{}] 🔍 MongoDB Change Streams 数据为空: {}", COLLECTION, next.getDocumentKey());
                }
            } catch (MessagingException e) {
                log.error("监控Tag变更并发送消息失败, tagId: {}", next.getDocumentKey(), e);
            }
        }
    }
}