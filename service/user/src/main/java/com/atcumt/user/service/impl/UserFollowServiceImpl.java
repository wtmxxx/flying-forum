package com.atcumt.user.service.impl;

import cn.hutool.core.util.IdUtil;
import com.atcumt.common.utils.UserContext;
import com.atcumt.model.user.entity.UserFollow;
import com.atcumt.model.user.enums.UserMessage;
import com.atcumt.user.service.UserFollowService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserFollowServiceImpl implements UserFollowService {
    private final MongoTemplate mongoTemplate;
    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    // 关闭线程池
    @PreDestroy
    public void shutdownExecutor() {
        log.info("正在关闭虚拟线程池");
        executorService.shutdown();
    }

    @Override
    public void followUser(String followedId) {
        String userId = UserContext.getUserId();
        if (userId.equals(followedId)) {
            throw new IllegalArgumentException(UserMessage.CANNOT_FOLLOW_SELF.getMessage());
        }

        // 异步查询和后续操作
        executorService.submit(() -> {
            // 构造查询条件：通过 followerId 和 followedId 查找
            Query query = new Query(Criteria
                    .where("followerId").is(userId)
                    .and("followedId").is(followedId));
            // 查询是否已存在该记录
            UserFollow existingFollow = mongoTemplate.findAndRemove(query, UserFollow.class);

            if (existingFollow == null) {
                // 如果不存在，创建并插入新记录
                UserFollow userFollow = UserFollow
                        .builder()
                        .followId(IdUtil.getSnowflakeNextId())
                        .followerId(userId)
                        .followedId(followedId)
                        .createTime(LocalDateTime.now())
                        .build();

                // 使用 MongoTemplate 保存新记录
                mongoTemplate.insert(userFollow);
            }

            // TODO: 异步更新用户关注量
        });
    }
}
