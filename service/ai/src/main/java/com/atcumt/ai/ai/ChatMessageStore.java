package com.atcumt.ai.ai;

import com.atcumt.model.ai.entity.Conversation;
import com.atcumt.model.ai.entity.Message;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
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
public class ChatMessageStore implements ChatMemoryStore {
    private final MongoTemplate mongoTemplate;

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        List<ChatMessage> chatMessages = new ArrayList<>();
        chatMessages.add(SystemMessage.from("你是中国矿业大学的AI助手，你的名字叫圈圈，现在是北京时间：" + LocalDateTime.now()));
        return chatMessages;
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> chatMessages) {
    }

    @Override
    public void deleteMessages(Object memoryId) {
    }

    public List<ChatMessage> toChatMessages(int currentMessageId , List<Message> messages) {
        Map<Integer, Message> messageMap = messages.parallelStream()
                .collect(Collectors.toMap(
                        Message::getMessageId,
                        Function.identity(),
                        (existing, replacement) -> replacement
                ));

        List<ChatMessage> chatMessages = new ArrayList<>();

        while (currentMessageId > 0) {
            Message message = messageMap.get(currentMessageId);
            if (message == null) currentMessageId = 0;
            else {
                chatMessages.add(toChatMessage(message));
                currentMessageId = message.getParentId();
            }
        }

        return chatMessages.reversed();
    }

    public ChatMessage toChatMessage(Message message) {
        if (message == null || message.getRole() == null || message.getContent() == null) {
            return null;
        }
        String role = message.getRole();
        String content = message.getContent();

        if ("assistant".equalsIgnoreCase(role)) {
            if (message.getReasoningEnabled()) {
                content = content.replaceFirst("(?s)^<think>.*?</think>", "").stripLeading();
            }
            return AiMessage.from(content);
        } else if ("user".equalsIgnoreCase(role)) {
            return UserMessage.from(content);
        }
        return null;
    }

    public Conversation getConversation(String conversationId, String userId) {
        return mongoTemplate.findOne(new Query(Criteria
                .where("conversationId").is(conversationId)
                .and("userId").is(userId)
        ), Conversation.class);
    }

    public void updateMessage(String conversationId, Message message) {
        Query query = new Query(Criteria.where("_id").is(conversationId));
        Update update = new Update()
                .push("messages", message)
                .set("currentMessageId", message.getMessageId())
                .set("updateTime", LocalDateTime.now());
        mongoTemplate.updateFirst(query, update, Conversation.class);
    }

    public void createEmptyConversation(String conversationId, String userId) {
        Conversation conversation = Conversation
                .builder()
                .conversationId(conversationId)
                .userId(userId)
                .title("新对话")
                .currentMessageId(0)
                .messages(Collections.emptyList())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        mongoTemplate.save(conversation);
    }
}
