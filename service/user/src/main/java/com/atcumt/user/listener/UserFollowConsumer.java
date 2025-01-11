package com.atcumt.user.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
//@RocketMQMessageListener(
//        topic = "user",
//        selectorExpression = "userFollow",
//        consumerGroup = "user-follow-consumer",
//        maxReconsumeTimes = 3
//)
@RequiredArgsConstructor
@Slf4j
public class UserFollowConsumer {
}
