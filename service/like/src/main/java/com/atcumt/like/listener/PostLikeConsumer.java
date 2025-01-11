package com.atcumt.like.listener;

import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.json.JSONObject;
import com.atcumt.common.utils.HeatScoreUtil;
import com.atcumt.model.common.enums.PostType;
import com.atcumt.model.like.constants.LikeAction;
import com.atcumt.model.like.dto.PostLikeCountDTO;
import com.atcumt.model.like.entity.PostLike;
import com.atcumt.model.user.entity.UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
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

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@RocketMQMessageListener(
        topic = "like",
        selectorExpression = "postLike",
        consumerGroup = "post-like-consumer",
        maxReconsumeTimes = 3
)
@RequiredArgsConstructor
@Slf4j
public class PostLikeConsumer implements RocketMQListener<PostLikeCountDTO> {
    // 消息堆积触发阈值
    private static final int BATCH_SIZE = 20;
    private final MongoTemplate mongoTemplate;
    private final ConcurrentHashSet<PostLikeCountDTO> likeCounts = new ConcurrentHashSet<>();

    @Override
    public void onMessage(PostLikeCountDTO postLikeCountDTO) {
        // 缓存消息
        likeCounts.add(postLikeCountDTO);

        // 如果消息堆积到 BATCH_SIZE，触发批量消费
        if (likeCounts.size() >= BATCH_SIZE) {
            log.info("消息堆积达到阈值，触发批量消费...");
            batchCount();
        }
    }

    @Scheduled(fixedRate = 5678)
    public void scheduledCount() {
        if (!likeCounts.isEmpty()) {
            log.info("定时任务触发批量消费...");
            batchCount();
        }
    }

    // 批量消费逻辑
    private void batchCount() {
        log.info("正在批量计算帖子点赞量...");

        List<PostLikeCountDTO> posts;

        // 消费消息时加锁，避免多线程同时消费
        synchronized (likeCounts) {
            posts = likeCounts.stream().toList();

            // 清空消息
            likeCounts.clear();
        }

        Set<String> users = new HashSet<>();

        // 创建虚拟线程池
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        // 创建批处理操作
        Map<String, BulkOperations> bulkOpsMap = new HashMap<>();
        // 批量处理消息
        for (var post : posts) {
            executor.submit(() -> {
                try {
                    log.info("计算帖子点赞量，post: {}", post);

                    long start = System.currentTimeMillis();

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

                    users.add(completePost.get("userId").toString());

                    // 计算评论热度
                    double score;
                    try {
                        log.info("计算帖子热度");
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

                    long end = System.currentTimeMillis();

                    log.info("计算帖子点赞量耗时：{}ms", end - start);
                } catch (Exception e) {
                    log.error("计算帖子点赞量时发生错误，postId: {}", post.getPostId(), e);
                }
            });
        }

        // 等待所有任务完成
        shutdownExecutor(executor);

        long start = System.currentTimeMillis();

        // 执行所有批量操作
        for (BulkOperations bulkOps : bulkOpsMap.values()) {
            try {
                bulkOps.execute();
            } catch (Exception e) {
                log.error("批量更新帖子点赞量时发生错误", e);
            }
        }

        long end = System.currentTimeMillis();

        log.info("批量更新帖子点赞量耗时：{}ms", end - start);

        // 批量更新用户获赞量
        users.remove(null);
        batchUserLikeReceivedCount(users);
    }

    private void batchUserLikeReceivedCount(Set<String> users) {
        // 创建虚拟线程池
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        // 获取批处理操作
        BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, UserInfo.class);

        long start = System.currentTimeMillis();

        for (var userId : users) {
            executor.submit(() -> {
                try {
                    Query userInfoQuery = new Query(Criteria.where("_id").is(userId));
                    userInfoQuery.fields().include("likeReceivedCount");
                    UserInfo userInfo = mongoTemplate.findOne(userInfoQuery, UserInfo.class);
                    if (userInfo == null) return;
                    if (userInfo.getLikeReceivedCount() == 1000000) {
                        return;
                    } else {
                        bulkOps.updateOne(userInfoQuery, Update.update("likeReceivedCount", 1000000));
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
            });
        }

        // 等待所有任务完成
        shutdownExecutor(executor);

        long end = System.currentTimeMillis();

        log.info("计算用户获赞量耗时：{}ms", end - start);

        start = System.currentTimeMillis();

        bulkOps.execute();

        end = System.currentTimeMillis();

        log.info("批量更新用户获赞量耗时：{}ms", end - start);
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
