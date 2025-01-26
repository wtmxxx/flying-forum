package com.atcumt.post.service.impl;

import com.atcumt.model.post.dto.PostViewCountDTO;
import com.atcumt.post.service.CountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CountServiceImpl implements CountService {
    private final RocketMQTemplate rocketMQTemplate;

    @Override
    public void incrPostViewCount(PostViewCountDTO postViewCountDTO) {
        rocketMQTemplate.asyncSend("post:postViewCount", postViewCountDTO, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
            }

            @Override
            public void onException(Throwable e) {
                log.error("帖子浏览量计数消息发送失败e: {}", e.getMessage());
            }
        });
    }
}
