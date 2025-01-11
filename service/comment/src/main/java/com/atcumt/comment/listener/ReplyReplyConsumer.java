package com.atcumt.comment.listener;

import cn.hutool.core.collection.ConcurrentHashSet;
import com.atcumt.common.utils.HeatScoreUtil;
import com.atcumt.model.comment.dto.ReplyReplyScoreDTO;
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
        selectorExpression = "replyReply",
        consumerGroup = "reply-reply-consumer",
        maxReconsumeTimes = 3
)
@RequiredArgsConstructor
@Slf4j
public class ReplyReplyConsumer implements RocketMQListener<ReplyReplyScoreDTO> {

    // 消息堆积触发阈值
    private static final int BATCH_SIZE = 20;
    private final MongoTemplate mongoTemplate;
    private final ConcurrentHashSet<ReplyReplyScoreDTO> replyRoots = new ConcurrentHashSet<>();

    @Override
    public void onMessage(ReplyReplyScoreDTO replyReplyScoreDTO) {
        // 缓存消息
        replyRoots.add(replyReplyScoreDTO);

        // 如果消息堆积到 BATCH_SIZE，触发批量消费
        if (replyRoots.size() >= BATCH_SIZE) {
            log.info("消息堆积达到阈值，触发批量消费...");
            batchScore();
        }
    }

    @Scheduled(fixedRate = 5000)
    public void scheduledCount() {
        if (!replyRoots.isEmpty()) {
            log.info("定时任务触发批量消费...");
            batchScore();
        }
    }

    // 批量消费逻辑
    private void batchScore() {
        log.info("正在批量计算根回复热度...");

        List<ReplyReplyScoreDTO> replies;

        // 消费消息时加锁，避免多线程同时消费
        synchronized (replyRoots) {
            replies = replyRoots.stream().toList();

            // 清空消息
            replyRoots.clear();
        }

        // 创建虚拟线程池
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        // 创建批处理操作
        BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, Comment.class);
        // 批量处理消息
        for (ReplyReplyScoreDTO reply : replies) {
            executor.submit(() -> {
                try {
                    log.info("计算根回复热度，reply: {}", reply);

                    long start = System.currentTimeMillis();

                    // 查询根回复信息
                    Query queryReply = Query.query(Criteria.where("_id").is(reply.getReplyId()));
                    queryReply.fields().include("replyToId", "rootCommentId", "likeCount", "dislikeCount", "score", "createTime");
                    Reply completeReply = mongoTemplate.findOne(queryReply, Reply.class);

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

                    // 根据 postType 执行不同的操作，假设是更新操作
                    bulkOps.updateOne(
                            Query.query(Criteria.where("_id").is(reply.getReplyId())),
                            new Update().set("score", score)
                    );

                    long end = System.currentTimeMillis();

                    log.info("计算根回复热度耗时：{}ms", end - start);
                } catch (Exception e) {
                    log.error("计算根回复热度时发生错误，replyId: {}", reply.getReplyId(), e);
                }
            });
        }

        // 等待所有任务完成
        shutdownExecutor(executor);

        long start = System.currentTimeMillis();

        // 执行所有批量操作
        try {
            bulkOps.execute();
        } catch (Exception e) {
            log.error("批量更新评论回复数时发生错误", e);
        }

        long end = System.currentTimeMillis();

        log.info("批量更新根回复热度耗时：{}ms", end - start);
    }

    private void shutdownExecutor(ExecutorService executor) {
        // 等待所有任务完成
        try {
            // 关闭线程池之前等待任务完成
            executor.shutdown();
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("任务未能在指定时间内完成，强制关闭线程池");
                executor.shutdownNow(); // 超时后强制关闭
            }
        } catch (InterruptedException e) {
            log.error("等待线程池关闭时发生异常", e);
            executor.shutdownNow(); // 中断时强制关闭线程池
        }
    }
}
