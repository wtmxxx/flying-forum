package com.atcumt.comment.listener;

import cn.hutool.core.collection.ConcurrentHashSet;
import com.atcumt.common.utils.HeatScoreUtil;
import com.atcumt.model.comment.dto.CommentReplyCountDTO;
import com.atcumt.model.comment.entity.Comment;
import com.atcumt.model.comment.entity.Reply;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@RocketMQMessageListener(
        topic = "comment",
        selectorExpression = "commentReply",
        consumerGroup = "comment-reply-consumer",
        maxReconsumeTimes = 3
)
@RequiredArgsConstructor
@Slf4j
public class CommentReplyConsumer implements RocketMQListener<CommentReplyCountDTO> {

    // 消息堆积触发阈值
    private static final int BATCH_SIZE = 20;
    private final MongoTemplate mongoTemplate;
    private final ConcurrentHashSet<CommentReplyCountDTO> replyCounts = new ConcurrentHashSet<>();

    @Override
    public void onMessage(CommentReplyCountDTO commentReplyCountDTO) {
        // 缓存消息
        replyCounts.add(commentReplyCountDTO);

        // 如果消息堆积到 BATCH_SIZE，触发批量消费
        if (replyCounts.size() >= BATCH_SIZE) {
            log.info("消息堆积达到阈值，触发批量消费...");
            batchCount();
        }
    }

    @Scheduled(fixedRate = 5000)
    public void scheduledCount() {
        if (!replyCounts.isEmpty()) {
            log.info("定时任务触发批量消费...");
            batchCount();
        }
    }

    // 批量消费逻辑
    private void batchCount() {
        log.info("正在批量计算回复数...");

        List<CommentReplyCountDTO> comments = null;

        // 消费消息时加锁，避免多线程同时消费
        synchronized (replyCounts) {
            comments = replyCounts.stream().toList();

            // 清空消息
            replyCounts.clear();
        }

        // 创建虚拟线程池
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        // 创建批处理操作
        BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, Comment.class);
        // 批量处理消息
        for (CommentReplyCountDTO comment : comments) {
            executor.submit(() -> {
                try {
                    log.info("计算评论回复数，comment: {}", comment);

                    long start = System.currentTimeMillis();

                    // 计算评论数
                    int replyCount = (int) mongoTemplate.count(
                            Query.query(Criteria.where("rootCommentId").is(comment.getCommentId())),
                            Reply.class);

                    // 查询评论信息
                    Query queryComment = Query.query(Criteria.where("_id").is(comment.getCommentId()));
                    queryComment.fields().include("likeCount", "dislikeCount", "score", "createTime");
                    Comment completeComment = mongoTemplate.findOne(queryComment, Comment.class);

                    // 计算评论热度
                    double score;
                    try {
                        log.info("计算评论热度");
                        score = HeatScoreUtil.getCommentHeat(
                                completeComment.getLikeCount(),
                                completeComment.getDislikeCount(),
                                replyCount,
                                completeComment.getCreateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        );
                    } catch (Exception e) {
                        log.error("计算评论热度时发生错误，commentId: {}", comment.getCommentId(), e);
                        score = completeComment.getScore();
                    }

                    // 根据 postType 执行不同的操作，假设是更新操作
                    bulkOps.updateOne(
                            Query.query(Criteria.where("_id").is(comment.getCommentId())),
                            new Update().set("replyCount", replyCount).set("score", score)
                    );

                    long end = System.currentTimeMillis();

                    log.info("计算回复数耗时：{}ms", end - start);
                } catch (Exception e) {
                    log.error("计算评论回复数时发生错误，commentId: {}", comment.getCommentId(), e);
                }
            });
        }

        // 等待所有任务完成
        try {
            // 关闭线程池之前等待任务完成
            executor.shutdown();
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                log.warn("任务未能在指定时间内完成，强制关闭线程池");
                executor.shutdownNow(); // 超时后强制关闭
            }
        } catch (InterruptedException e) {
            log.error("等待线程池关闭时发生异常", e);
            executor.shutdownNow(); // 中断时强制关闭线程池
        }

        long start = System.currentTimeMillis();

        // 执行所有批量操作
        try {
            bulkOps.execute();
        } catch (Exception e) {
            log.error("批量更新评论回复数时发生错误", e);
        }

        long end = System.currentTimeMillis();

        log.info("批量更新评论回复数耗时：{}ms", end - start);
    }
}
