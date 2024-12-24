package com.atcumt.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.atcumt.common.utils.UserContext;
import com.atcumt.model.user.entity.UserInfo;
import com.atcumt.model.user.vo.UserInfoVO;
import com.atcumt.user.service.UserInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserInfoServiceImpl implements UserInfoService {
    private final MongoTemplate mongoTemplate;

    @Override
    public UserInfoVO getUserInfo(String userId) {
        UserInfo userInfo = mongoTemplate.findOne(
                Query.query(Criteria.where("userId").is(userId)), UserInfo.class
        );

        return BeanUtil.toBean(userInfo, UserInfoVO.class);
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
    public void changeBanner(String banner) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("userId").is(UserContext.getUserId())),
                Update.update("banner", banner), UserInfo.class
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
    public void changeStatus(List<String> status) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("userId").is(UserContext.getUserId())),
                Update.update("status", status), UserInfo.class
        );
    }
}
