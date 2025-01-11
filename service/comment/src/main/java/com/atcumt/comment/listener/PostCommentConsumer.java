package com.atcumt.comment.listener;

import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.json.JSONObject;
import com.atcumt.common.utils.HeatScoreUtil;
import com.atcumt.model.comment.dto.PostCommentCountDTO;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@RocketMQMessageListener(
        topic = "comment",
        selectorExpression = "postComment",
        consumerGroup = "post-comment-consumer",
        maxReconsumeTimes = 3
)
@RequiredArgsConstructor
@Slf4j
public class PostCommentConsumer implements RocketMQListener<PostCommentCountDTO> {

    // 消息堆积触发阈值
    private static final int BATCH_SIZE = 20;
    private final MongoTemplate mongoTemplate;
    private final ConcurrentHashSet<PostCommentCountDTO> commentCounts = new ConcurrentHashSet<>();

    @Override
    public void onMessage(PostCommentCountDTO postCommentCountDTO) {
        // 缓存消息
        commentCounts.add(postCommentCountDTO);

        // 如果消息堆积到 BATCH_SIZE，触发批量消费
        if (commentCounts.size() >= BATCH_SIZE) {
            log.info("消息堆积达到阈值，触发批量消费...");
            batchCount();
        }
    }

    @Scheduled(fixedRate = 5000)
    public void scheduledCount() {
        if (!commentCounts.isEmpty()) {
            log.info("定时任务触发批量消费...");
            batchCount();
        }
    }

    // 批量消费逻辑
    private void batchCount() {
        log.info("正在批量计算评论数...");

        List<PostCommentCountDTO> posts = null;

        // 消费消息时加锁，避免多线程同时消费
        synchronized (commentCounts) {
            posts = commentCounts.stream().toList();

            // 清空消息
            commentCounts.clear();
        }

        // 创建虚拟线程池
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        // 创建批处理操作
        Map<String, BulkOperations> bulkOpsMap = new HashMap<>();
        // 批量处理消息
        for (var post : posts) {
            executor.submit(() -> {
                try {
                    log.info("计算帖子评论数，post: {}", post);

                    Long start = System.currentTimeMillis();

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

                    // 计算评论热度
                    double score;
                    try {
                        log.info("计算帖子热度");
                        score = HeatScoreUtil.getPostHeat(
                                completePost.getInt("likeCount"),
                                completePost.getInt("dislikeCount"),
                                commentCount
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
                            new Update().set("commentCount", commentCount).set("score", score)
                    );

                    Long end = System.currentTimeMillis();

                    log.info("计算评论数耗时：{}ms", end - start);
                } catch (Exception e) {
                    log.error("计算帖子评论数时发生错误，postId: {}", post.getPostId(), e);
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
                log.error("批量更新帖子评论数时发生错误", e);
            }
        }

        Long end = System.currentTimeMillis();

        log.info("批量更新帖子评论数耗时：{}ms", end - start);
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
