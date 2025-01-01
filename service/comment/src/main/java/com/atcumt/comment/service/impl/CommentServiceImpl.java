package com.atcumt.comment.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONObject;
import com.atcumt.comment.repository.CommentRepository;
import com.atcumt.comment.service.CommentService;
import com.atcumt.comment.service.ReplyService;
import com.atcumt.common.utils.HeatScoreUtil;
import com.atcumt.common.utils.UserContext;
import com.atcumt.common.utils.UserInfoUtil;
import com.atcumt.model.comment.dto.CommentDTO;
import com.atcumt.model.comment.dto.PostCommentCountDTO;
import com.atcumt.model.comment.dto.PostCommentDTO;
import com.atcumt.model.comment.dto.UserCommentDTO;
import com.atcumt.model.comment.entity.Comment;
import com.atcumt.model.comment.enums.CommentMessage;
import com.atcumt.model.comment.vo.CommentPlusVO;
import com.atcumt.model.comment.vo.CommentVO;
import com.atcumt.model.comment.vo.PostCommentVO;
import com.atcumt.model.comment.vo.UserCommentVO;
import com.atcumt.model.common.entity.MediaFile;
import com.atcumt.model.common.vo.MediaFileVO;
import com.atcumt.model.like.entity.CommentLike;
import com.atcumt.model.post.enums.PostMessage;
import com.atcumt.model.user.vo.UserInfoSimpleVO;
import com.github.houbb.sensitive.word.core.SensitiveWordHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final MongoTemplate mongoTemplate;
    private final UserInfoUtil userInfoUtil;
    private final ReplyService replyService;

    @Override
    public CommentVO postComment(CommentDTO commentDTO) {
        if (SensitiveWordHelper.contains(commentDTO.getContent())) {
            throw new RuntimeException(CommentMessage.SENSITIVE_WORD.getMessage());
        }

        if (commentDTO.getMediaFiles() != null && commentDTO.getMediaFiles().size() > 1) {
            throw new RuntimeException(CommentMessage.MEDIA_FILE_LIMIT.getMessage());
        }

        // 判断帖子是否存在
        Query query = Query.query(Criteria.where("_id").is(commentDTO.getPostId()));
        query.fields().include("userId");
        JSONObject post = mongoTemplate.findOne(query, JSONObject.class, commentDTO.getPostType());

        if (post == null) {
            throw new IllegalArgumentException(PostMessage.POST_NOT_FOUND.getMessage());
        }

        List<MediaFile> mediaFiles = BeanUtil.copyToList(commentDTO.getMediaFiles(), MediaFile.class);

        double score = HeatScoreUtil.getCommentHeat(0, 0, 0, System.currentTimeMillis());

        Comment comment = Comment.builder()
                .commentId(IdUtil.getSnowflakeNextId())
                .postId(commentDTO.getPostId())
                .postType(commentDTO.getPostType())
                .commentToUserId(post.getStr("userId"))
                .userId(UserContext.getUserId())
                .content(commentDTO.getContent())
                .mediaFiles(mediaFiles)
                .likeCount(0)
                .dislikeCount(0)
                .replyCount(0)
                .score(score)
                .createTime(LocalDateTime.now())
                .build();

        // 保存评论
        commentRepository.insert(comment);
        // 消息队列异步处理，更新帖子评论数
        PostCommentCountDTO postCommentCountDTO = PostCommentCountDTO.builder()
                .postId(comment.getPostId())
                .postType(comment.getPostType())
                .build();

        replyService.changePostComment(postCommentCountDTO);

        CommentVO commentVO = BeanUtil.copyProperties(comment, CommentVO.class);

        return commentVO;
    }

    @Override
    public CommentVO getComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(
                () -> new IllegalArgumentException(CommentMessage.COMMENT_NOT_FOUND.getMessage())
        );

        CommentVO commentVO = BeanUtil.copyProperties(comment, CommentVO.class);

        return commentVO;
    }

    @Override
    public void deleteComment(Long commentId) {
        Comment comment = mongoTemplate.findAndRemove(
                Query.query(Criteria
                        .where("_id").is(commentId)
                        .and("userId").is(UserContext.getUserId())
                ),
                Comment.class
        );

        if (comment == null) {
            throw new IllegalArgumentException(CommentMessage.COMMENT_NOT_FOUND.getMessage());
        }

        // 消息队列异步处理，更新帖子评论数
        PostCommentCountDTO postCommentCountDTO = PostCommentCountDTO.builder()
                .postId(comment.getPostId())
                .postType(comment.getPostType())
                .build();

        replyService.changePostComment(postCommentCountDTO);
    }

    @Override
    public PostCommentVO getPostComments(PostCommentDTO postCommentDTO) {
        Query query = Query.query(Criteria.where("postId").is(postCommentDTO.getPostId()));

        // 游标条件：score、commentIds 大于（等于）传入的游标
        switch (postCommentDTO.getSort()) {
            case "score" -> {
                if (postCommentDTO.getCursor() != null) {
                    double cursor;
                    try {
                        cursor = Double.parseDouble(postCommentDTO.getCursor());
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(CommentMessage.CURSOR_FORMAT_INCORRECT.getMessage());
                    }
                    // 先添加筛选条件，再进行排序
                    query.addCriteria(Criteria.where("score").lte(cursor));
                }

                // 如果有 lastCommentId，添加额外的条件：筛选 commentId 小于 lastCommentId
                if (postCommentDTO.getLastCommentId() != null) {
                    query.addCriteria(Criteria.where("commentId").lt(postCommentDTO.getLastCommentId()));
                }

                // 排序：先按 score 排序，再按 commentIds 排序
                query.with(Sort.by(
                        Sort.Order.desc("score"),
                        Sort.Order.desc("commentId")
                ));
            }
            case "time_desc" -> {
                if (postCommentDTO.getCursor() != null) {
                    LocalDateTime cursor;
                    try {
                        cursor = LocalDateTime.parse(postCommentDTO.getCursor());
                    } catch (Exception e) {
                        throw new IllegalArgumentException(CommentMessage.CURSOR_FORMAT_INCORRECT.getMessage());
                    }
                    // 先添加筛选条件，再进行排序
                    query.addCriteria(Criteria.where("createTime").lte(cursor));
                }

                // 如果有 lastCommentId，添加额外的条件：筛选 commentIds 小于 lastCommentId
                if (postCommentDTO.getLastCommentId() != null) {
                    query.addCriteria(Criteria.where("commentId").lt(postCommentDTO.getLastCommentId()));
                }

                // 排序：先按 score 排序，再按 commentIds 排序
                query.with(Sort.by(
                        Sort.Order.desc("createTime"),
                        Sort.Order.desc("commentId")
                ));
            }
            case "time_asc" -> {
                if (postCommentDTO.getCursor() != null) {
                    LocalDateTime cursor;
                    try {
                        cursor = LocalDateTime.parse(postCommentDTO.getCursor());
                    } catch (Exception e) {
                        throw new IllegalArgumentException(CommentMessage.CURSOR_FORMAT_INCORRECT.getMessage());
                    }
                    // 先添加筛选条件，再进行排序
                    query.addCriteria(Criteria.where("createTime").gte(cursor));
                }

                // 如果有 lastCommentId，添加额外的条件：筛选 commentId 大于 lastCommentId
                if (postCommentDTO.getLastCommentId() != null) {
                    query.addCriteria(Criteria.where("commentId").gt(postCommentDTO.getLastCommentId()));
                }

                // 排序：先按 score 排序，再按 commentIds 排序
                query.with(Sort.by(
                        Sort.Order.asc("createTime"),
                        Sort.Order.asc("commentId")
                ));
            }
            default -> throw new IllegalArgumentException(CommentMessage.SORT_NOT_SUPPORT.getMessage());
        }

        // 设置分页大小
        query.limit(postCommentDTO.getSize());

        List<Comment> comments = mongoTemplate.find(query, Comment.class);

        List<CommentPlusVO> commentPlusVOs = new ArrayList<>();

        List<Long> commentIds = new ArrayList<>();

        // 使用Set去重
        Set<String> userIdSet = new HashSet<>();

        for (var comment : comments) {
            commentIds.add(comment.getCommentId());
            userIdSet.add(comment.getUserId());
        }

        userIdSet.remove(null);

        // 获取用户点赞信息
        Query likeQuery = Query.query(Criteria
                .where("userId").is(UserContext.getUserId())
                .and("commentId").in(commentIds)
        );
        likeQuery.fields().include("commentId", "isLike");

        List<CommentLike> likes = mongoTemplate.find(likeQuery, CommentLike.class);

        Set<Long> likeCommentIds = likes.stream()
                .filter(CommentLike::getIsLike)
                .map(CommentLike::getCommentId)
                .collect(Collectors.toSet());

        Set<Long> dislikeCommentIds = likes.stream()
                .filter(commentLike -> !commentLike.getIsLike())
                .map(CommentLike::getCommentId)
                .collect(Collectors.toSet());

        List<UserInfoSimpleVO> userInfoSimpleVOs = userInfoUtil.getUserInfoSimpleBatch(userIdSet.stream().toList());
        Map<String, UserInfoSimpleVO> userInfoSimpleVOMap = userInfoSimpleVOs.stream()
                .collect(Collectors.toMap(UserInfoSimpleVO::getUserId, userInfoSimpleVO -> userInfoSimpleVO));

        for (var comment : comments) {
            List<MediaFileVO> mediaFiles = new ArrayList<>();
            if (comment.getMediaFiles() != null) {
                for (var mediaFile : comment.getMediaFiles()) {
                    MediaFileVO mediaFileVO = MediaFileVO
                            .builder()
                            .url(mediaFile.getUrl())
                            .bucket(mediaFile.getBucket())
                            .fileName(mediaFile.getFileName())
                            .customName(mediaFile.getCustomName())
                            .description(mediaFile.getDescription())
                            .fileType(mediaFile.getFileType())
                            .build();
                    mediaFiles.add(mediaFileVO);
                }
            }

            CommentPlusVO commentPlusVO = CommentPlusVO
                    .builder()
                    .commentId(comment.getCommentId())
                    .commentToUserId(comment.getCommentToUserId())
                    .userInfo(userInfoSimpleVOMap.get(comment.getUserId()))
                    .content(comment.getContent())
                    .mediaFiles(mediaFiles)
                    .likeCount(comment.getLikeCount())
                    .dislikeCount(comment.getDislikeCount())
                    .liked(likeCommentIds.contains(comment.getCommentId()))
                    .disliked(dislikeCommentIds.contains(comment.getCommentId()))
                    .replyCount(comment.getReplyCount())
                    .score(comment.getScore())
                    .createTime(comment.getCreateTime())
                    .build();

            commentPlusVOs.add(commentPlusVO);
        }

        Long lastCommentId = null;
        String cursor = null;
        if (!commentPlusVOs.isEmpty()) {
            lastCommentId = comments.getLast().getCommentId();

            cursor = switch (postCommentDTO.getSort()) {
                case "score" -> String.valueOf(comments.getLast().getScore());
                case "time_desc", "time_asc" -> comments.getLast().getCreateTime().toString();
                default -> null;
            };
        }

        PostCommentVO postCommentVO = PostCommentVO.builder()
                .postId(postCommentDTO.getPostId())
                .postType(postCommentDTO.getPostType())
                .size(comments.size())
                .sort(postCommentDTO.getSort())
                .cursor(cursor)
                .lastCommentId(lastCommentId)
                .comments(commentPlusVOs)
                .build();

        return postCommentVO;
    }

    @Override
    public UserCommentVO getUserComments(UserCommentDTO userCommentDTO) {
        String userId = userCommentDTO.getUserId();

        Query query = Query.query(Criteria.where("userId").is(userCommentDTO.getUserId()));

        if (userCommentDTO.getCursor() != null) {
            LocalDateTime cursor;
            try {
                cursor = LocalDateTime.parse(userCommentDTO.getCursor());
            } catch (Exception e) {
                throw new IllegalArgumentException(CommentMessage.CURSOR_FORMAT_INCORRECT.getMessage());
            }
            // 先添加筛选条件，再进行排序
            query.addCriteria(Criteria.where("createTime").lte(cursor));
        }

        // 如果有 lastCommentId，添加额外的条件：筛选 commentId 小于 lastCommentId
        if (userCommentDTO.getLastCommentId() != null) {
            query.addCriteria(Criteria.where("commentId").lt(userCommentDTO.getLastCommentId()));
        }

        // 排序：先按 score 排序，再按 commentId 排序
        query.with(Sort.by(
                Sort.Order.desc("createTime"),
                Sort.Order.desc("commentId")
        ));

        // 设置分页大小
        query.limit(userCommentDTO.getSize());

        List<Comment> comments = mongoTemplate.find(query, Comment.class);

        List<CommentVO> commentVOs = new ArrayList<>();

        for (var comment : comments) {
            CommentVO commentVO = BeanUtil.copyProperties(comment, CommentVO.class);

            commentVOs.add(commentVO);
        }

        Long lastCommentId = null;
        String cursor = null;
        if (!commentVOs.isEmpty()) {
            lastCommentId = comments.getLast().getCommentId();

            cursor = comments.getLast().getCreateTime().toString();
        }

        UserCommentVO userCommentVO = UserCommentVO.builder()
                .userInfo(userInfoUtil.getUserInfoSimple(userId))
                .size(comments.size())
                .cursor(cursor)
                .lastCommentId(lastCommentId)
                .comments(commentVOs)
                .build();

        return userCommentVO;
    }
}
