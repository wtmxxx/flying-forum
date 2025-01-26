package com.atcumt.post.listener;

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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@RocketMQMessageListener(
        topic = "post",
        selectorExpression = "tagUsageCount",
        consumerGroup = "tag-usage-count-consumer",
        maxReconsumeTimes = 3
)
@RequiredArgsConstructor
@Slf4j
public class TagUsageCountConsumer implements RocketMQListener<List<Long>> {
    // 消息堆积触发阈值
    private static final int BATCH_SIZE = 10;
    private final MongoTemplate mongoTemplate;
    private final ConcurrentHashMap<String, Long> tagUsageCountMap = new ConcurrentHashMap<>();

    @Override
    public void onMessage(List<Long> tagIds) {
        // 缓存消息
        for (var tagId : tagIds) {
            tagUsageCountMap.merge(tagId.toString(), 1L, Long::sum);
        }

        // 如果消息堆积到 BATCH_SIZE，触发批量消费
        if (tagUsageCountMap.size() >= BATCH_SIZE) {
            log.info("消息堆积达到阈值，触发批量消费...");
            batchTagUsageCount();
        }
    }

    @Scheduled(fixedRate = 30000)
    public void scheduledCount() {
        if (!tagUsageCountMap.isEmpty()) {
            log.info("定时任务触发批量消费...");
            batchTagUsageCount();
        }
    }

    private void batchTagUsageCount() {
        log.info("正在批量计算标签使用量...");

        // 创建虚拟线程池
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        Map<String, Long> tagUsageCounts = new HashMap<>(tagUsageCountMap);
        tagUsageCountMap.clear();

        // 创建批处理操作
        BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, Tag.class);
        // 批量处理消息
        for (var tag : tagUsageCounts.entrySet()) {
            executor.submit(() -> {
                try {
                    log.info("计算标签使用量，tag: {}", tag.getKey());

                    long start = System.currentTimeMillis();

                    // 根据 postType 执行不同的操作，假设是更新操作
                    bulkOps.updateOne(
                            Query.query(Criteria.where("_id").is(Long.valueOf(tag.getKey()))),
                            new Update().inc("usageCount", tag.getValue())
                    );

                    long end = System.currentTimeMillis();

                    log.info("计算标签使用量耗时：{}ms", end - start);
                } catch (Exception e) {
                    log.error("计算标签使用量时发生错误，tag: {}", tag.getKey(), e);
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
            log.error("批量更新标签使用量时发生错误", e);
        }

        long end = System.currentTimeMillis();

        log.info("批量更新标签使用量耗时：{}ms", end - start);
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
