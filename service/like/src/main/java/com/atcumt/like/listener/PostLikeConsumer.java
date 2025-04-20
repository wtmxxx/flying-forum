package com.atcumt.like.listener;

import cn.hutool.json.JSONObject;
import com.atcumt.common.mq.AbstractBatchSetConsumer;
import com.atcumt.common.utils.HeatScoreUtil;
import com.atcumt.model.common.annotation.BatchSetConsumerConfig;
import com.atcumt.model.like.constants.LikeAction;
import com.atcumt.model.like.dto.PostLikeCountDTO;
import com.atcumt.model.like.dto.UserLikeReceivedDTO;
import com.atcumt.model.like.entity.PostLike;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RocketMQMessageListener(
        topic = "like",
        selectorExpression = "postLike",
        consumerGroup = "post-like-consumer",
        maxReconsumeTimes = 3
)
@BatchSetConsumerConfig(messageLog = "帖子点赞量")
@RequiredArgsConstructor
@Slf4j
public class PostLikeConsumer extends AbstractBatchSetConsumer<PostLikeCountDTO> {
    private final MongoTemplate mongoTemplate;
    private final RocketMQTemplate rocketMQTemplate;

    @Scheduled(fixedRate = 4060)
    public void scheduledCount() {
        super.scheduledCount();
    }

    @Override
    public void consumeMessage(PostLikeCountDTO post, Map<String, BulkOperations> bulkOpsMap) {
        try {
            // 计算点赞量
            int likeCount = (int) mongoTemplate.count(
                    Query.query(Criteria
                            .where("postType").is(post.getPostType())
                            .and("postId").is(post.getPostId())
                            .and("action").is(LikeAction.LIKE)
                    ), PostLike.class);

            int dislikeCount = (int) mongoTemplate.count(
                    Query.query(Criteria
                            .where("postType").is(post.getPostType())
                            .and("postId").is(post.getPostId())
                            .and("action").is(LikeAction.DISLIKE)
                    ), PostLike.class);

            // 查询评论信息
            Query postQuery = Query.query(Criteria.where("_id").is(post.getPostId()));
            postQuery.fields().include("userId", "commentCount", "score", "createTime");
            JSONObject completePost = mongoTemplate.findOne(postQuery, JSONObject.class, post.getPostType());

            if (completePost == null) {
                log.error("未找到对应的帖子，postType: {}, postId: {}", post.getPostType(), post.getPostId());
                return;
            }

            // 计算评论热度
            double score;
            try {
                score = HeatScoreUtil.getPostHeat(
                        likeCount,
                        dislikeCount,
                        completePost.getInt("commentCount")
                );
            } catch (Exception e) {
                log.error("计算帖子热度时发生错误，postId: {}", post.getPostId(), e);
                score = completePost.getDouble("score", 0.0);
            }

            // 获取批处理操作
            BulkOperations bulkOps = bulkOpsMap.computeIfAbsent(post.getPostType(), key ->
                    mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, post.getPostType())
            );

            // 根据 postType 执行不同的操作，假设是更新操作
            bulkOps.updateOne(
                    Query.query(Criteria.where("_id").is(post.getPostId())),
                    new Update()
                            .set("likeCount", likeCount)
                            .set("dislikeCount", dislikeCount)
                            .set("score", score)
            );

            rocketMQTemplate.asyncSend("like:userLikeReceived", new UserLikeReceivedDTO(completePost.get("userId").toString()), new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                }

                @Override
                public void onException(Throwable e) {
                    log.error("用户获赞消息发送失败e: {}", e.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("计算帖子点赞量时发生错误，post: {}", post, e);
        }
    }
}
