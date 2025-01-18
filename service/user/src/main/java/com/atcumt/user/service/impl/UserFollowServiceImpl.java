package com.atcumt.user.service.impl;

import cn.hutool.core.util.IdUtil;
import com.atcumt.common.exception.AuthorizationException;
import com.atcumt.common.utils.UserContext;
import com.atcumt.common.utils.UserInfoUtil;
import com.atcumt.common.utils.UserPrivacyUtil;
import com.atcumt.model.like.enums.LikeMessage;
import com.atcumt.model.user.dto.UserFollowCountDTO;
import com.atcumt.model.user.dto.UserFollowerDTO;
import com.atcumt.model.user.dto.UserFollowingDTO;
import com.atcumt.model.user.entity.UserFollow;
import com.atcumt.model.user.enums.PrivacyScope;
import com.atcumt.model.user.enums.UserMessage;
import com.atcumt.model.user.vo.UserFollowerVO;
import com.atcumt.model.user.vo.UserFollowingVO;
import com.atcumt.model.user.vo.UserInfoSimpleVO;
import com.atcumt.user.service.UserFollowService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserFollowServiceImpl implements UserFollowService {
    private final MongoTemplate mongoTemplate;
    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
    private final RocketMQTemplate rocketMQTemplate;
    private final UserInfoUtil userInfoUtil;
    private final UserPrivacyUtil userPrivacyUtil;

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

            // 异步更新用户关注量和粉丝量
            followCount(UserFollowCountDTO
                    .builder()
                    .followerId(userId)
                    .followedId(followedId)
                    .build());
        });
    }

    @Override
    public UserFollowingVO getUserFollowings(UserFollowingDTO userFollowingDTO) {
        String userId = userFollowingDTO.getUserId();
        // 检查用户是否有权限访问关注列表
        if (!userId.equals(UserContext.getUserId()) && !userPrivacyUtil.checkPrivacy(userId, PrivacyScope.FOLLOWING)) {
            throw new AuthorizationException(UserMessage.FOLLOWING_PRIVACY_DENIED.getMessage());
        }

        Query query = Query.query(Criteria.where("followerId").is(userId));

        if (userFollowingDTO.getCursor() != null) {
            LocalDateTime cursor;
            try {
                cursor = LocalDateTime.parse(userFollowingDTO.getCursor());
            } catch (Exception e) {
                throw new IllegalArgumentException(LikeMessage.CURSOR_FORMAT_INCORRECT.getMessage());
            }
            // 先添加筛选条件，再进行排序
            query.addCriteria(Criteria.where("createTime").lte(cursor));
        }

        // 如果有 lastFollowId，添加额外的条件：筛选 followId 小于 lastFollowId
        if (userFollowingDTO.getLastFollowId() != null) {
            query.addCriteria(Criteria.where("followId").lt(userFollowingDTO.getLastFollowId()));
        }

        // 排序：先按 score 排序，再按 followId 排序
        query.with(Sort.by(
                Sort.Order.desc("createTime"),
                Sort.Order.desc("followId")
        ));

        // 设置分页大小
        query.limit(userFollowingDTO.getSize());

        List<UserFollow> userFollows = mongoTemplate.find(query, UserFollow.class);

        // 使用Set去重
        Set<String> userIdSet = new HashSet<>();

        for (var userFollow : userFollows) {
            userIdSet.add(userFollow.getFollowedId());
        }
        userIdSet.remove(null);

        List<UserInfoSimpleVO> userInfoSimpleVOs = userInfoUtil.getUserInfoSimpleBatch(userIdSet.stream().toList());
        Map<String, UserInfoSimpleVO> userInfoSimpleVOMap = userInfoSimpleVOs.stream()
                .collect(Collectors.toMap(UserInfoSimpleVO::getUserId, userInfoSimpleVO -> userInfoSimpleVO));

        List<UserInfoSimpleVO> followings = new ArrayList<>();

        for (var userFollow : userFollows) {
            UserInfoSimpleVO userInfoSimpleVO = userInfoSimpleVOMap.get(userFollow.getFollowedId());

            if (userInfoSimpleVO == null) {
                continue;
            }

            followings.add(userInfoSimpleVO);
        }

        Long lastFollowId = null;
        String cursor = null;
        if (!userFollows.isEmpty()) {
            lastFollowId = userFollows.getLast().getFollowId();

            cursor = userFollows.getLast().getCreateTime().toString();
        }

        UserFollowingVO userFollowingVO = UserFollowingVO.builder()
                .size(followings.size())
                .cursor(cursor)
                .lastFollowId(lastFollowId)
                .followings(followings)
                .build();
        return userFollowingVO;
    }

    @Override
    public UserFollowerVO getUserFollowers(UserFollowerDTO userFollowerDTO) {
        String userId = userFollowerDTO.getUserId();
        // 检查用户是否有权限访问关注列表
        if (!userId.equals(UserContext.getUserId()) && !userPrivacyUtil.checkPrivacy(userId, PrivacyScope.FOLLOWER)) {
            throw new AuthorizationException(UserMessage.FOLLOWER_PRIVACY_DENIED.getMessage());
        }

        Query query = Query.query(Criteria.where("followedId").is(userId));

        if (userFollowerDTO.getCursor() != null) {
            LocalDateTime cursor;
            try {
                cursor = LocalDateTime.parse(userFollowerDTO.getCursor());
            } catch (Exception e) {
                throw new IllegalArgumentException(LikeMessage.CURSOR_FORMAT_INCORRECT.getMessage());
            }
            // 先添加筛选条件，再进行排序
            query.addCriteria(Criteria.where("createTime").lte(cursor));
        }

        // 如果有 lastFollowId，添加额外的条件：筛选 followId 小于 lastFollowId
        if (userFollowerDTO.getLastFollowId() != null) {
            query.addCriteria(Criteria.where("followId").lt(userFollowerDTO.getLastFollowId()));
        }

        // 排序：先按 score 排序，再按 followId 排序
        query.with(Sort.by(
                Sort.Order.desc("createTime"),
                Sort.Order.desc("followId")
        ));

        // 设置分页大小
        query.limit(userFollowerDTO.getSize());

        List<UserFollow> userFollows = mongoTemplate.find(query, UserFollow.class);

        // 使用Set去重
        Set<String> userIdSet = new HashSet<>();

        for (var userFollow : userFollows) {
            userIdSet.add(userFollow.getFollowerId());
        }
        userIdSet.remove(null);

        List<UserInfoSimpleVO> userInfoSimpleVOs = userInfoUtil.getUserInfoSimpleBatch(userIdSet.stream().toList());
        Map<String, UserInfoSimpleVO> userInfoSimpleVOMap = userInfoSimpleVOs.stream()
                .collect(Collectors.toMap(UserInfoSimpleVO::getUserId, userInfoSimpleVO -> userInfoSimpleVO));

        List<UserInfoSimpleVO> followers = new ArrayList<>();

        for (var userFollow : userFollows) {
            UserInfoSimpleVO userInfoSimpleVO = userInfoSimpleVOMap.get(userFollow.getFollowerId());

            if (userInfoSimpleVO == null) {
                continue;
            }

            followers.add(userInfoSimpleVO);
        }

        Long lastFollowId = null;
        String cursor = null;
        if (!userFollows.isEmpty()) {
            lastFollowId = userFollows.getLast().getFollowId();

            cursor = userFollows.getLast().getCreateTime().toString();
        }

        UserFollowerVO userFollowerVO = UserFollowerVO.builder()
                .size(followers.size())
                .cursor(cursor)
                .lastFollowId(lastFollowId)
                .followers(followers)
                .build();
        return userFollowerVO;
    }

    public void followCount(UserFollowCountDTO userFollowCountDTO) {
        rocketMQTemplate.asyncSend("user:userFollowCount", userFollowCountDTO, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
            }

            @Override
            public void onException(Throwable e) {
                log.error("用户关注计数消息发送失败e: {}", e.getMessage());
            }
        });
    }
}
