package com.atcumt.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.atcumt.common.utils.UserContext;
import com.atcumt.model.user.dto.UserPrivacyDTO;
import com.atcumt.model.user.entity.UserPrivacy;
import com.atcumt.model.user.enums.PrivacyLevel;
import com.atcumt.model.user.enums.PrivacyScope;
import com.atcumt.model.user.enums.UserMessage;
import com.atcumt.model.user.vo.UserPrivacyVO;
import com.atcumt.user.service.UserPrivacyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserPrivacyServiceImpl implements UserPrivacyService {
    private final MongoTemplate mongoTemplate;

    @Override
    public void setPrivacyLevel(UserPrivacyDTO userPrivacyDTO) {
        PrivacyScope scope = PrivacyScope.fromString(userPrivacyDTO.getPrivacyScope());
        PrivacyLevel level = PrivacyLevel.fromString(userPrivacyDTO.getPrivacyLevel());
        if (scope == null) {
            log.warn("无效的隐私范围: {}, userId: {}", userPrivacyDTO.getPrivacyScope(), UserContext.getUserId());
            throw new IllegalArgumentException(UserMessage.PRIVACY_SCOPE_INVALID.getMessage());
        }
        if (level == null) {
            log.warn("无效的隐私级别: {}, userId: {}", userPrivacyDTO.getPrivacyLevel(), UserContext.getUserId());
            throw new IllegalArgumentException(UserMessage.PRIVACY_LEVEL_INVALID.getMessage());
        }

        String privacyScope = scope.getValue();
        String privacyLevel = level.getValue();

        Update update = new Update()
                .setOnInsert("privacyId", IdUtil.getSnowflakeNextId())
                .setOnInsert("userId", UserContext.getUserId())
                .setOnInsert("privacyScope", privacyScope)
                .set("privacyLevel", privacyLevel);
        if (Objects.equals(privacyLevel, PrivacyLevel.SPECIFIC_USERS_ONLY.getValue())) {
            if (userPrivacyDTO.getSpecificUsers() != null && !userPrivacyDTO.getSpecificUsers().isEmpty()) {
                if (userPrivacyDTO.getSpecificUsers().size() > 20) {
                    log.warn("指定用户列表超过20个, userId: {}", UserContext.getUserId());
                    throw new IllegalArgumentException(UserMessage.SPECIFIC_USERS_TOO_MANY.getMessage());
                }
                update.set("specificUsers", userPrivacyDTO.getSpecificUsers());
            } else {
                log.warn("指定用户列表为空, userId: {}", UserContext.getUserId());
                throw new IllegalArgumentException(UserMessage.SPECIFIC_USERS_EMPTY.getMessage());
            }
        }
        TypeAlias typeAlias = UserPrivacy.class.getAnnotation(TypeAlias.class);
        if (typeAlias != null) {
            update.setOnInsert("_class", typeAlias.value());
        }
        // 保存用户隐私设置
        mongoTemplate.upsert(
                new Query(Criteria.where("userId").is(UserContext.getUserId()).and("privacyScope").is(privacyScope)),
                update,
                UserPrivacy.class
        );
    }

    @Override
    public UserPrivacyVO getPrivacyLevel(String privacyScope) {
        PrivacyScope scope = PrivacyScope.fromString(privacyScope);
        if (scope == null) {
            log.warn("无效的隐私范围: {}, userId: {}", privacyScope, UserContext.getUserId());
            throw new IllegalArgumentException(UserMessage.PRIVACY_SCOPE_INVALID.getMessage());
        }

        UserPrivacy userPrivacy = mongoTemplate.findOne(
                new Query(Criteria.where("userId").is(UserContext.getUserId()).and("privacyScope").is(privacyScope)),
                UserPrivacy.class
        );
        UserPrivacyVO userPrivacyVO;
        if (userPrivacy == null) {
            userPrivacyVO = UserPrivacyVO
                    .builder()
                    .userId(UserContext.getUserId())
                    .privacyScope(scope.getValue())
                    .privacyLevel(PrivacyLevel.PUBLIC.getValue())
                    .build();
        } else {
            userPrivacyVO = BeanUtil.copyProperties(userPrivacy, UserPrivacyVO.class);
        }

        return userPrivacyVO;
    }
}
