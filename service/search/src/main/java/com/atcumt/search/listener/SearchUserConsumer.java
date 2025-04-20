package com.atcumt.search.listener;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import com.atcumt.common.api.auth.AuthDubboService;
import com.atcumt.model.search.dto.SearchUserDTO;
import com.atcumt.model.search.entity.UserEs;
import com.atcumt.model.user.entity.UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@RocketMQMessageListener(
        topic = "search",
        selectorExpression = "searchUser",
        consumerGroup = "search-user-consumer",
        maxReconsumeTimes = 1
)
@RequiredArgsConstructor
@Slf4j
public class SearchUserConsumer implements RocketMQListener<SearchUserDTO> {
    private final MongoTemplate mongoTemplate;
    private final ElasticsearchClient elasticsearchClient;
    @DubboReference
    private AuthDubboService authDubboService;

    @Override
    public void onMessage(SearchUserDTO searchUserDTO) {
        if (searchUserDTO.getUserId() == null) {
            log.error("用户信息不完整，无法处理");
        } else if (Boolean.FALSE.equals(searchUserDTO.getIsUserAuth())) {
            log.info("更新用户信息索引，用户ID: {}", searchUserDTO.getUserId());
            updateUserInfo(searchUserDTO.getUserId());
        } else {
            log.info("更新用户认证索引，用户ID: {}", searchUserDTO.getUserId());
            updateUserAuth(searchUserDTO.getUserId());
        }
    }

    public void updateUserInfo(String userId) {
        try {
            UserInfo userInfo = getUserInfo(userId);
            if (userInfo == null) {
                deleteUser(userId);
                return;
            }

            elasticsearchClient.update(ur -> ur
                    .index("user")
                    .id(userId)
                    .doc(getUserInfo(userId)), UserEs.class);
        } catch (ElasticsearchException e) {
            indexUser(userId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateUserAuth(String userId) {
        try {
            String username = getUsername(userId);
            if (username == null || username.isEmpty()) {
                deleteUser(userId);
                return;
            }

            elasticsearchClient.update(ur -> ur
                    .index("user")
                    .id(userId)
                    .doc(Map.of("username", username)), UserEs.class);
        } catch (ElasticsearchException e) {
            indexUser(userId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    public void indexUser(String userId) {
        UserInfo userInfo = getUserInfo(userId);

        if (userInfo == null) {
            deleteUser(userId);
            return;
        }

        List<String> statuses = null;
        if (userInfo.getStatuses() != null && !userInfo.getStatuses().isEmpty()) {
            statuses = userInfo.getStatuses().stream().map(userStatus -> userStatus.getEmoji() + userStatus.getText()).toList();
        }

        UserEs userEs = UserEs
                .builder()
                .username(getUsername(userId))
                .nickname(userInfo.getNickname())
                .bio(userInfo.getBio())
                .hometown(userInfo.getHometown())
                .major(userInfo.getMajor())
                .statuses(statuses)
                .level(userInfo.getLevel())
                .experience(userInfo.getExperience())
                .followersCount(userInfo.getFollowersCount())
                .build();

        elasticsearchClient.index(ir -> ir
                .index("user")
                .id(userId)
                .document(userEs));
    }

    @SneakyThrows
    public void deleteUser(String userId) {
        elasticsearchClient.delete(dr -> dr
                .index("user")
                .id(userId));
    }

    private UserInfo getUserInfo(String userId) {
        return mongoTemplate.findById(userId, UserInfo.class);
    }

    private String getUsername(String userId) {
        return authDubboService.getUsername(userId);
    }
}
