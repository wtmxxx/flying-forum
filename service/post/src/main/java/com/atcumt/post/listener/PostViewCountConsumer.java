package com.atcumt.post.listener;

import com.atcumt.model.post.dto.PostViewCountDTO;
import com.atcumt.model.post.entity.Tag;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@RocketMQMessageListener(
        topic = "post",
        selectorExpression = "postViewCount",
        consumerGroup = "post-view-count-consumer",
        maxReconsumeTimes = 1
)
@RequiredArgsConstructor
@Slf4j
public class PostViewCountConsumer implements RocketMQListener<PostViewCountDTO> {
    // 消息堆积触发阈值
    private static final int BATCH_SIZE = 20;
    private final MongoTemplate mongoTemplate;
    private final ConcurrentHashMap<String, Long> postViewCountMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> tagViewCountMap = new ConcurrentHashMap<>();

    @Override
    public void onMessage(PostViewCountDTO postViewCountDTO) {
        // 缓存消息
        String postKey = postViewCountDTO.getPostType() + ":" + postViewCountDTO.getPostId();
        postViewCountMap.merge(postKey, 1L, Long::sum);
        for (var tagId : postViewCountDTO.getTagIds()) {
            tagViewCountMap.merge(tagId.toString(), 1L, Long::sum);
        }

        // 如果消息堆积到 BATCH_SIZE，触发批量消费
        if (postViewCountMap.size() >= BATCH_SIZE) {
            log.info("消息堆积达到阈值，触发批量消费...");
            batchPostViewCount();
        }
        if (tagViewCountMap.size() >= BATCH_SIZE) {
            log.info("消息堆积达到阈值，触发批量消费...");
            batchTagViewCount();
        }
    }

    @Scheduled(fixedRate = 5580)
    public void scheduledCount() {
        if (!postViewCountMap.isEmpty()) {
            log.info("定时任务触发批量消费...");
            batchPostViewCount();
        }
        if (!tagViewCountMap.isEmpty()) {
            log.info("定时任务触发批量消费...");
            batchTagViewCount();
        }
    }

    private void batchPostViewCount() {
        log.info("正在批量计算帖子浏览量...");

        // 创建虚拟线程池
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        Map<String, Long> postViewCounts = new HashMap<>(postViewCountMap);
        postViewCountMap.clear();

        // 创建批处理操作
        Map<String, BulkOperations> bulkOpsMap = new HashMap<>();
        // 批量处理消息
        for (var post : postViewCounts.entrySet()) {
            executor.submit(() -> {
                try {
                    log.info("计算帖子浏览量，post: {}", post.getKey());

                    long start = System.currentTimeMillis();

                    String postKey = post.getKey();
                    String postType = postKey.split(":")[0];
                    Long postId = Long.valueOf(postKey.split(":")[1]);
                    // 获取批处理操作
                    BulkOperations bulkOps = bulkOpsMap.computeIfAbsent(postType, key ->
                            mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, postType)
                    );

                    // 根据 postType 执行不同的操作，假设是更新操作
                    bulkOps.updateOne(
                            Query.query(Criteria.where("_id").is(postId)),
                            new Update().inc("viewCount", post.getValue())
                    );

                    long end = System.currentTimeMillis();

                    log.info("计算帖子浏览量耗时：{}ms", end - start);
                } catch (Exception e) {
                    log.error("计算帖子浏览量时发生错误，post: {}", post.getKey(), e);
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
                log.error("批量更新帖子浏览量时发生错误", e);
            }
        }

        long end = System.currentTimeMillis();

        log.info("批量更新帖子浏览量耗时：{}ms", end - start);
    }

    private void batchTagViewCount() {
        log.info("正在批量计算标签浏览量...");

        // 创建虚拟线程池
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        Map<String, Long> tagViewCounts = new HashMap<>(tagViewCountMap);
        tagViewCountMap.clear();

        // 创建批处理操作
        BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, Tag.class);
        // 批量处理消息
        for (var tag : tagViewCounts.entrySet()) {
            executor.submit(() -> {
                try {
                    log.info("计算标签浏览量，tag: {}", tag.getKey());

                    long start = System.currentTimeMillis();

                    // 根据 postType 执行不同的操作，假设是更新操作
                    bulkOps.updateOne(
                            Query.query(Criteria.where("_id").is(Long.valueOf(tag.getKey()))),
                            new Update().inc("viewCount", tag.getValue())
                    );

                    long end = System.currentTimeMillis();

                    log.info("计算标签浏览量耗时：{}ms", end - start);
                } catch (Exception e) {
                    log.error("计算标签浏览量时发生错误，tag: {}", tag.getKey(), e);
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
            log.error("批量更新标签浏览量时发生错误", e);
        }

        long end = System.currentTimeMillis();

        log.info("批量更新标签浏览量耗时：{}ms", end - start);
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
