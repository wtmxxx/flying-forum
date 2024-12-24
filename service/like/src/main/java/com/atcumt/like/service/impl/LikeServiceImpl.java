package com.atcumt.like.service.impl;

import com.atcumt.common.utils.UserContext;
import com.atcumt.like.service.LikeService;
import com.atcumt.model.common.enums.PostType;
import com.atcumt.model.forum.dto.PostLikeDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class LikeServiceImpl implements LikeService {
    private static final int MAX_RECORDS = 16;
    private final RocketMQTemplate rocketMQTemplate;
    private final RedisTemplate<Object, Object> redisTemplate;

    @Override
    public void likeDiscussion(Long discussionId, Integer action) {
        String userId = UserContext.getUserId();

        PostLikeDTO postLikeDTO = PostLikeDTO
                .builder()
                .postId(discussionId)
                .userId(userId)
                .postType(PostType.DISCUSSION.getValue())
                .action(action)
                .build();

        // 异步点赞
        asyncLikeDiscussion(postLikeDTO);

        // 更新redis点赞数据
        redisTemplate.opsForValue().increment("Like:Discussion:Count:" + postLikeDTO.getPostId(), (action == 1 ? 1 : -1));
        redisTemplate.expire("Like:Discussion:Count:" + postLikeDTO.getPostId(), 10, TimeUnit.MINUTES);

        if (action == 1) {
            // 使用时间戳作为分数
            long score = Instant.now().toEpochMilli();
            String userLikeKey = "Like:Discussion:User:" + userId;
            // 储存用户最近点赞（防止点赞数据展示不同步）
            redisTemplate.opsForZSet().add(userLikeKey, discussionId, score);
            // 控制队列大小，删除超出部分
            Long size = redisTemplate.opsForZSet().size(userLikeKey);
            if (size != null && size > MAX_RECORDS) {
                // 删除排名靠前（最早）的记录
                redisTemplate.opsForZSet().removeRange(userLikeKey, 0, size - MAX_RECORDS - 1);
            }
            redisTemplate.expire(userLikeKey, 20, TimeUnit.MINUTES);
        } else {
            String userLikeKey = "Like:Discussion:User:" + userId;
            redisTemplate.opsForZSet().remove(userLikeKey, discussionId);
        }
    }

    public void asyncLikeDiscussion(PostLikeDTO postLikeDTO) {
        rocketMQTemplate.asyncSendOrderly("forum:postLike", postLikeDTO, postLikeDTO.getPostType(), new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {

            }

            @Override
            public void onException(Throwable throwable) {
                log.info("点赞任务下发失败: {}, 原因: {}", postLikeDTO, throwable.getMessage());
            }
        });
    }
}