package com.atcumt.forum.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.atcumt.common.exception.AuthorizationException;
import com.atcumt.common.utils.UserContext;
import com.atcumt.forum.repository.DiscussionRepository;
import com.atcumt.forum.service.DiscussionService;
import com.atcumt.model.common.AuthMessage;
import com.atcumt.model.forum.dto.DiscussionDTO;
import com.atcumt.model.forum.dto.DiscussionUpdateDTO;
import com.atcumt.model.forum.entity.Discussion;
import com.atcumt.model.forum.entity.MediaFile;
import com.atcumt.model.forum.enums.PostStatus;
import com.atcumt.model.forum.vo.DiscussionPostVO;
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
    public void deleteDiscussion(Long discussionId) {
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
    public void privateDiscussion(Long discussionId) {
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
    public DiscussionPostVO updateDiscussion(DiscussionUpdateDTO discussionUpdateDTO) {
        String authorId = discussionRepository.findAuthorIdByDiscussionId(discussionUpdateDTO.getDiscussionId()).getAuthorId();
        if (authorId == null || !authorId.equals(UserContext.getUserId())) {
            throw new AuthorizationException(AuthMessage.PERMISSION_MISMATCH.getMessage());
        }

        // 创建Update对象，只更新非null字段
        Update update = new Update();

        Integer status = discussionUpdateDTO.getIsDraft() ? 0 : 1;

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
        if (discussionUpdateDTO.getIsDraft() != null) {
            update.set("status", status);
        } else {
            update.set("createTime", LocalDateTime.now());  // 更新创建时间
        }

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
