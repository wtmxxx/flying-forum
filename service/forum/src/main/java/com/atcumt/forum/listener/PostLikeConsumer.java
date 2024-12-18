package com.atcumt.forum.listener;

import cn.hutool.core.util.IdUtil;
import com.atcumt.model.forum.dto.PostLikeDTO;
import com.atcumt.model.forum.entity.PostLike;
import com.atcumt.model.forum.enums.PostType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.LocalDateTime;
import java.util.Objects;

//@Service
//@RocketMQMessageListener(
//        topic = "forum",
//        selectorExpression = "postLike",
//        consumerGroup = "forum-consumer",
//        consumeMode = ConsumeMode.ORDERLY,
//        maxReconsumeTimes = 16
//)
@RequiredArgsConstructor
@Slf4j
public class PostLikeConsumer implements RocketMQListener<PostLikeDTO> {
    private final MongoTemplate mongoTemplate;

    @Override
    public void onMessage(PostLikeDTO postLikeDTO) {
        try {
            // 构建查询条件
            Query query = new Query(Criteria
                    .where("postId").is(postLikeDTO.getPostId())
                    .and("userId").is(postLikeDTO.getUserId())
                    .and("postType").is(postLikeDTO.getPostType())
            );

            // 查找记录是否存在
            boolean exists = mongoTemplate.exists(query, PostLike.class);

            String collectionName = "discussion";
            String idField = "_id";
            if (Objects.equals(postLikeDTO.getPostType(), PostType.QA.getValue())) {
                collectionName = "qa";
            }

            if (exists) {
                if (postLikeDTO.getAction() == 1) return;
                // 如果存在，删除记录
                mongoTemplate.remove(query, PostLike.class);
                Update update = new Update().inc("likeCount", -1);

                mongoTemplate.updateFirst(Query.query(Criteria.where(idField).is(postLikeDTO.getPostId())), update, collectionName);
                // TODO 删除用户获赞
            } else {
                if (postLikeDTO.getAction() == -1) return;
                // 如果不存在，插入记录
                PostLike newLike = PostLike.builder()
                        .likeId(IdUtil.getSnowflakeNextId())
                        .postId(postLikeDTO.getPostId())
                        .userId(postLikeDTO.getUserId())
                        .postType(postLikeDTO.getPostType())
                        .createTime(LocalDateTime.now())
                        .build();
                mongoTemplate.insert(newLike);
                Update update = new Update().inc("likeCount", 1);
                mongoTemplate.updateFirst(Query.query(Criteria.where(idField).is(postLikeDTO.getPostId())), update, collectionName);
                // TODO 增加用户获赞
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
