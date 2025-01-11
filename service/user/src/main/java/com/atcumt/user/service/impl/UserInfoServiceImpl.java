package com.atcumt.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.atcumt.common.utils.UserContext;
import com.atcumt.model.auth.entity.UserAuth;
import com.atcumt.model.user.entity.UserInfo;
import com.atcumt.model.user.entity.UserStatus;
import com.atcumt.model.user.enums.UserMessage;
import com.atcumt.model.user.vo.UserInfoVO;
import com.atcumt.user.mapper.UserAuthMapper;
import com.atcumt.user.service.UserInfoService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserInfoServiceImpl implements UserInfoService {
    private final MongoTemplate mongoTemplate;
    private final UserAuthMapper userAuthMapper;

    @Override
    public UserInfoVO getUserInfo(String userId) throws ExecutionException, InterruptedException {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<UserInfo> userInfoFuture = executor.submit(() -> {
                UserInfo userInfo = mongoTemplate.findOne(
                        Query.query(Criteria.where("userId").is(userId)), UserInfo.class
                );

                if (userInfo != null && userInfo.getStatuses() != null && !userInfo.getStatuses().isEmpty()) {
                    List<UserStatus> userStatuses = new ArrayList<>();

                    for (var status : userInfo.getStatuses()) {
                        if (status.getExpiresAt().isAfter(LocalDateTime.now())) {
                            userStatuses.add(status);
                        }
                    }

                    if (userInfo.getStatuses().size() != userStatuses.size()) {
                        mongoTemplate.updateFirst(
                                Query.query(Criteria.where("userId").is(UserContext.getUserId())),
                                Update.update("statuses", userStatuses), UserInfo.class
                        );
                    }

                    userInfo.setStatuses(userStatuses);
                }

                return userInfo;
            });

            Future<UserAuth> userAuthFuture = executor.submit(() -> {
                return userAuthMapper.selectOne(Wrappers
                        .<UserAuth>lambdaQuery()
                        .select(UserAuth::getUsername)
                        .eq(UserAuth::getUserId, userId)
                );
            });

            UserInfo userInfo = userInfoFuture.get();
            if (userInfo == null) {
                throw new IllegalArgumentException(UserMessage.USER_NOT_FOUND.getMessage());
            }
            UserInfoVO userInfoVO = BeanUtil.toBean(userInfo, UserInfoVO.class);
            userInfoVO.setUsername(userAuthFuture.get().getUsername());

            return userInfoVO;
        }
    }

    @Override
    public void changeNickname(String nickname) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("userId").is(UserContext.getUserId())),
                Update.update("nickname", nickname), UserInfo.class
        );
    }

    @Override
    public void changeAvatar(String avatar) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("userId").is(UserContext.getUserId())),
                Update.update("avatar", avatar), UserInfo.class
        );
    }

    @Override
    public void changeBio(String bio) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("userId").is(UserContext.getUserId())),
                Update.update("bio", bio), UserInfo.class
        );
    }

    @Override
    public void changeGender(Integer gender) {
        if (gender < -1 || gender > 1) {
            log.warn("无效的性别值, userId: {}", UserContext.getUserId());
            return;  // 性别参数必须在[-1, 1]之间
        }

        mongoTemplate.updateFirst(
                Query.query(Criteria.where("userId").is(UserContext.getUserId())),
                Update.update("gender", gender), UserInfo.class
        );
    }

    @Override
    public void changeHometown(String hometown) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("userId").is(UserContext.getUserId())),
                Update.update("hometown", hometown), UserInfo.class
        );
    }

    @Override
    public void changeMajor(String major) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("userId").is(UserContext.getUserId())),
                Update.update("major", major), UserInfo.class
        );
    }

    @Override
    public void changeGrade(Integer grade) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("userId").is(UserContext.getUserId())),
                Update.update("grade", grade), UserInfo.class
        );
    }

    @Override
    public void changeStatuses(List<UserStatus> statuses) {
        if (statuses == null) {
            throw new IllegalArgumentException("状态无效");
        } else if (statuses.size() > 10) {
            throw new IllegalArgumentException("状态过多");
        }
        // 使用HashSet去重
        // 去重并过滤过期的状态
        statuses = statuses.stream()
                .filter(s -> s.getExpiresAt() == null || s.getExpiresAt().isAfter(LocalDateTime.now())) // 过滤未过期状态
                .filter(new HashSet<>()::add) // 利用HashSet的add方法去重
                .toList();

        mongoTemplate.updateFirst(
                Query.query(Criteria.where("userId").is(UserContext.getUserId())),
                Update.update("statuses", statuses), UserInfo.class
        );
    }
}
