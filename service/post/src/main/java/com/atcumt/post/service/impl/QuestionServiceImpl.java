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
import com.atcumt.model.post.dto.QuestionDTO;
import com.atcumt.model.post.dto.QuestionUpdateDTO;
import com.atcumt.model.post.entity.Question;
import com.atcumt.model.post.entity.Tag;
import com.atcumt.model.post.enums.PostMessage;
import com.atcumt.model.post.enums.PostStatus;
import com.atcumt.model.post.vo.QuestionPostVO;
import com.atcumt.model.post.vo.QuestionVO;
import com.atcumt.post.repository.QuestionRepository;
import com.atcumt.post.service.QuestionService;
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
public class QuestionServiceImpl implements QuestionService {
    private final QuestionRepository questionRepository;
    private final MongoTemplate mongoTemplate;
    private final RocketMQTemplate rocketMQTemplate;
    private final FileConvertUtil fileConvertUtil;
    private final PostReviewUtil postReviewUtil;
    private final TagService tagService;
    private final UserInfoUtil userInfoUtil;

    @Override
    public QuestionPostVO postQuestion(QuestionDTO questionDTO) {
        // 查询tagIds对应的标签是否存在
        Query queryTag = Query.query(Criteria.where("_id").in(questionDTO.getTagIds()));
        queryTag.fields().include("_id");
        List<Long> tagIds = mongoTemplate.find(queryTag, Tag.class).stream().map(Tag::getTagId).toList();

        questionDTO.setTagIds(tagIds);

        Question question = Question
                .builder()
                .questionId(IdUtil.getSnowflakeNextId())
                .userId(UserContext.getUserId())
                .title(questionDTO.getTitle())
                .content(questionDTO.getContent())
                .mediaFiles(BeanUtil.copyToList(questionDTO.getMediaFiles(), MediaFile.class))
                .tagIds(questionDTO.getTagIds())
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
        postReviewUtil.review(question);

        questionRepository.save(question);

        // 消息队列异步计算标签使用量
        tagUsageCount(questionDTO.getTagIds());

        return QuestionPostVO
                .builder()
                .questionId(question.getQuestionId())
                .status(question.getStatus())
                .build();
    }

    @Override
    public QuestionPostVO saveQuestionAsDraft(QuestionDTO questionDTO) {
        // 将tagIds去重
        Set<Long> tagIds = Set.copyOf(questionDTO.getTagIds());
        questionDTO.setTagIds(tagIds.stream().toList());

        Question question = Question
                .builder()
                .questionId(IdUtil.getSnowflakeNextId())
                .userId(UserContext.getUserId())
                .title(questionDTO.getTitle())
                .content(questionDTO.getContent())
                .mediaFiles(BeanUtil.copyToList(questionDTO.getMediaFiles(), MediaFile.class))
                .tagIds(questionDTO.getTagIds())
                .commentCount(0)
                .likeCount(0)
                .dislikeCount(0)
                .viewCount(0L)
                .status(PostStatus.DRAFT.getCode())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        questionRepository.save(question);

        return QuestionPostVO
                .builder()
                .questionId(question.getQuestionId())
                .status(question.getStatus())
                .build();
    }

    @Override
    public void deleteQuestion(Long questionId) throws AuthorizationException {
        String loginId = UserContext.getUserId();
        Update update = new Update();
        update.set("status", PostStatus.DELETED.getCode());  // 软删除
        mongoTemplate.updateFirst(
                Query.query(Criteria
                        .where("questionId").is(questionId)
                        .and("userId").is(loginId)
                ),
                update,
                Question.class
        );
    }

    @Override
    public void privateQuestion(Long questionId) throws AuthorizationException {
        String loginId = UserContext.getUserId();
        Update update = new Update();
        update.set("status", PostStatus.PRIVATE.getCode());  // 私密
        mongoTemplate.updateFirst(
                Query.query(Criteria
                        .where("questionId").is(questionId)
                        .and("userId").is(loginId)
                ),
                update,
                Question.class
        );
    }

