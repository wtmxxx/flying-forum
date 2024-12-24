package com.atcumt.post.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.atcumt.common.exception.AuthorizationException;
import com.atcumt.common.utils.UserContext;
import com.atcumt.model.common.entity.MediaFile;
import com.atcumt.model.common.enums.AuthMessage;
import com.atcumt.model.post.dto.DiscussionDTO;
import com.atcumt.model.post.dto.DiscussionUpdateDTO;
import com.atcumt.model.post.entity.Discussion;
import com.atcumt.model.post.enums.PostStatus;
import com.atcumt.model.post.vo.DiscussionPostVO;
import com.atcumt.model.post.vo.DiscussionVO;
import com.atcumt.post.repository.DiscussionRepository;
import com.atcumt.post.service.DiscussionService;
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

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscussionServiceImpl implements DiscussionService {
    private final DiscussionRepository discussionRepository;
    private final MongoTemplate mongoTemplate;
    private final RocketMQTemplate rocketMQTemplate;

    @Override
    public DiscussionPostVO postDiscussion(DiscussionDTO discussionDTO) {
        Discussion discussion = Discussion
                .builder()
                .discussionId(IdUtil.getSnowflakeNextId())
                .authorId(UserContext.getUserId())
                .title(discussionDTO.getTitle())
                .content(discussionDTO.getContent())
                .mediaFiles(BeanUtil.copyToList(discussionDTO.getMediaFiles(), MediaFile.class))
                .tagIds(discussionDTO.getTagIds())
                .commentCount(0)
                .likeCount(0)
                .viewCount(0L)
                .status(PostStatus.UNDER_REVIEW.getCode())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        discussionRepository.save(discussion);

        // 消息队列异步审核帖子
        asyncReviewDiscussion(discussion.getDiscussionId());

        return DiscussionPostVO
                .builder()
                .discussionId(discussion.getDiscussionId())
                .status(discussion.getStatus())
                .build();
    }

    @Override
    public DiscussionPostVO saveDiscussionAsDraft(DiscussionDTO discussionDTO) {
        Discussion discussion = Discussion
                .builder()
                .discussionId(IdUtil.getSnowflakeNextId())
                .authorId(UserContext.getUserId())
                .title(discussionDTO.getTitle())
                .content(discussionDTO.getContent())
                .mediaFiles(BeanUtil.copyToList(discussionDTO.getMediaFiles(), MediaFile.class))
                .tagIds(discussionDTO.getTagIds())
                .commentCount(0)
                .likeCount(0)
                .viewCount(0L)
                .status(PostStatus.DRAFT.getCode())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        discussionRepository.save(discussion);

        return DiscussionPostVO
                .builder()
                .discussionId(discussion.getDiscussionId())
                .status(discussion.getStatus())
                .build();
    }

    @Override
    public void deleteDiscussion(Long discussionId) throws AuthorizationException {
        String loginId = UserContext.getUserId();
        Discussion discussion = discussionRepository.findById(discussionId).orElse(null);

        if (discussion == null || discussion.getAuthorId() == null) {
            throw new IllegalArgumentException("无此帖子");
        }
        if (!loginId.equals(discussion.getAuthorId())) {
            throw new AuthorizationException(AuthMessage.PERMISSION_MISMATCH.getMessage());
        }

        Update update = new Update();
        update.set("status", PostStatus.DELETED.getCode());  // 软删除
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("discussionId").is(discussionId)),
                update,
                Discussion.class
        );
    }

    @Override
    public void privateDiscussion(Long discussionId) throws AuthorizationException {
        String loginId = UserContext.getUserId();
        Discussion discussion = discussionRepository.findById(discussionId).orElse(null);

        if (discussion == null || discussion.getAuthorId() == null) {
            throw new IllegalArgumentException("无此帖子");
        }
        if (!loginId.equals(discussion.getAuthorId())) {
            throw new AuthorizationException(AuthMessage.PERMISSION_MISMATCH.getMessage());
        }

        Update update = new Update();
        update.set("status", PostStatus.PRIVATE.getCode());  // -1 软删除
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("discussionId").is(discussionId)),
                update,
                Discussion.class
        );
    }

    @Override
    public DiscussionVO getDiscussion(Long discussionId) {
        Discussion discussion = discussionRepository.findById(discussionId).orElse(null);
        // 无此帖子
        if (discussion == null) {
            throw new IllegalArgumentException("无此帖子");
        }
        // 作者可见
        if (discussion.getAuthorId().equals(UserContext.getUserId())) {
            return BeanUtil.copyProperties(discussion, DiscussionVO.class);
        }
        // 帖子未发布
        if (discussion.getStatus() != PostStatus.PUBLISHED.getCode()) {
            throw new IllegalArgumentException("帖子未发布");
        }
        return BeanUtil.copyProperties(discussion, DiscussionVO.class);
    }

    @Override
    public DiscussionPostVO updateDiscussion(DiscussionUpdateDTO discussionUpdateDTO) throws AuthorizationException {
        String authorId = discussionRepository.findAuthorIdByDiscussionId(discussionUpdateDTO.getDiscussionId()).getAuthorId();
        if (authorId == null || !authorId.equals(UserContext.getUserId())) {
            throw new AuthorizationException(AuthMessage.PERMISSION_MISMATCH.getMessage());
        }

        // 创建Update对象，只更新非null字段
        Update update = new Update();

        if (discussionUpdateDTO.getTitle() != null) {
            update.set("title", discussionUpdateDTO.getTitle());
        }
        if (discussionUpdateDTO.getContent() != null) {
            update.set("content", discussionUpdateDTO.getContent());
        }
        if (discussionUpdateDTO.getMediaFiles() != null) {
            update.set("mediaFiles", discussionUpdateDTO.getMediaFiles());
        }
        if (discussionUpdateDTO.getTagIds() != null) {
            update.set("tagIds", discussionUpdateDTO.getTagIds());
        }
        // 设置状态为审核中
        Integer status = PostStatus.UNDER_REVIEW.getCode();
        update.set("status", status);
        update.set("updateTime", LocalDateTime.now());  // 更新修改时间

        // 使用MongoTemplate执行部分更新
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("discussionId").is(discussionUpdateDTO.getDiscussionId())),
                update,
                Discussion.class
        );

        // 消息队列异步审核帖子
        asyncReviewDiscussion(discussionUpdateDTO.getDiscussionId());

        return DiscussionPostVO
                .builder()
                .discussionId(discussionUpdateDTO.getDiscussionId())
                .status(status)
                .build();
    }


    public void asyncReviewDiscussion(Long discussionId) {
        rocketMQTemplate.asyncSend("forum:discussionReview", discussionId, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("帖子审核任务下发成功");
            }

            @Override
            public void onException(Throwable throwable) {
                log.info("帖子审核任务下发失败 {}", throwable.getMessage());
            }
        });
    }
}
