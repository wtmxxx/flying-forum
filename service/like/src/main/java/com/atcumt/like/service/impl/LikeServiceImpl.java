package com.atcumt.like.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONObject;
import com.atcumt.common.exception.AuthorizationException;
import com.atcumt.common.utils.UserContext;
import com.atcumt.common.utils.UserInfoUtil;
import com.atcumt.common.utils.UserPrivacyUtil;
import com.atcumt.like.service.LikeService;
import com.atcumt.model.auth.enums.AuthMessage;
import com.atcumt.model.common.enums.ResultCode;
import com.atcumt.model.like.constants.LikeAction;
import com.atcumt.model.like.dto.*;
import com.atcumt.model.like.entity.CommentLike;
import com.atcumt.model.like.entity.PostLike;
import com.atcumt.model.like.enums.LikeMessage;
import com.atcumt.model.like.vo.PostLikeVO;
import com.atcumt.model.like.vo.PostUserLikeVO;
import com.atcumt.model.like.vo.UserLikeVO;
import com.atcumt.model.like.vo.UserPostLikeVO;
import com.atcumt.model.post.enums.PostMessage;
import com.atcumt.model.post.enums.PostStatus;
import com.atcumt.model.user.enums.PrivacyScope;
import com.atcumt.model.user.enums.UserMessage;
import com.atcumt.model.user.vo.UserInfoSimpleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LikeServiceImpl implements LikeService {
    private final RocketMQTemplate rocketMQTemplate;
    private final MongoTemplate mongoTemplate;
    private final UserPrivacyUtil userPrivacyUtil;
    private final UserInfoUtil userInfoUtil;

    @Override
    public void likePost(PostLikeDTO postLikeDTO) {
        // 取消点赞/点踩
        if (postLikeDTO.getAction().equals(LikeAction.CANCEL)) {
            Query query = new Query(Criteria
                    .where("postType").is(postLikeDTO.getPostType())
                    .and("postId").is(postLikeDTO.getPostId())
                    .and("userId").is(postLikeDTO.getUserId())
            );
            mongoTemplate.remove(query, PostLike.class);
        } else if (postLikeDTO.getAction().equals(LikeAction.LIKE) || postLikeDTO.getAction().equals(LikeAction.DISLIKE)) {
            // 点赞/点踩
            PostLike postLike = PostLike.builder()
                    .likeId(IdUtil.getSnowflakeNextId())
                    .action(postLikeDTO.getAction())
                    .postType(postLikeDTO.getPostType())
                    .postId(postLikeDTO.getPostId())
                    .userId(postLikeDTO.getUserId())
                    .createTime(LocalDateTime.now())
                    .build();

            Query query = new Query(Criteria
                    .where("postType").is(postLikeDTO.getPostType())
                    .and("postId").is(postLikeDTO.getPostId())
                    .and("userId").is(postLikeDTO.getUserId())
            );
            Update update = new Update().set("action", postLikeDTO.getAction())
                    .set("createTime", LocalDateTime.now());
            PostLike oldPostLike = mongoTemplate.findAndModify(query, update, PostLike.class);

            if (oldPostLike == null) {
                mongoTemplate.insert(postLike);
            }
        }

        // 发送帖子点赞消息
        PostLikeCountDTO postLikeCountDTO = PostLikeCountDTO.builder()
                .postType(postLikeDTO.getPostType())
                .postId(postLikeDTO.getPostId())
                .build();
        postLike(postLikeCountDTO);
    }

    @Override
    public void likeComment(CommentLikeDTO commentLikeDTO) {
        // 取消点赞/点踩
        if (commentLikeDTO.getAction().equals(LikeAction.CANCEL)) {
            Query query = new Query(Criteria
                    .where("commentType").is(commentLikeDTO.getCommentType())
                    .and("commentId").is(commentLikeDTO.getCommentId())
                    .and("userId").is(commentLikeDTO.getUserId())
            );
            mongoTemplate.remove(query, CommentLike.class);
        } else if (commentLikeDTO.getAction().equals(LikeAction.LIKE) || commentLikeDTO.getAction().equals(LikeAction.DISLIKE)) {
            // 点赞/点踩
            CommentLike commentLike = CommentLike.builder()
                    .likeId(IdUtil.getSnowflakeNextId())
                    .action(commentLikeDTO.getAction())
                    .commentType(commentLikeDTO.getCommentType())
                    .commentId(commentLikeDTO.getCommentId())
                    .userId(commentLikeDTO.getUserId())
                    .createTime(LocalDateTime.now())
                    .build();

            Query query = new Query(Criteria
                    .where("commentType").is(commentLikeDTO.getCommentType())
                    .and("commentId").is(commentLikeDTO.getCommentId())
                    .and("userId").is(commentLikeDTO.getUserId())
            );
            Update update = new Update().set("action", commentLikeDTO.getAction())
                    .set("createTime", LocalDateTime.now());
            CommentLike oldCommentLike = mongoTemplate.findAndModify(query, update, CommentLike.class);

            if (oldCommentLike == null) {
                mongoTemplate.insert(commentLike);
            }
        }

        // 发送评论点赞消息
        CommentLikeCountDTO commentLikeCountDTO = CommentLikeCountDTO.builder()
                .commentType(commentLikeDTO.getCommentType())
                .commentId(commentLikeDTO.getCommentId())
                .build();
        commentLike(commentLikeCountDTO);
    }

    @Override
    public UserPostLikeVO getUserLikes(UserLikeDTO userLikeDTO) throws InterruptedException {
        String userId = userLikeDTO.getUserId();
        // 检查用户是否有权限访问点赞列表
        if (!userId.equals(UserContext.getUserId()) && !userPrivacyUtil.checkPrivacy(userId, PrivacyScope.LIKE)) {
            throw new AuthorizationException(UserMessage.LIKE_PRIVACY_DENIED.getMessage());
        }

        Query query = Query.query(Criteria
                .where("userId").is(userId)
                .and("action").is(LikeAction.LIKE));

        if (userLikeDTO.getCursor() != null) {
            LocalDateTime cursor;
            try {
                cursor = LocalDateTime.parse(userLikeDTO.getCursor());
            } catch (Exception e) {
                throw new IllegalArgumentException(LikeMessage.CURSOR_FORMAT_INCORRECT.getMessage());
            }
            // 先添加筛选条件，再进行排序
            query.addCriteria(Criteria.where("createTime").lte(cursor));
        }

        // 如果有 lastLikeId，添加额外的条件：筛选 likeId 小于 lastLikeId
        if (userLikeDTO.getLastLikeId() != null) {
            query.addCriteria(Criteria.where("likeId").lt(userLikeDTO.getLastLikeId()));
        }

        // 排序：先按 score 排序，再按 likeId 排序
        query.with(Sort.by(
                Sort.Order.desc("createTime"),
                Sort.Order.desc("likeId")
        ));

        // 设置分页大小
        query.limit(userLikeDTO.getSize());

        List<PostLike> postLikes = mongoTemplate.find(query, PostLike.class);

        // 创建虚拟线程池
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        // 按 postType 分组
        Map<String, List<PostLike>> groupedByPostType = postLikes.stream()
                .collect(Collectors.groupingBy(PostLike::getPostType));

        // 结果存储
        Map<Long, JSONObject> postMap = new ConcurrentHashMap<>();

        // 批量处理分组
        for (Map.Entry<String, List<PostLike>> entry : groupedByPostType.entrySet()) {
            String postType = entry.getKey();
            List<PostLike> likes = entry.getValue();

            executor.submit(() -> {
                try {
                    // 构建查询条件
                    List<Long> postIds = likes.stream()
                            .map(PostLike::getPostId)
                            .toList();

                    Query postQuery = Query.query(Criteria
                            .where("_id").in(postIds)
                            .and("status").is(PostStatus.PUBLISHED.getCode())
                    );
                    postQuery.fields().exclude("content");

                    // 查询数据
                    List<JSONObject> posts = mongoTemplate.find(postQuery, JSONObject.class, postType);

                    // 按 postId 存储结果
                    posts.forEach(post -> {
                        Long postId = post.getLong("_id");
                        post.set("createTime", post.getLocalDateTime("createTime", LocalDateTime.now()).atZone(ZoneOffset.systemDefault()));
                        post.set("updateTime", post.getLocalDateTime("updateTime", LocalDateTime.now()).atZone(ZoneOffset.systemDefault()));
                        postMap.put(postId, post);
                    });
                } catch (Exception e) {
                    log.error("处理帖子类型时发生错误，postType: {}", postType, e);
                    throw new RuntimeException(ResultCode.INTERNAL_SERVER_ERROR.getMessage());
                }
            });
        }

        // 关闭线程池，等待所有任务完成
        executor.shutdown();
        boolean finished = executor.awaitTermination(30, TimeUnit.SECONDS);
        if (!finished) {
            log.warn("获取帖子任务未能在指定时间内完成");
            throw new RuntimeException(ResultCode.INTERNAL_SERVER_ERROR.getMessage());
        }


        List<PostLikeVO> postLikeVOs = new ArrayList<>();

        for (var postLike : postLikes) {
            PostLikeVO postLikeVO = BeanUtil.copyProperties(postLike, PostLikeVO.class);

            postLikeVO.setPostInfo(postMap.get(postLike.getPostId()));

            if (postLikeVO.getPostInfo() == null) {
                continue;
            }

            postLikeVOs.add(postLikeVO);
        }

        Long lastLikeId = null;
        String cursor = null;
        if (!postLikeVOs.isEmpty()) {
            lastLikeId = postLikes.getLast().getLikeId();

            cursor = postLikes.getLast().getCreateTime().toString();
        }

        UserPostLikeVO userPostLikeVO = UserPostLikeVO.builder()
                .size(postLikeVOs.size())
                .cursor(cursor)
                .lastLikeId(lastLikeId)
                .likes(postLikeVOs)
                .build();
        return userPostLikeVO;
    }

    @Override
    public PostUserLikeVO getPostLikes(PostUserLikeDTO postUserLikeDTO) {
        String userId = UserContext.getUserId();

        // TODO 优化：使用Redis缓存帖子信息
        // 检查用户是否为帖子作者
        Query authorQuery = Query.query(Criteria.where("_id").is(postUserLikeDTO.getPostId()));
        authorQuery.fields().include("userId");
        JSONObject jsonObject = mongoTemplate.findOne(
                Query.query(Criteria
                        .where("_id").is(postUserLikeDTO.getPostId())),
                JSONObject.class,
                postUserLikeDTO.getPostType()
        );
        if (jsonObject == null) {
            throw new IllegalArgumentException(PostMessage.POST_NOT_FOUND.getMessage());
        }
        String authorId = jsonObject.getStr("userId", null);
        if (!Objects.equals(authorId, userId)) {
            throw new AuthorizationException(AuthMessage.PERMISSION_MISMATCH.getMessage());
        }

        Query query = Query.query(Criteria
                .where("postType").is(postUserLikeDTO.getPostType())
                .and("postId").is(postUserLikeDTO.getPostId())
                .and("action").is(LikeAction.LIKE)
        );

        if (postUserLikeDTO.getCursor() != null) {
            LocalDateTime cursor;
            try {
                cursor = LocalDateTime.parse(postUserLikeDTO.getCursor());
            } catch (Exception e) {
                throw new IllegalArgumentException(LikeMessage.CURSOR_FORMAT_INCORRECT.getMessage());
            }
            // 先添加筛选条件，再进行排序
            query.addCriteria(Criteria.where("createTime").lte(cursor));
        }

        // 如果有 lastLikeId，添加额外的条件：筛选 likeId 小于 lastLikeId
        if (postUserLikeDTO.getLastLikeId() != null) {
            query.addCriteria(Criteria.where("likeId").lt(postUserLikeDTO.getLastLikeId()));
        }

        // 排序：先按 score 排序，再按 likeId 排序
        query.with(Sort.by(
                Sort.Order.desc("createTime"),
                Sort.Order.desc("likeId")
        ));

        // 设置分页大小
        query.limit(postUserLikeDTO.getSize());

        List<PostLike> postLikes = mongoTemplate.find(query, PostLike.class);

        List<String> userIdList = postLikes.stream()
                .map(PostLike::getUserId)
                .toList();

        List<UserInfoSimpleVO> userInfoSimpleVOs = userInfoUtil.getUserInfoSimpleBatch(userIdList);
        Map<String, UserInfoSimpleVO> userInfoSimpleVOMap = userInfoSimpleVOs.stream()
                .collect(Collectors.toMap(UserInfoSimpleVO::getUserId, userInfoSimpleVO -> userInfoSimpleVO));

        List<UserLikeVO> userLikeVOs = new ArrayList<>();

        for (var postLike : postLikes) {
            UserLikeVO userLikeVO = UserLikeVO.builder()
                    .likeId(postLike.getLikeId())
                    .userId(postLike.getUserId())
                    .userInfo(userInfoSimpleVOMap.get(postLike.getUserId()))
                    .createTime(postLike.getCreateTime())
                    .build();

            if (userLikeVO.getUserInfo() == null) {
                continue;
            }

            userLikeVOs.add(userLikeVO);
        }

        Long lastLikeId = null;
        String cursor = null;
        if (!userLikeVOs.isEmpty()) {
            lastLikeId = postLikes.getLast().getLikeId();

            cursor = postLikes.getLast().getCreateTime().toString();
        }

        PostUserLikeVO postUserLikeVO = PostUserLikeVO.builder()
                .size(userLikeVOs.size())
                .cursor(cursor)
                .lastLikeId(lastLikeId)
                .likes(userLikeVOs)
                .build();
        return postUserLikeVO;
    }

    public void postLike(PostLikeCountDTO postLikeCountDTO) {
        rocketMQTemplate.asyncSend("like:postLike", postLikeCountDTO, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
            }

            @Override
            public void onException(Throwable e) {
                log.error("帖子点赞消息发送失败e: {}", e.getMessage());
            }
        });
    }

    public void commentLike(CommentLikeCountDTO commentLikeCountDTO) {
        rocketMQTemplate.asyncSend("like:commentLike", commentLikeCountDTO, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
            }

            @Override
            public void onException(Throwable e) {
                log.error("评论点赞消息发送失败e: {}", e.getMessage());
            }
        });
    }
}