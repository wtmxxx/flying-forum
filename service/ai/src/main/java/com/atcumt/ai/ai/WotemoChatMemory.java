package com.atcumt.ai.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.*;

import java.util.LinkedList;
import java.util.List;

@Slf4j
public class WotemoChatMemory implements ChatMemory {
    private final Integer maxTokens;
    private final WotemoTokenizer tokenizer;
    private final List<Message> messages = new LinkedList<>();

    public WotemoChatMemory() {
        this.maxTokens = 20000;
        this.tokenizer = new WotemoTokenizer();
    }

    public WotemoChatMemory(int maxTokens) {
        this.maxTokens = maxTokens;
        this.tokenizer = new WotemoTokenizer(maxTokens);
    }

    public void add(Message message) {
        this.messages.add(message);
        ensureCapacity(this.messages, this.maxTokens, this.tokenizer);
    }

    public void add(List<Message> messages) {
        this.messages.addAll(messages);
        ensureCapacity(this.messages, this.maxTokens, this.tokenizer);
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        return messages.subList(0, Math.min(lastN, messages.size()));
    }

    @Override
    public void clear(String conversationId) {
        messages.clear();
    }

    public void clear() {
        messages.clear();
    }

    private static void ensureCapacity(List<Message> messages, int maxTokens, WotemoTokenizer tokenizer) {
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

                Message evictedMessage = messages.remove(messageToEvictIndex);
                int tokenCountOfEvictedMessage = tokenizer.estimateTokenCountInMessage(evictedMessage);
                log.trace("Evicting the following message ({} tokens) to comply with the capacity requirement: {}", tokenCountOfEvictedMessage, evictedMessage);
                currentTokenCount -= tokenCountOfEvictedMessage;
                if (evictedMessage instanceof AssistantMessage && ((AssistantMessage)evictedMessage).hasToolCalls()) {
                    while(messages.size() > messageToEvictIndex && messages.get(messageToEvictIndex) instanceof ToolResponseMessage) {
                        Message orphanToolExecutionResultMessage = messages.remove(messageToEvictIndex);
                        log.trace("Evicting orphan {}", orphanToolExecutionResultMessage);
                        currentTokenCount -= tokenizer.estimateTokenCountInMessage(orphanToolExecutionResultMessage);
                    }
                }
            }

        }
    }
}
