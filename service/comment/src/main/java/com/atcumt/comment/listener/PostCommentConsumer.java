package com.atcumt.comment.listener;

import cn.hutool.json.JSONObject;
import com.atcumt.common.mq.AbstractBatchSetConsumer;
import com.atcumt.common.utils.HeatScoreUtil;
import com.atcumt.model.comment.dto.PostCommentCountDTO;
import com.atcumt.model.comment.entity.Comment;
import com.atcumt.model.comment.entity.Reply;
import com.atcumt.model.common.annotation.BatchSetConsumerConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
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
        topic = "comment",
        selectorExpression = "postComment",
        consumerGroup = "post-comment-consumer",
        maxReconsumeTimes = 3
)
@BatchSetConsumerConfig(messageLog = "帖子评论数")
@RequiredArgsConstructor
@Slf4j
public class PostCommentConsumer extends AbstractBatchSetConsumer<PostCommentCountDTO> {
    private final MongoTemplate mongoTemplate;

    @Scheduled(fixedRate = 5010)
    public void scheduledCount() {
        super.scheduledCount();
    }

    @Override
    public void consumeMessage(PostCommentCountDTO post, Map<String, BulkOperations> bulkOpsMap) {
        try {
            // 计算评论数
            int commentCount = (int) mongoTemplate.count(
                    Query.query(Criteria.where("postId").is(post.getPostId())),
                    Comment.class);

            commentCount += (int) mongoTemplate.count(
                    Query.query(Criteria.where("postId").is(post.getPostId())),
                    Reply.class);

            // 查询评论信息
            Query queryPost = Query.query(Criteria.where("_id").is(post.getPostId()));
            queryPost.fields().include("likeCount", "dislikeCount", "commentCount", "score", "createTime");
            JSONObject completePost = mongoTemplate.findOne(queryPost, JSONObject.class, post.getPostType());

            if (completePost == null) {
                log.error("未找到对应的帖子, postType: {}, postId: {}", post.getPostType(), post.getPostId());
                return;
            }

            // 计算评论热度
            double score;
            try {
                score = HeatScoreUtil.getPostHeat(
                        completePost.getInt("likeCount", 0),
                        completePost.getInt("dislikeCount", 0),
                        commentCount
                );
            } catch (Exception e) {
                log.error("计算帖子热度时发生错误, postId: {}", post.getPostId(), e);
                score = completePost.getDouble("score", 0.0);
            }

            // 获取批处理操作
            BulkOperations bulkOps = bulkOpsMap.computeIfAbsent(post.getPostType(), key ->
                    mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, post.getPostType())
            );

            // 根据 postType 执行不同的操作, 假设是更新操作
            bulkOps.updateOne(
                    Query.query(Criteria.where("_id").is(post.getPostId())),
                    new Update().set("commentCount", commentCount).set("score", score)
            );
        } catch (Exception e) {
            log.error("计算帖子评论数时发生错误, postId: {}", post.getPostId(), e);
        }
    }
}
