package com.atcumt.comment.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.atcumt.comment.repository.CommentRepository;
import com.atcumt.comment.repository.ReplyRepository;
import com.atcumt.comment.service.ReplyService;
import com.atcumt.common.api.forum.sensitive.SensitiveWordDubboService;
import com.atcumt.common.utils.FileConvertUtil;
import com.atcumt.common.utils.HeatScoreUtil;
import com.atcumt.common.utils.UserContext;
import com.atcumt.common.utils.UserInfoUtil;
import com.atcumt.model.comment.dto.*;
import com.atcumt.model.comment.entity.Comment;
import com.atcumt.model.comment.entity.Reply;
import com.atcumt.model.comment.enums.CommentMessage;
import com.atcumt.model.comment.vo.CommentReplyVO;
import com.atcumt.model.comment.vo.ReplyPlusVO;
import com.atcumt.model.comment.vo.ReplyVO;
import com.atcumt.model.comment.vo.UserReplyVO;
import com.atcumt.model.common.entity.MediaFile;
import com.atcumt.model.common.vo.MediaFileVO;
import com.atcumt.model.like.constants.LikeAction;
import com.atcumt.model.like.entity.CommentLike;
import com.atcumt.model.user.vo.UserInfoSimpleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
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
public class ReplyServiceImpl implements ReplyService {
    private final ReplyRepository replyRepository;
    private final CommentRepository commentRepository;
    private final MongoTemplate mongoTemplate;
    private final RocketMQTemplate rocketMQTemplate;
    private final UserInfoUtil userInfoUtil;
    private final FileConvertUtil fileConvertUtil;
    @DubboReference
    private SensitiveWordDubboService sensitiveWordDubboService;

    @Override
    public ReplyVO postReply(ReplyDTO replyDTO) {
        if (sensitiveWordDubboService.contains(replyDTO.getContent())) {
            throw new RuntimeException(CommentMessage.SENSITIVE_WORD.getMessage());
        }

        if (replyDTO.getMediaFiles() != null && replyDTO.getMediaFiles().size() > 1) {
            throw new RuntimeException(CommentMessage.MEDIA_FILE_LIMIT.getMessage());
        }

        List<MediaFile> mediaFiles = BeanUtil.copyToList(replyDTO.getMediaFiles(), MediaFile.class);

        Comment comment = null;
        Reply replyComment = null;

        if (replyDTO.getIsRoot()) {
            comment = commentRepository.findById(replyDTO.getReplyToId()).orElseThrow(
                    () -> new IllegalArgumentException(CommentMessage.COMMENT_NOT_FOUND.getMessage())
            );
        } else {
            replyComment = replyRepository.findById(replyDTO.getReplyToId()).orElseThrow(
                    () -> new IllegalArgumentException(CommentMessage.COMMENT_NOT_FOUND.getMessage())
            );
        }

        double score = HeatScoreUtil.getReplyHeat(System.currentTimeMillis());

        Reply reply = Reply.builder()
                .replyId(IdUtil.getSnowflakeNextId())
                .replyToId(replyDTO.getReplyToId())
                .userId(UserContext.getUserId())
                .content(replyDTO.getContent())
                .mediaFiles(mediaFiles)
                .likeCount(0)
                .dislikeCount(0)
                .score(score)
                .createTime(LocalDateTime.now())
                .build();

        if (comment != null) {
            reply.setRootCommentId(comment.getCommentId());
            reply.setPostId(comment.getPostId());
            reply.setPostType(comment.getPostType());
            reply.setReplyToUserId(comment.getUserId());
        } else if (replyComment != null) {
            reply.setRootCommentId(replyComment.getRootCommentId());
            reply.setPostId(replyComment.getPostId());
            reply.setPostType(replyComment.getPostType());
            reply.setReplyToUserId(replyComment.getUserId());
        } else {
            throw new IllegalArgumentException(CommentMessage.COMMENT_NOT_FOUND.getMessage());
        }

        // 保存回复
        replyRepository.save(reply);

        // 消息队列异步处理，更新帖子评论数
        PostCommentCountDTO postCommentCountDTO = PostCommentCountDTO.builder()
                .postId(reply.getPostId())
                .postType(reply.getPostType())
                .build();
        changePostComment(postCommentCountDTO);
        // 消息队列异步处理，更新评论回复数
        CommentReplyCountDTO commentReplyCountDTO = CommentReplyCountDTO.builder()
                .commentId(reply.getRootCommentId())
                .build();
        changeCommentReply(commentReplyCountDTO);

        String replyToUserId = reply.getReplyToUserId();

        ReplyVO replyVO = BeanUtil.copyProperties(reply, ReplyVO.class, "mediaFiles");
        replyVO.setMediaFiles(fileConvertUtil.convertToMediaFileVOs(reply.getMediaFiles()));
        replyVO.setReplyToUserId(replyToUserId);
        replyVO.setReplyToUserInfo(userInfoUtil.getUserInfoSimple(replyToUserId));

        return replyVO;
    }

