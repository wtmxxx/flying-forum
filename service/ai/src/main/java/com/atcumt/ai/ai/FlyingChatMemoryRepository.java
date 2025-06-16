package com.atcumt.ai.ai;

import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FlyingChatMemoryRepository implements ChatMemoryRepository {
    Map<String, List<Message>> chatMemoryStore = new ConcurrentHashMap<>();

    @Override
    public List<String> findConversationIds() {
        return new ArrayList<>(this.chatMemoryStore.keySet());
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        List<Message> messages = this.chatMemoryStore.get(conversationId);
        return messages != null ? new ArrayList<>(messages) : List.of();
    }

    public void add(String conversationId, List<Message> messages) {
        this.chatMemoryStore.put(conversationId, messages);
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        this.chatMemoryStore.put(conversationId, messages);
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        this.chatMemoryStore.remove(conversationId);
    }
}
