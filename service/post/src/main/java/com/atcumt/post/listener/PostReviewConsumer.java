package com.atcumt.post.listener;

import com.atcumt.model.post.dto.PostReviewDTO;
import com.atcumt.model.post.entity.Discussion;
import com.atcumt.post.repository.DiscussionRepository;
import com.github.houbb.sensitive.word.core.SensitiveWordHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

//@Service
//@RocketMQMessageListener(
//        topic = "post",
//        selectorExpression = "postReview",
//        consumerGroup = "post-review-consumer",
//        maxReconsumeTimes = 8
//)
@RequiredArgsConstructor
@Slf4j
public class PostReviewConsumer implements RocketMQListener<PostReviewDTO> {
    private final MongoTemplate mongoTemplate;
    private final DiscussionRepository discussionRepository;

    @Override
    public void onMessage(PostReviewDTO postReviewDTO) {
        log.info("收到帖子审核任务: {}", postReviewDTO);
        if (postReviewDTO.getPostType().equals("discussion")) {
            reviewDiscussion(postReviewDTO.getPostId());
        }
    }

    public void reviewDiscussion(Long discussionId) {
        Discussion discussion = discussionRepository.findById(discussionId).orElse(null);

        if (discussion == null) {
            log.info("未找到对应的帖子，消息内容有误");
            throw new RuntimeException("未找到对应的帖子，消息内容有误");
        }

        Update update = new Update();

        boolean titleApproved = true;
        boolean contentApproved = true;

        // 这里可以实现帖子的审核逻辑，根据消息内容进行不同的处理
        if (SensitiveWordHelper.contains(discussion.getTitle())) {
            log.info("标题含有敏感词，审核不通过");
            titleApproved = false;
        }
        if (SensitiveWordHelper.contains(discussion.getContent())) {
            log.info("内容含有敏感词，审核不通过");
            contentApproved = false;
        }

        if (titleApproved && contentApproved) {
            log.info("帖子审核通过");
            update.set("status", 2);
        } else if (!titleApproved && !contentApproved) {
            update.set("status", 3);
            update.set("rejectReason", "标题和内容均含有敏感词，审核不通过");
        } else if (titleApproved) {
            update.set("status", 3);
            update.set("rejectReason", "标题含有敏感词，审核不通过");
        } else {
            update.set("status", 3);
            update.set("rejectReason", "内容含有敏感词，审核不通过");
        }

        // 使用MongoTemplate执行部分更新
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("discussionId").is(discussionId)),
                update,
                Discussion.class
        );
    }
}
