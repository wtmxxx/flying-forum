package com.atcumt.comment.listener;

import com.atcumt.common.mq.AbstractBatchSetConsumer;
import com.atcumt.common.utils.HeatScoreUtil;
import com.atcumt.model.comment.dto.ReplyReplyScoreDTO;
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
        selectorExpression = "replyReply",
        consumerGroup = "reply-reply-consumer",
        maxReconsumeTimes = 3
)
@BatchSetConsumerConfig(messageLog = "根回复热度")
@RequiredArgsConstructor
@Slf4j
public class ReplyReplyConsumer extends AbstractBatchSetConsumer<ReplyReplyScoreDTO> {
    private final MongoTemplate mongoTemplate;

    @Scheduled(fixedRate = 5015)
    public void scheduledCount() {
        super.scheduledCount();
    }

    @Override
    public void consumeMessage(ReplyReplyScoreDTO reply, Map<String, BulkOperations> bulkOpsMap) {
        try {
            // 查询根回复信息
            Query queryReply = Query.query(Criteria.where("_id").is(reply.getReplyId()));
            queryReply.fields().include("replyToId", "rootCommentId", "likeCount", "dislikeCount", "score", "createTime");
            Reply completeReply = mongoTemplate.findOne(queryReply, Reply.class);

            if (completeReply == null) {
                log.error("未找到对应的回复, replyId: {}", reply.getReplyId());
                return;
            }

            // 计算根回复热度
            double score;
            try {
                score = HeatScoreUtil.getReplyHeat(
                        completeReply.getLikeCount(),
                        completeReply.getDislikeCount(),
                        completeReply.getCreateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                );
            } catch (Exception e) {
                log.error("计算根回复热度时发生错误，replyId: {}", reply.getReplyId(), e);
                score = completeReply.getScore();
            }

            BulkOperations bulkOps = bulkOpsMap.computeIfAbsent("default", key ->
                    mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, Reply.class)
            );
            bulkOps.updateOne(
                    Query.query(Criteria.where("_id").is(reply.getReplyId())),
                    new Update().set("score", score)
            );
        } catch (Exception e) {
            log.error("计算根回复热度时发生错误，replyId: {}", reply.getReplyId(), e);
        }
    }
}
