package com.atcumt.common.utils;

import com.atcumt.model.user.entity.UserFollow;
import com.atcumt.model.user.entity.UserPrivacy;
import com.atcumt.model.user.enums.PrivacyLevel;
import com.atcumt.model.user.enums.PrivacyScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnClass({MongoTemplate.class})
@Slf4j
public class UserPrivacyUtil {
    private final MongoTemplate mongoTemplate;

    /**
     * 检查用户是否具有访问特定对象的权限
     *
     * @param targetUserId 目标用户的 ID
     * @param privacyScope 目标隐私范围
     * @return 是否具有权限
     */
    public boolean checkPrivacy(String targetUserId, PrivacyScope privacyScope) {
        String currentUserId = UserContext.getUserId();

        // 查询用户隐私设置
        Query query = new Query(Criteria.where("userId").is(targetUserId)
                .and("privacyScope").is(privacyScope));
        UserPrivacy userPrivacy = mongoTemplate.findOne(query, UserPrivacy.class);

        if (userPrivacy == null) {
            return true; // 返回 true，表示未配置隐私设置，即公开可见
        }

        // 校验 PrivacyLevel
        PrivacyLevel level = PrivacyLevel.fromString(userPrivacy.getPrivacyLevel());
        if (level == null) {
            log.warn("无效的隐私级别: {}, targetUserId: {}", userPrivacy.getPrivacyLevel(), targetUserId);
            return false; // 返回 false，表示无效的隐私级别
        }

        // 根据隐私级别执行不同的权限检查
        return switch (level) {
            case PUBLIC -> true; // 公开可见，所有用户都能访问
            case FOLLOWERS_ONLY -> isFollower(currentUserId, targetUserId); // 仅粉丝可见
            case MUTUAL_FOLLOWERS_ONLY -> isMutualFollower(currentUserId, targetUserId); // 仅互关可见
            case SPECIFIC_USERS_ONLY ->
                    userPrivacy.getSpecificUsers() != null && userPrivacy.getSpecificUsers().contains(currentUserId); // 仅指定用户可见
        };
    }

    /**
     * 检查当前用户是否是目标用户的粉丝
     *
     * @param currentUserId 当前用户 ID
     * @param targetUserId  目标用户 ID
     * @return 是否为粉丝
     */
    private boolean isFollower(String currentUserId, String targetUserId) {
        UserFollow userFollow = mongoTemplate.findOne(
                new Query(Criteria.where("followerId").is(currentUserId).and("followedId").is(targetUserId)),
                UserFollow.class
        );

        return userFollow != null;
    }

    /**
     * 检查当前用户和目标用户是否是互关关系
     *
     * @param currentUserId 当前用户 ID
     * @param targetUserId  目标用户 ID
     * @return 是否是互关
     */
    private boolean isMutualFollower(String currentUserId, String targetUserId) {
        List<UserFollow> userFollows = mongoTemplate.find(
                new Query(Criteria.where("followerId").is(currentUserId).and("followedId").is(targetUserId)
                        .orOperator(Criteria.where("followerId").is(targetUserId).and("followedId").is(currentUserId))),
                UserFollow.class
        );

        return userFollows.size() >= 2;
    }
}
