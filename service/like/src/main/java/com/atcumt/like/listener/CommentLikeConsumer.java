package com.atcumt.like.listener;

import com.atcumt.common.mq.AbstractBatchSetConsumer;
import com.atcumt.common.utils.HeatScoreUtil;
import com.atcumt.model.comment.entity.Comment;
import com.atcumt.model.comment.entity.Reply;
import com.atcumt.model.common.annotation.BatchSetConsumerConfig;
import com.atcumt.model.like.constants.LikeAction;
import com.atcumt.model.like.dto.CommentLikeCountDTO;
import com.atcumt.model.like.entity.CommentLike;
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
        topic = "like",
        selectorExpression = "commentLike",
        consumerGroup = "comment-like-consumer",
        maxReconsumeTimes = 3
)
@BatchSetConsumerConfig(messageLog = "帖子点赞量")
@RequiredArgsConstructor
@Slf4j
public class CommentLikeConsumer extends AbstractBatchSetConsumer<CommentLikeCountDTO> {
    private final MongoTemplate mongoTemplate;

    @Scheduled(fixedRate = 4090)
    public void scheduledCount() {
        super.scheduledCount();
    }

    @Override
    public void consumeMessage(CommentLikeCountDTO comment, Map<String, BulkOperations> bulkOpsMap) {
        try {
            // 计算点赞量
            int likeCount = (int) mongoTemplate.count(
                    Query.query(Criteria
                            .where("commentType").is(comment.getCommentType())
                            .and("commentId").is(comment.getCommentId())
                            .and("action").is(LikeAction.LIKE)
                    ), CommentLike.class);
            // 计算点踩量
            int dislikeCount = (int) mongoTemplate.count(
                    Query.query(Criteria
                            .where("commentType").is(comment.getCommentType())
                            .and("commentId").is(comment.getCommentId())
                            .and("action").is(LikeAction.DISLIKE)
                    ), CommentLike.class);

            // 查询评论信息
            Query queryPost = Query.query(Criteria.where("_id").is(comment.getCommentId()));
            Comment completeComment = null;
            Reply completeReply = null;
            if (comment.getCommentType().equals("comment")) {
                queryPost.fields().include("replyCount", "score", "createTime");
                completeComment = mongoTemplate.findOne(queryPost, Comment.class, comment.getCommentType());
            } else {
                queryPost.fields().include("replyToId", "rootCommentId", "score", "createTime");
                completeReply = mongoTemplate.findOne(queryPost, Reply.class, comment.getCommentType());
            }

            // 计算评论热度
            double score;
            try {
                if (comment.getCommentType().equals("comment")) {
                    if (completeComment == null) {
                        log.error("未找到对应的评论, commentId: {}", comment.getCommentId());
                        return;
                    }

                    score = HeatScoreUtil.getCommentHeat(
                            likeCount,
                            dislikeCount,
                            completeComment.getReplyCount(),
                            completeComment.getCreateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    );
                } else if (comment.getCommentType().equals("reply")) {
                    if (completeReply == null) {
                        log.error("未找到对应的回复, replyId: {}", comment.getCommentId());
                        return;
                    }

                    if (!(completeReply.getReplyToId().equals(completeReply.getRootCommentId()))) {
                        // 非根回复
                        return;
                    }

                    score = HeatScoreUtil.getReplyHeat(
                            likeCount,
                            dislikeCount,
                            completeReply.getCreateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    );
                } else {
                    throw new IllegalArgumentException("不支持的评论类型");
                }
            } catch (Exception e) {
                log.error("计算评论热度时发生错误，commentId: {}", comment.getCommentId(), e);
                return;
            }

            // 获取批处理操作
            BulkOperations bulkOps = bulkOpsMap.computeIfAbsent(comment.getCommentType(), key ->
                    mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, comment.getCommentType())
            );

            // 根据 postType 执行不同的操作，假设是更新操作
            bulkOps.updateOne(
                    Query.query(Criteria.where("_id").is(comment.getCommentId())),
                    new Update()
                            .set("likeCount", likeCount)
                            .set("dislikeCount", dislikeCount)
                            .set("score", score)
            );
        } catch (Exception e) {
            log.error("计算评论点赞量时发生错误，postId: {}", comment.getCommentId(), e);
        }
    }
}
