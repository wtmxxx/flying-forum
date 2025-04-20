package com.atcumt.ai.listener;

import com.atcumt.ai.utils.SseEmitterRegistry;
import com.atcumt.model.ai.dto.StopConversationMsgDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Service;

@Service
@RocketMQMessageListener(
        topic = "ai",
        selectorExpression = "conversationStop",
        consumerGroup = "conversation-stop-consumer",
        maxReconsumeTimes = 0,
        messageModel = MessageModel.BROADCASTING
)
@RequiredArgsConstructor
@Slf4j
public class StopConversationConsumer implements RocketMQListener<StopConversationMsgDTO> {
    private final SseEmitterRegistry sseEmitterRegistry;

    @Override
    public void onMessage(StopConversationMsgDTO stopConversationMsgDTO) {
        long now = System.currentTimeMillis();
        long delta = now - stopConversationMsgDTO.getTimestamp();

        if (delta > 5000) {
            log.warn("忽略过期的中止指令，userId={}, conversationId={}, 延迟={}ms",
                    stopConversationMsgDTO.getUserId(), stopConversationMsgDTO.getConversationId(), delta);
            return;
        }

        sseEmitterRegistry.remove(stopConversationMsgDTO.getUserId(), stopConversationMsgDTO.getConversationId());
    }
}
