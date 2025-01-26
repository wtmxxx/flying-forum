package com.atcumt.common.utils;

import cn.hutool.core.bean.BeanUtil;
import com.atcumt.model.user.entity.UserInfo;
import com.atcumt.model.user.vo.UserInfoSimpleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@ConditionalOnClass({MongoTemplate.class, RedisTemplate.class})
public class UserInfoUtil {
    private final MongoTemplate mongoTemplate;
    private final RedisTemplate<String, UserInfoSimpleVO> userInfoSimpleRedisTemplate;

    public UserInfoSimpleVO getUserInfoSimple(String userId) {
        // 获取用户信息
        // 1. 从缓存中获取用户信息
        String userInfoKey = "User:info:simple:" + userId;

        UserInfoSimpleVO userInfoSimpleVO = userInfoSimpleRedisTemplate.opsForValue().get(userInfoKey);

        if (userInfoSimpleVO != null) {
            return userInfoSimpleVO;
        }

        // 2. 从数据库中获取用户信息
        UserInfo userInfo = mongoTemplate.findById(userId, UserInfo.class);
        // 3. 保存到缓存中
        userInfoSimpleVO = BeanUtil.copyProperties(userInfo, UserInfoSimpleVO.class);
        userInfoSimpleRedisTemplate.opsForValue().set(userInfoKey, userInfoSimpleVO, 7, TimeUnit.DAYS);

        return userInfoSimpleVO;
    }

    public List<UserInfoSimpleVO> getUserInfoSimpleBatch(List<String> userIds) {
        if (userIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取用户信息
        // 1. 从缓存中获取用户信息
        String userInfoKey = "User:info:simple:";

        List<String> userInfoKeys = userIds.stream()
                .map(userId -> userInfoKey + userId)
                .toList();

        List<UserInfoSimpleVO> userInfoSimpleVOs = userInfoSimpleRedisTemplate.opsForValue().multiGet(userInfoKeys);

        if (userInfoSimpleVOs == null) {
            userInfoSimpleVOs = new ArrayList<>();
        }

        Map<String, UserInfoSimpleVO> userInfoPart = new HashMap<>();

        for (int i = 0; i < userIds.size(); i++) {
            if (userInfoSimpleVOs.get(i) == null) {
                String userId = userIds.get(i);

                if (userId == null) {
                    continue;
                }

                // 先从Map缓存中获取
                if (userInfoPart.containsKey(userId)) {
                    userInfoSimpleVOs.set(i, userInfoPart.get(userId));
                    continue;
                }

                // 从数据库中获取
                UserInfo userInfo = mongoTemplate.findById(userId, UserInfo.class);
                UserInfoSimpleVO userInfoSimpleVO = BeanUtil.copyProperties(userInfo, UserInfoSimpleVO.class);
                userInfoSimpleRedisTemplate.opsForValue().set(userInfoKey + userId, userInfoSimpleVO, 7, TimeUnit.DAYS);
                userInfoSimpleVOs.set(i, userInfoSimpleVO);
                userInfoPart.put(userId, userInfoSimpleVO);
            }
        }

        return userInfoSimpleVOs;
    }
}