    @Override
    public ReplyVO getReply(Long replyId) {
        Reply reply = replyRepository.findById(replyId).orElseThrow(
                () -> new IllegalArgumentException(CommentMessage.COMMENT_NOT_FOUND.getMessage())
        );

        ReplyVO replyVO = BeanUtil.copyProperties(reply, ReplyVO.class, "mediaFiles");
        replyVO.setMediaFiles(fileConvertUtil.convertToMediaFileVOs(reply.getMediaFiles()));

        return replyVO;
    }

    @Override
    public void deleteReply(Long replyId) {
        Reply reply = mongoTemplate.findAndRemove(
                Query.query(Criteria
                        .where("_id").is(replyId)
                        .and("userId").is(UserContext.getUserId())
                ),
                Reply.class
        );

        if (reply == null) {
            throw new IllegalArgumentException(CommentMessage.COMMENT_NOT_FOUND.getMessage());
        }

        // 消息队列异步处理，更新帖子评论数
        PostCommentCountDTO postCommentCountDTO = PostCommentCountDTO.builder()
                .postId(reply.getPostId())
                .postType(reply.getPostType())
                .build();
        changePostComment(postCommentCountDTO);
        // 消息队列异步处理，更新评论回复数
        CommentReplyCountDTO commentReplyCountDTO = CommentReplyCountDTO.builder()
                .commentId(reply.getRootCommentId())
                .build();
        changeCommentReply(commentReplyCountDTO);
    }

    @Override
    public CommentReplyVO getCommentReplies(CommentReplyDTO commentReplyDTO) {
        Query query = Query.query(Criteria.where("rootCommentId").is(commentReplyDTO.getCommentId()));

        if (commentReplyDTO.getCursor() != null) {
            double cursor;
            try {
                cursor = Double.parseDouble(commentReplyDTO.getCursor());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(CommentMessage.CURSOR_FORMAT_INCORRECT.getMessage());
            }
            // 先添加筛选条件，再进行排序
            query.addCriteria(Criteria.where("score").lte(cursor));
        }

        // 如果有 lastReplyId，添加额外的条件：筛选 replyIds 小于 lastReplyId
        if (commentReplyDTO.getLastReplyId() != null) {
            query.addCriteria(Criteria.where("replyId").lt(commentReplyDTO.getLastReplyId()));
        }

        // 排序：先按 score 排序，再按 replyIds 排序
        query.with(Sort.by(
                Sort.Order.desc("score"),
                Sort.Order.desc("replyId")
        ));

        // 设置分页大小
        query.limit(commentReplyDTO.getSize());

        List<Reply> replies = mongoTemplate.find(query, Reply.class);

        List<ReplyPlusVO> replyPlusVOs = new ArrayList<>();

        // 获取用户信息
        List<Long> replyIds = new ArrayList<>();
        // 使用Set去重
        Set<String> userIdSet = new HashSet<>();

        for (var reply : replies) {
            replyIds.add(reply.getReplyId());

            userIdSet.add(reply.getUserId());
            userIdSet.add(reply.getReplyToUserId());
        }

        userIdSet.remove(null);

        // 获取用户点赞信息
        Query likeQuery = Query.query(Criteria
                .where("userId").is(UserContext.getUserId())
                .and("commentId").in(replyIds)
        );

        List<CommentLike> likes = mongoTemplate.find(likeQuery, CommentLike.class);

        Set<Long> likeReplyIds = likes.stream()
                .filter(commentLike -> commentLike.getAction().equals(LikeAction.LIKE))
                .map(CommentLike::getCommentId)
                .collect(Collectors.toSet());

        Set<Long> dislikeReplyIds = likes.stream()
                .filter(commentLike -> commentLike.getAction().equals(LikeAction.DISLIKE))
                .map(CommentLike::getCommentId)
                .collect(Collectors.toSet());

        List<UserInfoSimpleVO> userInfoSimpleVOs = userInfoUtil.getUserInfoSimpleBatch(userIdSet.stream().toList());
        Map<String, UserInfoSimpleVO> userInfoSimpleVOMap = userInfoSimpleVOs.stream()
                .collect(Collectors.toMap(UserInfoSimpleVO::getUserId, userInfoSimpleVO -> userInfoSimpleVO));

        for (var reply : replies) {
            List<MediaFileVO> mediaFiles = fileConvertUtil.convertToMediaFileVOs(reply.getMediaFiles());

            ReplyPlusVO replyPlusVO = ReplyPlusVO
                    .builder()
                    .replyId(reply.getReplyId())
                    .replyToId(reply.getReplyToId())
                    .replyToUserId(reply.getReplyToUserId())
                    .replyToUserInfo(userInfoSimpleVOMap.get(reply.getReplyToUserId()))
                    .rootCommentId(reply.getRootCommentId())
                    .userInfo(userInfoSimpleVOMap.get(reply.getUserId()))
                    .content(reply.getContent())
                    .mediaFiles(mediaFiles)
                    .likeCount(reply.getLikeCount())
                    .dislikeCount(reply.getDislikeCount())
                    .liked(likeReplyIds.contains(reply.getReplyId()))
                    .disliked(dislikeReplyIds.contains(reply.getReplyId()))
                    .score(reply.getScore())
                    .createTime(reply.getCreateTime())
                    .build();

            replyPlusVOs.add(replyPlusVO);
        }

        Long lastReplyId = null;
        String cursor = null;
        if (!replyPlusVOs.isEmpty()) {
            lastReplyId = replies.getLast().getReplyId();

            cursor = String.valueOf(replies.getLast().getScore());
        }

        CommentReplyVO commentReplyVO = CommentReplyVO.builder()
                .commentId(commentReplyDTO.getCommentId())
                .cursor(cursor)
                .lastReplyId(lastReplyId)
                .size(replyPlusVOs.size())
                .replies(replyPlusVOs)
                .build();

        return commentReplyVO;
    }

