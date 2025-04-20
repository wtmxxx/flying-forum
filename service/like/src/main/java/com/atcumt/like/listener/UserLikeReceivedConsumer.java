package com.atcumt.like.listener;

import cn.hutool.json.JSONObject;
import com.atcumt.common.mq.AbstractBatchSetConsumer;
import com.atcumt.model.common.annotation.BatchSetConsumerConfig;
import com.atcumt.model.post.enums.PostType;
import com.atcumt.model.like.dto.UserLikeReceivedDTO;
import com.atcumt.model.user.entity.UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOptions;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RocketMQMessageListener(
        topic = "like",
        selectorExpression = "userLikeReceived",
        consumerGroup = "user-like-received-consumer",
        maxReconsumeTimes = 3
)
@BatchSetConsumerConfig(batchSize = 10, messageLog = "用户获赞量")
@RequiredArgsConstructor
@Slf4j
public class UserLikeReceivedConsumer extends AbstractBatchSetConsumer<UserLikeReceivedDTO> {
    private final MongoTemplate mongoTemplate;

    @Scheduled(fixedRate = 6060)
    public void scheduledCount() {
        super.scheduledCount();
    }

    @Override
    public void consumeMessage(UserLikeReceivedDTO user, Map<String, BulkOperations> bulkOpsMap) {
        try {
            String userId = user.getUserId();
            BulkOperations bulkOps = bulkOpsMap.computeIfAbsent("default", key ->
                    mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, UserInfo.class)
            );

            Query userInfoQuery = new Query(Criteria.where("_id").is(userId));
            userInfoQuery.fields().include("likeReceivedCount");
            UserInfo userInfo = mongoTemplate.findOne(userInfoQuery, UserInfo.class);
            if (userInfo == null) return;
            // 如果用户获赞量已经达到 1 亿，不再累加
            if (userInfo.getLikeReceivedCount() == 100000000) {
                return;
            } else if (userInfo.getLikeReceivedCount() > 100000000) {
                bulkOps.updateOne(userInfoQuery, Update.update("likeReceivedCount", 100000000));
                return;
            }

            int totalLikeCount = 0;

            for (var collection : PostType.values()) {
                // 跳过新闻计数
                if (collection.equals(PostType.NEWS)) continue;

                var collectionName = collection.getValue();
                // 聚合管道
                AggregationOptions options = AggregationOptions.builder()
                        .allowDiskUse(true) // 允许使用磁盘
                        .build();
                Aggregation aggregation = Aggregation.newAggregation(
                        Aggregation.match(Criteria.where("userId").is(userId)),
                        Aggregation.group().sum("likeCount").as("totalLikeCount")
                ).withOptions(options);

                // 执行聚合查询
                AggregationResults<JSONObject> result = mongoTemplate.aggregate(aggregation, collectionName, JSONObject.class);

                if (result.getUniqueMappedResult() == null) continue;

                // 累加当前集合的总和
                totalLikeCount += result.getUniqueMappedResult().getInt("totalLikeCount", 0);

                userInfoQuery = new Query(Criteria.where("_id").is(userId));

                bulkOps.updateOne(userInfoQuery, Update.update("likeReceivedCount", totalLikeCount));
            }
        } catch (Exception e) {
            log.error("更新用户获赞量时发生错误", e);
        }
    }
}
