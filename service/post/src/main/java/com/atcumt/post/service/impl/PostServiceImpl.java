package com.atcumt.post.service.impl;

import cn.hutool.json.JSONObject;
import com.atcumt.common.utils.UserInfoUtil;
import com.atcumt.model.like.enums.LikeMessage;
import com.atcumt.model.post.dto.OnePostFeedDTO;
import com.atcumt.model.post.dto.PostFeedDTO;
import com.atcumt.model.post.enums.PostStatus;
import com.atcumt.model.post.vo.PostFeedListVO;
import com.atcumt.model.post.vo.PostFeedVO;
import com.atcumt.model.user.vo.UserInfoSimpleVO;
import com.atcumt.post.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {
    private final MongoTemplate mongoTemplate;
    private final UserInfoUtil userInfoUtil;

    @Override
    public PostFeedListVO getPosts(PostFeedDTO postFeedDTO) {
        if (postFeedDTO.getPostType() == null || postFeedDTO.getPostType().equalsIgnoreCase("all")) {

            List<PostFeedVO> posts = new ArrayList<>();

            // 虚拟线程异步执行获取不同帖子信息
            Map<String, CompletableFuture<PostFeedListVO>> futures = new HashMap<>();
            for (var postType : postFeedDTO.getLastPostIds().keySet()) {
                CompletableFuture<PostFeedListVO> future = CompletableFuture.supplyAsync(() -> getPostInfo(OnePostFeedDTO
                        .builder()
                        .postType(postType)
                        .cursor(postFeedDTO.getCursor())
                        .lastPostId(postFeedDTO.getLastPostIds().get(postType))
                        .size(postFeedDTO.getSize())
                        .build()), Executors.newVirtualThreadPerTaskExecutor());

                futures.put(postType, future);
            }
            for (var postType : futures.keySet()) {
                PostFeedListVO postFeedListVO = futures.get(postType).join();
                if (postFeedListVO.getSize() == 0) {
                    continue;
                }

                posts.addAll(postFeedListVO.getPosts());
            }

            posts.sort(Comparator.<PostFeedVO, LocalDateTime>comparing(post ->
                            post.getPostInfo().getLocalDateTime("createTime", LocalDateTime.now()),
                    Comparator.nullsLast(LocalDateTime::compareTo)).reversed());

            posts = posts.subList(0, Math.min(posts.size(), postFeedDTO.getSize()));

            LocalDateTime cursor = LocalDateTime.MIN;

            Map<String, Long> lastPostIds = new HashMap<>();

            for (PostFeedVO post : posts.reversed()) {
                if (lastPostIds.containsKey(post.getPostType())) {
                    continue;
                }
                lastPostIds.put(post.getPostType(), post.getPostId());

                LocalDateTime postCursor = post.getPostInfo().getLocalDateTime("createTime", LocalDateTime.MIN);
                if (postCursor.isAfter(cursor)) {
                    cursor = postCursor;
                }
            }

            PostFeedListVO postFeedListVO = PostFeedListVO
                    .builder()
                    .size(posts.size())
                    .cursor(cursor.toString())
                    .lastPostIds(lastPostIds)
                    .posts(posts)
                    .build();

            return postFeedListVO;
        } else {
            return getPostInfo(OnePostFeedDTO
                    .builder()
                    .postType(postFeedDTO.getPostType())
                    .cursor(postFeedDTO.getCursor())
                    .lastPostId(postFeedDTO.getLastPostIds().get(postFeedDTO.getPostType()))
                    .size(postFeedDTO.getSize())
                    .build());
        }
    }

    public PostFeedListVO getPostInfo(OnePostFeedDTO onePostFeedDTO) {
        Query query = new Query(Criteria.where("status").is(PostStatus.PUBLISHED.getCode()));
        // 后续可以根据不同的帖子类型设置
        query.fields().exclude("content");

        if (onePostFeedDTO.getCursor() != null && !onePostFeedDTO.getCursor().isEmpty()) {
            LocalDateTime cursor;
            try {
                cursor = LocalDateTime.parse(onePostFeedDTO.getCursor());
            } catch (Exception e) {
                throw new IllegalArgumentException(LikeMessage.CURSOR_FORMAT_INCORRECT.getMessage());
            }
            // 先添加筛选条件，再进行排序
            query.addCriteria(Criteria.where("createTime").lte(cursor));
        }

        // 如果有 lastPostId，添加额外的条件：筛选 postId 小于 lastPostId
        if (onePostFeedDTO.getLastPostId() != null) {
            query.addCriteria(Criteria.where("_id").lt(onePostFeedDTO.getLastPostId()));
        }

        // 排序：先按 score 排序，再按 postId 排序
        query.with(Sort.by(
                Sort.Order.desc("createTime"),
                Sort.Order.desc("_id")
        ));

        // 设置分页大小
        query.limit(onePostFeedDTO.getSize());

        List<JSONObject> posts = mongoTemplate.find(query, JSONObject.class, onePostFeedDTO.getPostType());

        List<String> userIdList = posts.stream()
                .map(post -> post.getStr("userId"))
                .distinct()
                .toList();

        List<UserInfoSimpleVO> userInfoSimpleVOs = userInfoUtil.getUserInfoSimpleBatch(userIdList);
        Map<String, UserInfoSimpleVO> userInfoSimpleVOMap;

        if (userInfoSimpleVOs != null && !userInfoSimpleVOs.isEmpty()) {
            userInfoSimpleVOMap = userInfoSimpleVOs.stream()
                    .collect(Collectors.toMap(UserInfoSimpleVO::getUserId, userInfoSimpleVO -> userInfoSimpleVO));
        } else {
            userInfoSimpleVOMap = new HashMap<>();
        }

        List<PostFeedVO> postFeedVOs = new ArrayList<>();

        for (var post : posts) {
            post.set("createTime", post.getLocalDateTime("createTime", LocalDateTime.now()).atZone(ZoneOffset.systemDefault()));
            post.set("updateTime", post.getLocalDateTime("updateTime", LocalDateTime.now()).atZone(ZoneOffset.systemDefault()));
            PostFeedVO postFeedVO = PostFeedVO
                    .builder()
                    .postType(onePostFeedDTO.getPostType())
                    .postId(post.getLong("_id"))
                    .postInfo(post)
                    .userInfo(userInfoSimpleVOMap.get(post.getStr("userId")))
                    .build();

            if (postFeedVO.getUserInfo() == null) {
                continue;
            }

            postFeedVOs.add(postFeedVO);
        }

        Long lastPostId = null;
        String cursor = null;
        if (!postFeedVOs.isEmpty()) {
            lastPostId = posts.getLast().getLong("_id");

            cursor = posts.getLast().getLocalDateTime("createTime", LocalDateTime.now()).toString();
        }

        Map<String, Long> lastPostIds = new HashMap<>();
        lastPostIds.put(onePostFeedDTO.getPostType(), lastPostId);

        PostFeedListVO postFeedListVO = PostFeedListVO
                .builder()
                .size(postFeedVOs.size())
                .cursor(cursor)
                .lastPostIds(lastPostIds)
                .posts(postFeedVOs)
                .build();

        return postFeedListVO;
    }
}
