package com.atcumt.ai.ai;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.internal.ValidationUtils;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.Tokenizer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class WotemoTokenWindowChatMemory implements ChatMemory {
    private final Object id;
    private final Integer maxTokens;
    private final Tokenizer tokenizer;
    private final ChatMemoryStore store;
    private final List<ChatMessage> messages = new LinkedList<>();

    private WotemoTokenWindowChatMemory(WotemoTokenWindowChatMemory.Builder builder) {
        this.id = ValidationUtils.ensureNotNull(builder.id, "id");
        this.maxTokens = ValidationUtils.ensureGreaterThanZero(builder.maxTokens, "maxTokens");
        this.tokenizer = ValidationUtils.ensureNotNull(builder.tokenizer, "tokenizer");
        this.store = ValidationUtils.ensureNotNull(builder.store(), "store");
        this.messages.addAll(this.store.getMessages(id));
    }

    public Object id() {
        return this.id;
    }

    public void add(ChatMessage message) {
        if (message instanceof SystemMessage) {
            Optional<SystemMessage> maybeSystemMessage = findSystemMessage(this.messages);
            if (maybeSystemMessage.isPresent()) {
                if (maybeSystemMessage.get().equals(message)) {
                    return;
                }

                this.messages.remove(maybeSystemMessage.get());
            }
        }

        this.messages.add(message);
        ensureCapacity(this.messages, this.maxTokens, this.tokenizer);
    }

    private static Optional<SystemMessage> findSystemMessage(List<ChatMessage> messages) {
        return messages.stream().filter((message) -> message instanceof SystemMessage).map((message) -> (SystemMessage)message).findAny();
    }

    public List<ChatMessage> messages() {
        List<ChatMessage> messages = new LinkedList<>(this.messages);
        ensureCapacity(messages, this.maxTokens, this.tokenizer);
        return messages;
    }

    private static void ensureCapacity(List<ChatMessage> messages, int maxTokens, Tokenizer tokenizer) {
        if (!messages.isEmpty()) {
            int currentTokenCount = tokenizer.estimateTokenCountInMessages(messages);

            while(currentTokenCount > maxTokens && !messages.isEmpty()) {
                int messageToEvictIndex = 0;
                if (messages.getFirst() instanceof SystemMessage) {
                    if (messages.size() == 1) {
                        return;
                    }

                    messageToEvictIndex = 1;
                }

                ChatMessage evictedMessage = (ChatMessage)messages.remove(messageToEvictIndex);
                int tokenCountOfEvictedMessage = tokenizer.estimateTokenCountInMessage(evictedMessage);
                log.trace("Evicting the following message ({} tokens) to comply with the capacity requirement: {}", tokenCountOfEvictedMessage, evictedMessage);
                currentTokenCount -= tokenCountOfEvictedMessage;
                if (evictedMessage instanceof AiMessage && ((AiMessage)evictedMessage).hasToolExecutionRequests()) {
                    while(messages.size() > messageToEvictIndex && messages.get(messageToEvictIndex) instanceof ToolExecutionResultMessage) {
                        ChatMessage orphanToolExecutionResultMessage = (ChatMessage)messages.remove(messageToEvictIndex);
                        log.trace("Evicting orphan {}", orphanToolExecutionResultMessage);
                        currentTokenCount -= tokenizer.estimateTokenCountInMessage(orphanToolExecutionResultMessage);
                    }
                }
            }

        }
    }

    public void clear() {
        messages.clear();
    }

    public static WotemoTokenWindowChatMemory.Builder builder() {
        return new WotemoTokenWindowChatMemory.Builder();
    }

    public static WotemoTokenWindowChatMemory withMaxTokens(int maxTokens, Tokenizer tokenizer) {
        return builder().maxTokens(maxTokens, tokenizer).build();
    }

    public static class Builder {
        private Object id = "default";
        private Integer maxTokens;
        private Tokenizer tokenizer;
        private ChatMemoryStore store;

        public Builder() {
        }

        public WotemoTokenWindowChatMemory.Builder id(Object id) {
            this.id = id;
            return this;
        }

        public WotemoTokenWindowChatMemory.Builder maxTokens(Integer maxTokens, Tokenizer tokenizer) {
            this.maxTokens = maxTokens;
            this.tokenizer = tokenizer;
            return this;
        }

        public WotemoTokenWindowChatMemory.Builder chatMemoryStore(ChatMemoryStore store) {
            this.store = store;
            return this;
        }

        public ChatMemoryStore store() {
            return this.store;
        }

        public WotemoTokenWindowChatMemory build() {
            return new WotemoTokenWindowChatMemory(this);
        }
    }
}
