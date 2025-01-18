package com.atcumt.user.listener;

import com.atcumt.model.user.dto.UserFollowCountDTO;
import com.atcumt.model.user.entity.UserFollow;
import com.atcumt.model.user.entity.UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@RocketMQMessageListener(
        topic = "user",
        selectorExpression = "userFollowCount",
        consumerGroup = "user-follow-count-consumer",
        maxReconsumeTimes = 3
)
@RequiredArgsConstructor
@Slf4j
public class UserFollowConsumer implements RocketMQListener<UserFollowCountDTO> {
    // 消息堆积触发阈值
    private static final int BATCH_SIZE = 20;
    private final MongoTemplate mongoTemplate;
    private final ConcurrentLinkedQueue<String> followerCounts = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<String> followedCounts = new ConcurrentLinkedQueue<>();

    @Override
    public void onMessage(UserFollowCountDTO userFollowCountDTO) {
        // 缓存消息
        followerCounts.add(userFollowCountDTO.getFollowerId());
        followedCounts.add(userFollowCountDTO.getFollowedId());

        // 如果消息堆积到 BATCH_SIZE，触发批量消费
        if (followerCounts.size() >= BATCH_SIZE) {
            log.info("消息堆积达到阈值，触发批量消费...");
            batchFollowingCount();
        }
        if (followedCounts.size() >= BATCH_SIZE) {
            log.info("消息堆积达到阈值，触发批量消费...");
            batchFollowersCount();
        }
    }

    @Scheduled(fixedRate = 3695)
    public void scheduledCount() {
        if (!followerCounts.isEmpty()) {
            log.info("定时任务触发批量消费...");
            batchFollowingCount();
        }
        if (!followedCounts.isEmpty()) {
            log.info("定时任务触发批量消费...");
            batchFollowersCount();
        }
    }

    private void batchFollowingCount() {
        log.info("正在批量计算关注量...");

        Set<String> followers = new HashSet<>();

        for (int i = 0; i < 20; i++) {
            String follower = followerCounts.poll();
            if (follower == null) {
                break;
            }
            followers.add(follower);
        }
        followers.remove(null);

        // 创建虚拟线程池
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        // 创建批处理操作
        BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, UserInfo.class);
        // 批量处理消息
        for (var follower : followers) {
            executor.submit(() -> {
                try {
                    log.info("计算关注量，follower: {}", follower);

                    long start = System.currentTimeMillis();

                    Integer followerCount = (int) mongoTemplate.count(
                            Query.query(Criteria.where("followerId").is(follower)),
                            UserFollow.class
                    );

                    bulkOps.updateOne(
                            Query.query(Criteria.where("_id").is(follower)),
                            new Update().set("followingsCount", followerCount)
                    );

                    long end = System.currentTimeMillis();

                    log.info("计算关注量耗时：{}ms", end - start);
                } catch (Exception e) {
                    log.error("计算关注量时发生错误，followerId: {}", follower, e);
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
            log.error("批量更新用户关注量时发生错误", e);
        }

        long end = System.currentTimeMillis();

        log.info("批量更新用户关注量耗时：{}ms", end - start);
    }

    private void batchFollowersCount() {
        log.info("正在批量计算粉丝量...");

        Set<String> followeds = new HashSet<>();

        for (int i = 0; i < 20; i++) {
            String followed = followedCounts.poll();
            if (followed == null) {
                break;
            }
            followeds.add(followed);
        }
        followeds.remove(null);

        // 创建虚拟线程池
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        // 创建批处理操作
        BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, UserInfo.class);
        // 批量处理消息
        for (var followed : followeds) {
            executor.submit(() -> {
                try {
                    log.info("计算粉丝量，followed: {}", followed);

                    long start = System.currentTimeMillis();

                    Integer followerCount = (int) mongoTemplate.count(
                            Query.query(Criteria.where("followedId").is(followed)),
                            UserFollow.class
                    );

                    bulkOps.updateOne(
                            Query.query(Criteria.where("_id").is(followed)),
                            new Update().set("followersCount", followerCount)
                    );

                    long end = System.currentTimeMillis();

                    log.info("计算粉丝量耗时：{}ms", end - start);
                } catch (Exception e) {
                    log.error("计算粉丝量时发生错误，followedId: {}", followed, e);
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
            log.error("批量更新用户粉丝量时发生错误", e);
        }

        long end = System.currentTimeMillis();

        log.info("批量更新用户粉丝量耗时：{}ms", end - start);
    }

    private void shutdownExecutor(@NotNull ExecutorService executor) {
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
