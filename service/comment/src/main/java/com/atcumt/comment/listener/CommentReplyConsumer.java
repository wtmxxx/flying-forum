package com.atcumt.comment.listener;

import com.atcumt.common.mq.AbstractBatchSetConsumer;
import com.atcumt.common.utils.HeatScoreUtil;
import com.atcumt.model.comment.dto.CommentReplyCountDTO;
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

import java.time.ZoneId;
import java.util.Map;

@Service
@RocketMQMessageListener(
        topic = "comment",
        selectorExpression = "commentReply",
        consumerGroup = "comment-reply-consumer",
        maxReconsumeTimes = 3
)
@BatchSetConsumerConfig(messageLog = "评论回复数")
@RequiredArgsConstructor
@Slf4j
public class CommentReplyConsumer extends AbstractBatchSetConsumer<CommentReplyCountDTO> {
    private final MongoTemplate mongoTemplate;

    @Scheduled(fixedRate = 5005)
    public void scheduledCount() {
        super.scheduledCount();
    }

    @Override
    public void consumeMessage(CommentReplyCountDTO comment, Map<String, BulkOperations> bulkOpsMap) {
        try {
            // 计算评论数
            int replyCount = (int) mongoTemplate.count(
                    Query.query(Criteria.where("rootCommentId").is(comment.getCommentId())),
                    Reply.class);

            // 查询评论信息
            Query queryComment = Query.query(Criteria.where("_id").is(comment.getCommentId()));
            queryComment.fields().include("likeCount", "dislikeCount", "score", "createTime");
            Comment completeComment = mongoTemplate.findOne(queryComment, Comment.class);

            if (completeComment == null) {
                log.error("未找到对应的评论, commentId: {}", comment.getCommentId());
                return;
            }
            
            // 计算评论热度
            double score;
            try {
                score = HeatScoreUtil.getCommentHeat(
                        completeComment.getLikeCount(),
                        completeComment.getDislikeCount(),
                        replyCount,
                        completeComment.getCreateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                );
            } catch (Exception e) {
                log.error("计算评论热度时发生错误, commentId: {}", comment.getCommentId(), e);
                score = completeComment.getScore();
            }

            BulkOperations bulkOps = bulkOpsMap.computeIfAbsent("default", key ->
                    mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, Comment.class)
            );
            bulkOps.updateOne(
                    Query.query(Criteria.where("_id").is(comment.getCommentId())),
                    new Update().set("replyCount", replyCount).set("score", score)
            );
        } catch (Exception e) {
            log.error("计算评论回复数时发生错误, commentId: {}", comment.getCommentId(), e);
        }
    }
}