    @Override
    public UserReplyVO getUserReplies(UserReplyDTO userReplyDTO) {
        Query query = Query.query(Criteria.where("userId").is(userReplyDTO.getUserId()));

        if (userReplyDTO.getCursor() != null) {
            LocalDateTime cursor;
            try {
                cursor = LocalDateTime.parse(userReplyDTO.getCursor());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(CommentMessage.CURSOR_FORMAT_INCORRECT.getMessage());
            }
            // 先添加筛选条件，再进行排序
            query.addCriteria(Criteria.where("createTime").lte(cursor));
        }

        // 如果有 lastReplyId，添加额外的条件：筛选 replyIds 小于 lastReplyId
        if (userReplyDTO.getLastReplyId() != null) {
            query.addCriteria(Criteria.where("replyId").lt(userReplyDTO.getLastReplyId()));
        }

        // 排序：先按 score 排序，再按 replyIds 排序
        query.with(Sort.by(
                Sort.Order.desc("createTime"),
                Sort.Order.desc("replyId")
        ));

        // 设置分页大小
        query.limit(userReplyDTO.getSize());

        List<Reply> replies = mongoTemplate.find(query, Reply.class);

        List<ReplyVO> replyVOs = new ArrayList<>();

        // 使用Set去重
        Set<String> userIdSet = new HashSet<>();
        userIdSet.add(userReplyDTO.getUserId());

        for (var reply : replies) {
            ReplyVO replyVO = BeanUtil.copyProperties(reply, ReplyVO.class, "mediaFiles");
            replyVO.setMediaFiles(fileConvertUtil.convertToMediaFileVOs(reply.getMediaFiles()));
            replyVOs.add(replyVO);

            userIdSet.add(reply.getReplyToUserId());
        }

        userIdSet.remove(null);

        List<UserInfoSimpleVO> userInfoSimpleVOs = userInfoUtil.getUserInfoSimpleBatch(userIdSet.stream().toList());
        Map<String, UserInfoSimpleVO> userInfoSimpleVOMap = userInfoSimpleVOs.stream()
                .collect(Collectors.toMap(UserInfoSimpleVO::getUserId, userInfoSimpleVO -> userInfoSimpleVO));

        for (var replyVO : replyVOs) {
            replyVO.setReplyToUserInfo(userInfoSimpleVOMap.get(replyVO.getUserId()));
        }

        Long lastReplyId = null;
        String cursor = null;
        if (!replyVOs.isEmpty()) {
            lastReplyId = replies.getLast().getReplyId();

            cursor = replies.getLast().getCreateTime().toString();
        }

        UserReplyVO userReplyVO = UserReplyVO.builder()
                .userInfo(userInfoSimpleVOMap.get(userReplyDTO.getUserId()))
                .cursor(cursor)
                .lastReplyId(lastReplyId)
                .size(replyVOs.size())
                .replies(replyVOs)
                .build();

        return userReplyVO;
    }

    @Override
    public void changePostComment(PostCommentCountDTO postCommentCountDTO) {
        rocketMQTemplate.asyncSend("comment:postComment", postCommentCountDTO, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
            }

            @Override
            public void onException(Throwable e) {
                log.error("帖子评论计数消息发送失败, e: {}", e.getMessage());
            }
        });
    }

    public void changeCommentReply(CommentReplyCountDTO commentReplyCountDTO) {
        rocketMQTemplate.asyncSend("comment:commentReply", commentReplyCountDTO, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
            }

            @Override
            public void onException(Throwable e) {
                log.error("评论回复计数消息发送失败, e: {}", e.getMessage());
            }
        });
    }
}