    @Override
    public QuestionVO getQuestion(Long questionId) {
        Question question = mongoTemplate.findOne(
                Query.query(Criteria.where("questionId").is(questionId)),
                Question.class
        );
        // 无此帖子
        if (question == null) {
            throw new IllegalArgumentException(PostMessage.POST_NOT_FOUND.getMessage());
        }
        // 帖子已删除
        if (Objects.equals(question.getStatus(), PostStatus.DELETED.getCode())) {
            throw new IllegalArgumentException(PostMessage.POST_DELETED.getMessage());
        }
        QuestionVO questionVO = BeanUtil.copyProperties(question, QuestionVO.class, "mediaFiles", "tags");
        questionVO.setMediaFiles(fileConvertUtil.convertToMediaFileVOs(question.getMediaFiles()));
        questionVO.setTags(tagService.getSimpleTags(question.getTagIds()));
        questionVO.setUserInfo(userInfoUtil.getUserInfoSimple(question.getUserId()));
        // 作者可见
        if (question.getUserId().equals(UserContext.getUserId())) {
            return questionVO;
        }
        // 帖子未发布
        if (!Objects.equals(question.getStatus(), PostStatus.PUBLISHED.getCode())) {
            throw new IllegalArgumentException(PostMessage.POST_UNPUBLISHED.getMessage());
        }
        return questionVO;
    }

    @Override
    public void pinQuestion(Long questionId) {
        String loginId = UserContext.getUserId();
        Question question = questionRepository.findById(questionId).orElse(null);

        if (question == null || question.getUserId() == null) {
            throw new IllegalArgumentException(PostMessage.POST_NOT_FOUND.getMessage());
        }
        if (!loginId.equals(question.getUserId())) {
            throw new AuthorizationException(AuthMessage.PERMISSION_MISMATCH.getMessage());
        }

        Update update = new Update();
        update.set("pinnedTime", LocalDateTime.now());
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("questionId").is(questionId)),
                update,
                Question.class
        );
    }

    @Override
    public void unpinQuestion(Long questionId) {
        String loginId = UserContext.getUserId();
        mongoTemplate.updateFirst(
                Query.query(Criteria
                        .where("questionId").is(questionId)
                        .and("userId").is(loginId)
                ),
                new Update().unset("pinnedTime"),
                Question.class
        );
    }

    @Override
    public QuestionPostVO updateQuestion(QuestionUpdateDTO questionUpdateDTO) throws AuthorizationException {
        // 审核帖子内容
        postReviewUtil.review(questionUpdateDTO.getTitle(), questionUpdateDTO.getContent());

        // 创建Update对象，只更新非null字段
        Update update = new Update();

        if (questionUpdateDTO.getTitle() != null) {
            update.set("title", questionUpdateDTO.getTitle());
        }
        if (questionUpdateDTO.getContent() != null) {
            update.set("content", questionUpdateDTO.getContent());
        }
        if (questionUpdateDTO.getMediaFiles() != null) {
            update.set("mediaFiles", questionUpdateDTO.getMediaFiles());
        }
        if (questionUpdateDTO.getTagIds() != null) {
            // 将tagIds去重
            Set<Long> tagIds = Set.copyOf(questionUpdateDTO.getTagIds());
            questionUpdateDTO.setTagIds(tagIds.stream().toList());

            update.set("tagIds", questionUpdateDTO.getTagIds());
        }
        // 设置状态为审核中
        String status = PostStatus.PUBLISHED.getCode();
        update.set("status", status);
        update.set("updateTime", LocalDateTime.now());  // 更新修改时间

        // 使用MongoTemplate执行部分更新
        mongoTemplate.updateFirst(
                Query.query(Criteria
                        .where("questionId").is(questionUpdateDTO.getQuestionId())
                        .and("userId").is(UserContext.getUserId())
                ),
                update,
                Question.class
        );

        return QuestionPostVO
                .builder()
                .questionId(questionUpdateDTO.getQuestionId())
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
