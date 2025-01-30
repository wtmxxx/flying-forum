package com.atcumt.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.atcumt.common.utils.FileConvertUtil;
import com.atcumt.common.utils.UserContext;
import com.atcumt.model.auth.entity.UserAuth;
import com.atcumt.model.common.entity.MediaFile;
import com.atcumt.model.common.entity.Result;
import com.atcumt.model.oss.dto.FileInfoDTO;
import com.atcumt.model.oss.vo.FileInfoVO;
import com.atcumt.model.user.entity.UserFollow;
import com.atcumt.model.user.entity.UserInfo;
import com.atcumt.model.user.entity.UserStatus;
import com.atcumt.model.user.enums.UserMessage;
import com.atcumt.model.user.vo.UserInfoOtherVO;
import com.atcumt.model.user.vo.UserInfoVO;
import com.atcumt.user.api.client.OssClient;
import com.atcumt.user.mapper.UserAuthMapper;
import com.atcumt.user.service.UserInfoService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
    private final OssClient ossClient;
    private final FileConvertUtil fileConvertUtil;
    private final RocketMQTemplate rocketMQTemplate;

    @Override
    public UserInfoVO getMyUserInfo() throws ExecutionException, InterruptedException {
        String userId = UserContext.getUserId();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<UserInfo> userInfoFuture = executor.submit(() -> {
                UserInfo userInfo = mongoTemplate.findOne(
                        Query.query(Criteria.where("userId").is(userId)), UserInfo.class
                );

                if (userInfo != null && userInfo.getStatuses() != null && !userInfo.getStatuses().isEmpty()) {
                    List<UserStatus> userStatuses = new ArrayList<>();

                    for (var status : userInfo.getStatuses()) {
                        if (status.getEndTime().isAfter(LocalDateTime.now())) {
                            userStatuses.add(status);
                        }
                    }

                    if (userInfo.getStatuses().size() != userStatuses.size()) {
                        mongoTemplate.updateFirst(
                                Query.query(Criteria.where("userId").is(userId)),
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
            UserInfoVO userInfoVO = BeanUtil.copyProperties(userInfo, UserInfoVO.class, "avatar");
            userInfoVO.setAvatar(fileConvertUtil.convertToUrl(userInfo.getAvatar()));
            userInfoVO.setUsername(userAuthFuture.get().getUsername());

            return userInfoVO;
        }
    }

    @Override
    public UserInfoOtherVO getOtherUserInfo(String userId) throws ExecutionException, InterruptedException {
        String currentUserId = UserContext.getUserId();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<UserInfo> userInfoFuture = executor.submit(() -> {
                UserInfo userInfo = mongoTemplate.findOne(
                        Query.query(Criteria.where("userId").is(userId)), UserInfo.class
                );

                if (userInfo != null && userInfo.getStatuses() != null && !userInfo.getStatuses().isEmpty()) {
                    List<UserStatus> userStatuses = new ArrayList<>();

                    for (var status : userInfo.getStatuses()) {
                        if (status.getEndTime().isAfter(LocalDateTime.now())) {
                            userStatuses.add(status);
                        }
                    }

                    if (userInfo.getStatuses().size() != userStatuses.size()) {
                        mongoTemplate.updateFirst(
                                Query.query(Criteria.where("userId").is(currentUserId)),
                                Update.update("statuses", userStatuses), UserInfo.class
                        );
                    }

                    userInfo.setStatuses(userStatuses);
                }

                return userInfo;
            });

            Future<List<UserFollow>> userFollowFuture = executor.submit(() -> {
                return mongoTemplate.find(
                        Query.query(new Criteria().orOperator(
                                Criteria.where("followerId").is(currentUserId)
                                        .and("followedId").is(userId),
                                Criteria.where("followerId").is(userId)
                                        .and("followedId").is(currentUserId))
                        ), UserFollow.class);
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
            UserInfoOtherVO userInfoOtherVO = BeanUtil.copyProperties(userInfo, UserInfoOtherVO.class, "avatar");
            userInfoOtherVO.setAvatar(fileConvertUtil.convertToUrl(userInfo.getAvatar()));
            List<UserFollow> userFollows = userFollowFuture.get();
            if (userFollows != null && !userFollows.isEmpty()) {
                if (userFollows.size() == 2) {
                    userInfoOtherVO.setIsFollowing(true);
                    userInfoOtherVO.setIsFollower(true);
                } else if (userFollows.getFirst().getFollowerId().equals(currentUserId)) {
                    userInfoOtherVO.setIsFollowing(true);
                    userInfoOtherVO.setIsFollower(false);
                } else {
                    userInfoOtherVO.setIsFollowing(false);
                    userInfoOtherVO.setIsFollower(true);
                }
            } else {
                userInfoOtherVO.setIsFollowing(false);
                userInfoOtherVO.setIsFollower(false);
            }
            userInfoOtherVO.setUsername(userAuthFuture.get().getUsername());

            return userInfoOtherVO;
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
    public void changeAvatar(MultipartFile file) {
        Result<FileInfoVO> fileInfoResult = ossClient.uploadAvatar(file);

        if (fileInfoResult.getData() == null) throw new RuntimeException("头像上传失败");
        FileInfoVO fileInfoVO = fileInfoResult.getData();

        MediaFile mediaFile = MediaFile
                .builder()
                .bucket(fileInfoVO.getBucket())
                .fileName(fileInfoVO.getFileName())
                .fileType(fileInfoVO.getContentType())
                .build();

        Query avatarQuery = new Query(Criteria.where("userId").is(UserContext.getUserId()));
        avatarQuery.fields().include("avatar");
        UserInfo userInfo = mongoTemplate.findAndModify(
                avatarQuery,
                Update.update("avatar", mediaFile), UserInfo.class
        );
        // 删除原头像
        if (userInfo != null && userInfo.getAvatar() != null) {
            deleteFile(FileInfoDTO
                    .builder()
                    .bucket(userInfo.getAvatar().getBucket())
                    .fileName(userInfo.getAvatar().getFileName())
                    .build());
        }
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
                .filter(s -> s.getEndTime() == null || s.getEndTime().isAfter(LocalDateTime.now())) // 过滤未过期状态
                .filter(new HashSet<>()::add) // 利用HashSet的add方法去重
                .toList();

        mongoTemplate.updateFirst(
                Query.query(Criteria.where("userId").is(UserContext.getUserId())),
                Update.update("statuses", statuses), UserInfo.class
        );
    }

    public void deleteFile(FileInfoDTO fileInfoDTO) {
        rocketMQTemplate.asyncSend("oss:file-delete", fileInfoDTO, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
            }

            @Override
            public void onException(Throwable e) {
                log.error("文件删除消息发送失败e: {}", e.getMessage());
            }
        });
    }
}
