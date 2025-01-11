package com.atcumt.like.listener;

import cn.hutool.core.collection.ConcurrentHashSet;
import com.atcumt.common.utils.HeatScoreUtil;
import com.atcumt.model.comment.entity.Comment;
import com.atcumt.model.comment.entity.Reply;
import com.atcumt.model.like.constants.LikeAction;
import com.atcumt.model.like.dto.CommentLikeCountDTO;
import com.atcumt.model.like.entity.CommentLike;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@RocketMQMessageListener(
        topic = "like",
        selectorExpression = "commentLike",
        consumerGroup = "comment-like-consumer",
        maxReconsumeTimes = 3
)
@RequiredArgsConstructor
@Slf4j
public class CommentLikeConsumer implements RocketMQListener<CommentLikeCountDTO> {
    // 消息堆积触发阈值
    private static final int BATCH_SIZE = 20;
    private final MongoTemplate mongoTemplate;
    private final ConcurrentHashSet<CommentLikeCountDTO> likeCounts = new ConcurrentHashSet<>();

    @Override
    public void onMessage(CommentLikeCountDTO commentLikeCountDTO) {
        // 缓存消息
        likeCounts.add(commentLikeCountDTO);

        // 如果消息堆积到 BATCH_SIZE，触发批量消费
        if (likeCounts.size() >= BATCH_SIZE) {
            log.info("消息堆积达到阈值，触发批量消费...");
            batchCount();
        }
    }

    @Scheduled(fixedRate = 5000)
    public void scheduledCount() {
        if (!likeCounts.isEmpty()) {
            log.info("定时任务触发批量消费...");
            batchCount();
        }
    }

    // 批量消费逻辑
    private void batchCount() {
        log.info("正在批量计算帖子点赞量...");

        List<CommentLikeCountDTO> comments = null;

        // 消费消息时加锁，避免多线程同时消费
        synchronized (likeCounts) {
            comments = likeCounts.stream().toList();

            // 清空消息
            likeCounts.clear();
        }

        // 创建虚拟线程池
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        // 创建批处理操作
        Map<String, BulkOperations> bulkOpsMap = new HashMap<>();
        // 批量处理消息
        for (var comment : comments) {
            executor.submit(() -> {
                try {
                    log.info("计算评论点赞量，post: {}", comment);

                    Long start = System.currentTimeMillis();

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
                        log.info("计算评论热度");
                        if (comment.getCommentType().equals("comment")) {
                            score = HeatScoreUtil.getCommentHeat(
                                    likeCount,
                                    dislikeCount,
                                    completeComment.getReplyCount(),
                                    completeComment.getCreateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                            );
                        } else if (comment.getCommentType().equals("reply")) {
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
                        if (comment.getCommentType().equals("comment")) {
                            score = completeComment.getScore();
                        } else if (comment.getCommentType().equals("reply")) {
                            score = completeReply.getScore();
                        } else {
                            throw new IllegalArgumentException("不支持的评论类型");
                        }
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

                    Long end = System.currentTimeMillis();

                    log.info("计算评论点赞量耗时：{}ms", end - start);
                } catch (Exception e) {
                    log.error("计算评论点赞量时发生错误，postId: {}", comment.getCommentId(), e);
                }
            });
        }

        // 等待所有任务完成
        shutdownExecutor(executor);

        Long start = System.currentTimeMillis();

        // 执行所有批量操作
        for (BulkOperations bulkOps : bulkOpsMap.values()) {
            try {
                bulkOps.execute();
            } catch (Exception e) {
                log.error("批量更新评论点赞量时发生错误", e);
            }
        }

        Long end = System.currentTimeMillis();

        log.info("批量更新评论点赞量耗时：{}ms", end - start);
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
