package com.atcumt.post.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.atcumt.common.exception.AuthorizationException;
import com.atcumt.common.utils.FileConvertUtil;
import com.atcumt.common.utils.HeatScoreUtil;
import com.atcumt.common.utils.UserContext;
import com.atcumt.common.utils.UserInfoUtil;
import com.atcumt.model.auth.enums.AuthMessage;
import com.atcumt.model.common.entity.MediaFile;
import com.atcumt.model.post.dto.DiscussionDTO;
import com.atcumt.model.post.dto.DiscussionUpdateDTO;
import com.atcumt.model.post.entity.Discussion;
import com.atcumt.model.post.entity.Tag;
import com.atcumt.model.post.enums.PostMessage;
import com.atcumt.model.post.enums.PostStatus;
import com.atcumt.model.post.vo.DiscussionPostVO;
import com.atcumt.model.post.vo.DiscussionVO;
import com.atcumt.post.repository.DiscussionRepository;
import com.atcumt.post.service.DiscussionService;
import com.atcumt.post.service.TagService;
import com.atcumt.post.utils.PostReviewUtil;
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
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscussionServiceImpl implements DiscussionService {
    private final DiscussionRepository discussionRepository;
    private final MongoTemplate mongoTemplate;
    private final RocketMQTemplate rocketMQTemplate;
    private final FileConvertUtil fileConvertUtil;
    private final PostReviewUtil postReviewUtil;
    private final TagService tagService;
    private final UserInfoUtil userInfoUtil;

    @Override
    public DiscussionPostVO postDiscussion(DiscussionDTO discussionDTO) {
        // 查询tagIds对应的标签是否存在
        Query queryTag = Query.query(Criteria.where("_id").in(discussionDTO.getTagIds()));
        queryTag.fields().include("_id");
        List<Long> tagIds = mongoTemplate.find(queryTag, Tag.class).stream().map(Tag::getTagId).toList();

        discussionDTO.setTagIds(tagIds);

        Discussion discussion = Discussion
                .builder()
                .discussionId(IdUtil.getSnowflakeNextId())
                .userId(UserContext.getUserId())
                .title(discussionDTO.getTitle())
                .content(discussionDTO.getContent())
                .mediaFiles(BeanUtil.copyToList(discussionDTO.getMediaFiles(), MediaFile.class))
                .tagIds(discussionDTO.getTagIds())
                .commentCount(0)
                .likeCount(0)
                .dislikeCount(0)
                .viewCount(0L)
                .score(HeatScoreUtil.getPostHeat(0, 0, 0))
                .status(PostStatus.PUBLISHED.getCode())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        // 审核帖子内容
        postReviewUtil.review(discussion);

        discussionRepository.save(discussion);

        // 消息队列异步计算标签使用量
        tagUsageCount(discussionDTO.getTagIds());

        return DiscussionPostVO
                .builder()
                .discussionId(discussion.getDiscussionId())
                .status(discussion.getStatus())
                .build();
    }

    @Override
    public DiscussionPostVO saveDiscussionAsDraft(DiscussionDTO discussionDTO) {
        // 将tagIds去重
        Set<Long> tagIds = Set.copyOf(discussionDTO.getTagIds());
        discussionDTO.setTagIds(tagIds.stream().toList());

        Discussion discussion = Discussion
                .builder()
                .discussionId(IdUtil.getSnowflakeNextId())
                .userId(UserContext.getUserId())
                .title(discussionDTO.getTitle())
                .content(discussionDTO.getContent())
                .mediaFiles(BeanUtil.copyToList(discussionDTO.getMediaFiles(), MediaFile.class))
                .tagIds(discussionDTO.getTagIds())
                .commentCount(0)
                .likeCount(0)
                .dislikeCount(0)
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
        Update update = new Update();
        update.set("status", PostStatus.DELETED.getCode());  // 软删除
        mongoTemplate.updateFirst(
                Query.query(Criteria
                        .where("discussionId").is(discussionId)
                        .and("userId").is(loginId)
                ),
                update,
                Discussion.class
        );
    }

    @Override
    public void privateDiscussion(Long discussionId) throws AuthorizationException {
        String loginId = UserContext.getUserId();
        Update update = new Update();
        update.set("status", PostStatus.PRIVATE.getCode());  // 私密
        mongoTemplate.updateFirst(
                Query.query(Criteria
                        .where("discussionId").is(discussionId)
                        .and("userId").is(loginId)
                ),
                update,
                Discussion.class
        );
    }

    @Override
    public DiscussionVO getDiscussion(Long discussionId) {
        Discussion discussion = mongoTemplate.findOne(
                Query.query(Criteria.where("discussionId").is(discussionId)),
                Discussion.class
        );
        // 无此帖子
        if (discussion == null) {
            throw new IllegalArgumentException(PostMessage.POST_NOT_FOUND.getMessage());
        }
        // 帖子已删除
        if (Objects.equals(discussion.getStatus(), PostStatus.DELETED.getCode())) {
            throw new IllegalArgumentException(PostMessage.POST_DELETED.getMessage());
        }
        DiscussionVO discussionVO = BeanUtil.copyProperties(discussion, DiscussionVO.class, "mediaFiles", "tags");
        discussionVO.setMediaFiles(fileConvertUtil.convertToMediaFileVOs(discussion.getMediaFiles()));
        discussionVO.setTags(tagService.getSimpleTags(discussion.getTagIds()));
        discussionVO.setUserInfo(userInfoUtil.getUserInfoSimple(discussion.getUserId()));
        // 作者可见
        if (discussion.getUserId().equals(UserContext.getUserId())) {
            return discussionVO;
        }
        // 帖子未发布
        if (!Objects.equals(discussion.getStatus(), PostStatus.PUBLISHED.getCode())) {
            throw new IllegalArgumentException(PostMessage.POST_UNPUBLISHED.getMessage());
        }
        return discussionVO;
    }

    @Override
    public void pinDiscussion(Long discussionId) {
        String loginId = UserContext.getUserId();
        Discussion discussion = discussionRepository.findById(discussionId).orElse(null);

        if (discussion == null || discussion.getUserId() == null) {
            throw new IllegalArgumentException(PostMessage.POST_NOT_FOUND.getMessage());
        }
        if (!loginId.equals(discussion.getUserId())) {
            throw new AuthorizationException(AuthMessage.PERMISSION_MISMATCH.getMessage());
        }

        Update update = new Update();
        update.set("pinnedTime", LocalDateTime.now());
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("discussionId").is(discussionId)),
                update,
                Discussion.class
        );
    }

    @Override
    public void unpinDiscussion(Long discussionId) {
        String loginId = UserContext.getUserId();
        mongoTemplate.updateFirst(
                Query.query(Criteria
                        .where("discussionId").is(discussionId)
                        .and("userId").is(loginId)
                ),
                new Update().unset("pinnedTime"),
                Discussion.class
        );
    }

    @Override
    public DiscussionPostVO updateDiscussion(DiscussionUpdateDTO discussionUpdateDTO) throws AuthorizationException {
        // 审核帖子内容
        postReviewUtil.review(discussionUpdateDTO.getTitle(), discussionUpdateDTO.getContent());

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
            // 将tagIds去重
            Set<Long> tagIds = Set.copyOf(discussionUpdateDTO.getTagIds());
            discussionUpdateDTO.setTagIds(tagIds.stream().toList());

            update.set("tagIds", discussionUpdateDTO.getTagIds());
        }
        // 设置状态为审核中
        String status = PostStatus.PUBLISHED.getCode();
        update.set("status", status);
        update.set("updateTime", LocalDateTime.now());  // 更新修改时间

        // 使用MongoTemplate执行部分更新
        mongoTemplate.updateFirst(
                Query.query(Criteria
                        .where("discussionId").is(discussionUpdateDTO.getDiscussionId())
                        .and("userId").is(UserContext.getUserId())
                ),
                update,
                Discussion.class
        );

        return DiscussionPostVO
                .builder()
                .discussionId(discussionUpdateDTO.getDiscussionId())
                .status(status)
                .build();
    }

    public void tagUsageCount(List<Long> tagIds) {
        rocketMQTemplate.asyncSend("post:tagUsageCount", tagIds, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
            }

            @Override
            public void onException(Throwable e) {
                log.error("标签使用量计数消息发送失败e: {}", e.getMessage());
            }
        });
    }
}
