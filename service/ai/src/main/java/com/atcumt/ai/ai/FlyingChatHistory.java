package com.atcumt.ai.ai;

import com.atcumt.model.ai.entity.Conversation;
import com.atcumt.model.ai.entity.StoreMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FlyingChatHistory {
    private final MongoTemplate mongoTemplate;

    public List<Message> toChatMessages(int currentMessageId , List<StoreMessage> storeMessages) {
        Map<Integer, StoreMessage> messageMap = storeMessages.parallelStream()
                .collect(Collectors.toMap(
                        StoreMessage::getMessageId,
                        Function.identity(),
                        (existing, replacement) -> replacement
                ));

        List<Message> messages = new ArrayList<>();

        while (currentMessageId > 0) {
            StoreMessage storeMessage = messageMap.get(currentMessageId);
            if (storeMessage == null) currentMessageId = 0;
            else {
                messages.add(toChatMessage(storeMessage));
                currentMessageId = storeMessage.getParentId();
            }
        }

        return messages.reversed();
    }

    public Message toChatMessage(StoreMessage storeMessage) {
        if (storeMessage == null || storeMessage.getRole() == null || storeMessage.getTextContent() == null) {
            return new UserMessage("");
        }
        String role = storeMessage.getRole();
        String textContent = storeMessage.getTextContent();

        if (MessageType.ASSISTANT.getValue().equalsIgnoreCase(role)) {
            return new AssistantMessage(textContent);
        } else if (MessageType.USER.getValue().equalsIgnoreCase(role)) {
            return new UserMessage(textContent);
        }
        return new UserMessage("");
    }

    public Conversation getConversation(String conversationId, String userId) {
        return mongoTemplate.findOne(new Query(Criteria
                .where("conversationId").is(conversationId)
                .and("userId").is(userId)
        ), Conversation.class);
    }

    public void updateMessage(String conversationId, StoreMessage storeMessage) {
        Query query = new Query(Criteria.where("_id").is(conversationId));
        Update update = new Update()
                .push("messages", storeMessage)
                .set("currentMessageId", storeMessage.getMessageId())
                .set("updateTime", LocalDateTime.now());
        mongoTemplate.updateFirst(query, update, Conversation.class);
    }

    public void createEmptyConversation(String conversationId, String userId) {
        LocalDateTime now = LocalDateTime.now();
        Conversation conversation = Conversation
                .builder()
                .conversationId(conversationId)
                .userId(userId)
                .title("新对话")
                .currentMessageId(0)
                .messages(Collections.emptyList())
                .createTime(now)
                .updateTime(now)
                .build();
        mongoTemplate.save(conversation);
    }
}
