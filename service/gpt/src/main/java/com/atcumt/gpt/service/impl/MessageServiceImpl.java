package com.atcumt.gpt.service.impl;

import com.atcumt.gpt.mapper.MessageMapper;
import com.atcumt.gpt.service.MessageService;
import com.atcumt.model.gpt.entity.Message;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {
    // 消息相关的数据库操作Mapper
    private final MessageMapper messageMapper;

    @Override
    public void deleteMessages(String messageId) {
        Message message = messageMapper.getConversationIdAndUpdateTime(messageId);

        LambdaUpdateWrapper<Message> messageUpdateWrapper = new LambdaUpdateWrapper<>();
        messageUpdateWrapper
                .eq(Message::getConversationId, message.getConversationId())
                .ge(Message::getUpdateTime, message.getUpdateTime());
        messageMapper.delete(messageUpdateWrapper);
    }
}
